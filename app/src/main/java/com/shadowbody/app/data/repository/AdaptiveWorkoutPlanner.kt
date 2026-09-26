package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.ExerciseDao
import com.shadowbody.app.data.local.PlanDao
import com.shadowbody.app.data.local.PlanExerciseDao
import com.shadowbody.app.data.local.RecommendationDetail
import com.shadowbody.app.data.local.RecommendedExercise
import com.shadowbody.app.data.local.SessionDao
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.local.WorkoutPlanExercise
import com.shadowbody.app.data.local.WorkoutRecommendation
import com.shadowbody.app.domain.adaptive.AdaptationLimits
import com.shadowbody.app.domain.adaptive.GenerationContext
import com.shadowbody.app.domain.adaptive.PlanCandidate
import com.shadowbody.app.domain.adaptive.SlotCandidate
import com.shadowbody.app.domain.adaptive.WorkoutGenerator
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.MuscleGroup
import com.shadowbody.app.domain.model.RecommendationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Phase 4: assembles the generator's inputs from Room, runs the pure
 * [WorkoutGenerator], and stores the result as a snapshot.
 *
 * The Phase 3 plan system is not replaced — a recommendation is turned back
 * into a *real* plan when adopted, so ordinary session logging, the rest
 * timer and history keep working exactly as before.
 */
class AdaptiveWorkoutPlanner(
    private val profiles: ProfileRepository,
    private val plans: PlanRepository,
    private val planDao: PlanDao,
    private val planExercises: PlanExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val sessions: SessionDao,
    private val adaptation: AdaptationRepository,
    private val readiness: ReadinessRepository,
    private val recommendations: RecommendationRepository,
) {

    fun observeLatest(): Flow<RecommendationDetail?> = recommendations.observeLatest()

    suspend fun latest(): RecommendationDetail? = recommendations.latestOnce()

    suspend fun count(): Int = recommendations.count()

    /**
     * Folds newly completed sessions into adaptation state without generating
     * anything. Safe to call on every screen entry: the checkpoint makes it
     * idempotent.
     */
    suspend fun applyLatestEvidence(now: Long = System.currentTimeMillis()): Int =
        adaptation.applyCompletedSessions(now)

    /**
     * Folds in any newly completed sessions, then generates and stores the
     * next recommendation. Returns null when the user has no usable exercise
     * for their recorded equipment.
     */
    suspend fun generate(now: Long = System.currentTimeMillis()): RecommendationDetail? {
        // Fresh evidence first: a session finished since the last generation
        // must be able to change the recommendation.
        adaptation.applyCompletedSessions(now)

        val workout = WorkoutGenerator.generate(buildContext(now)) ?: return null
        val profile = profiles.get()
        val readinessReport = readiness.latestOnce()
        val missed = adaptation.latestMissed()

        val id = recommendations.insert(
            header = WorkoutRecommendation(
                createdAt = now,
                planId = workout.sourcePlanId,
                readinessReportId = readinessReport?.id,
                missedWorkoutId = missed?.id,
                name = workout.name,
                estimatedMinutes = workout.estimatedMinutes,
                summary = workout.summary,
                status = RecommendationStatus.ACTIVE,
            ),
            exercises = workout.exercises.map { generated ->
                RecommendedExercise(
                    recommendationId = 0,
                    exerciseId = generated.exerciseId,
                    position = generated.position,
                    sets = generated.sets,
                    reps = generated.reps,
                    durationSec = generated.durationSec,
                    restSec = generated.restSec,
                    reasonCode = generated.reason.code,
                    reasonText = generated.reason.message,
                )
            },
        )
        return recommendations.byId(id)
    }

    /**
     * Turns a recommendation into a real Phase 3 plan, so the user trains it
     * with the normal session flow. Returns the new plan id.
     */
    suspend fun adopt(recommendationId: Long, now: Long = System.currentTimeMillis()): Long? {
        val detail = recommendations.byId(recommendationId) ?: return null
        if (detail.exercises.isEmpty()) return null
        val header = detail.recommendation
        val planId = plans.savePlan(
            plan = WorkoutPlan(
                name = header.name.removePrefix("ADAPTIVE · ").ifBlank { header.name },
                description = header.summary,
                targetDurationMin = header.estimatedMinutes,
                createdAt = now,
                updatedAt = now,
            ),
            entries = detail.exercises.sortedBy { it.position }.map { row ->
                WorkoutPlanExercise(
                    planId = 0,
                    exerciseId = row.exerciseId,
                    position = row.position,
                    targetSets = row.sets,
                    targetReps = row.reps,
                    targetDurationSec = row.durationSec,
                    restSec = row.restSec,
                    notes = row.reasonText,
                )
            },
        )
        recommendations.markAdopted(recommendationId, planId)
        return planId
    }

    /**
     * Records an explicit skip. The miss is stored separately from session
     * history, and completed sessions are never touched.
     */
    suspend fun markMissed(
        recommendationId: Long,
        now: Long = System.currentTimeMillis(),
    ) {
        val detail = recommendations.byId(recommendationId) ?: return
        adaptation.recordMissed(planId = detail.recommendation.planId, now = now)
        recommendations.updateStatus(recommendationId, RecommendationStatus.MISSED)
    }

    suspend fun dismiss(recommendationId: Long) {
        recommendations.updateStatus(recommendationId, RecommendationStatus.DISMISSED)
    }

    // --- Context assembly ---------------------------------------------------

    internal suspend fun buildContext(now: Long): GenerationContext {
        val profile = profiles.get()
        val readinessReport = readiness.latestOnce()
        val planRows = planDao.getActivePlans()
        val planCandidates = planRows.map { plan ->
            PlanCandidate(
                id = plan.id,
                name = plan.name,
                targetDurationMin = plan.targetDurationMin,
                completedCount = sessions.completedCountForPlan(plan.id),
                lastCompletedAt = sessions.lastCompletedAtForPlan(plan.id) ?: 0L,
                slots = planExercises.getByPlan(plan.id).map { slot ->
                    // A missing exercise fails loudly — RESTRICT makes this
                    // unreachable, never silent.
                    val exercise = requireNotNull(exerciseDao.getById(slot.exerciseId)) {
                        "Plan ${plan.id} references missing exercise ${slot.exerciseId}"
                    }
                    SlotCandidate(
                        exerciseId = slot.exerciseId,
                        exerciseName = exercise.name,
                        muscleGroup = exercise.muscleGroup,
                        equipment = exercise.equipment,
                        category = exercise.category,
                        sets = slot.targetSets,
                        reps = slot.targetReps,
                        durationSec = slot.targetDurationSec,
                        restSec = slot.restSec,
                    )
                },
            )
        }

        val library = WorkoutGenerator.orderLibrary(
            exerciseDao.getActive().map { exercise ->
                val (reps, duration) = WorkoutGenerator.defaultTarget(exercise.category)
                SlotCandidate(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    muscleGroup = exercise.muscleGroup,
                    equipment = exercise.equipment,
                    category = exercise.category,
                    sets = DEFAULT_LIBRARY_SETS,
                    reps = reps,
                    durationSec = duration,
                    restSec = DEFAULT_LIBRARY_REST_SEC,
                )
            },
            goals = profile?.goals ?: emptySet(),
        )

        return GenerationContext(
            now = now,
            equipment = profile?.equipment ?: setOf(Equipment.BODYWEIGHT),
            goals = profile?.goals ?: emptySet(),
            fitnessLevel = profile?.fitnessLevel,
            sessionMinutes = profile?.sessionMinutes,
            trainingDays = profile?.trainingDays ?: emptySet(),
            plans = planCandidates,
            library = library,
            adaptations = adaptation.allProgress(),
            readiness = ReadinessRepository.toSnapshot(readinessReport),
            completedThisWeek = sessions.completedCountSince(now - AdaptationLimits.WEEK_WINDOW_MS),
            missedSessions = adaptation.missedInWindow(now),
        )
    }

    companion object {
        const val DEFAULT_LIBRARY_SETS = 3
        const val DEFAULT_LIBRARY_REST_SEC = 60
    }
}
