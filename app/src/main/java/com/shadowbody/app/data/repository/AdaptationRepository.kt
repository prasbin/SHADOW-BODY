package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.AdaptationDao
import com.shadowbody.app.data.local.ExerciseAdaptation
import com.shadowbody.app.data.local.MissedWorkout
import com.shadowbody.app.data.local.MissedWorkoutDao
import com.shadowbody.app.data.local.ReadinessDao
import com.shadowbody.app.data.local.SessionDao
import com.shadowbody.app.data.local.SessionExerciseDetail
import com.shadowbody.app.domain.adaptive.AdaptationContext
import com.shadowbody.app.domain.adaptive.AdaptationLimits
import com.shadowbody.app.domain.adaptive.ExerciseProgress
import com.shadowbody.app.domain.adaptive.PerformanceRecord
import com.shadowbody.app.domain.adaptive.WorkoutAdaptationEngine
import com.shadowbody.app.domain.model.ProgressionState
import kotlinx.coroutines.flow.Flow

/**
 * Phase 4: the adaptive engine's memory.
 *
 * Folds *completed* sessions into per-exercise targets through the pure
 * [WorkoutAdaptationEngine]. Three properties matter:
 *
 * - **Idempotent.** A checkpoint records the last applied session id, so
 *   replaying history can never double-count progress.
 * - **Non-destructive.** Completed sessions are only ever *read*. Nothing here
 *   rewrites or deletes history.
 * - **Honest.** A session's own snapshot is the only truth about what was
 *   asked, so a manually edited plan always wins over remembered state, and an
 *   upgraded install starts from zero rather than inventing progress.
 */
class AdaptationRepository(
    private val adaptations: AdaptationDao,
    private val sessions: SessionDao,
    private val readinessReports: ReadinessDao,
    private val missed: MissedWorkoutDao,
) {

    // --- Reads ---

    fun observeAll(): Flow<List<ExerciseAdaptation>> = adaptations.observeAll()

    suspend fun getAll(): List<ExerciseAdaptation> = adaptations.getAll()

    suspend fun getByExercise(exerciseId: Long): ExerciseAdaptation? =
        adaptations.getByExercise(exerciseId)

    suspend fun count(): Int = adaptations.count()

    suspend fun lastAppliedSessionId(): Long = adaptations.checkpoint()?.lastAppliedSessionId ?: 0

    /** Engine-shaped view of one exercise; null when it has no state yet. */
    suspend fun progressFor(exerciseId: Long): ExerciseProgress? =
        getByExercise(exerciseId)?.let { toProgress(it) }

    suspend fun allProgress(): Map<Long, ExerciseProgress> =
        getAll().associate { it.exerciseId to toProgress(it) }

    // --- Missed sessions (always user-reported, never inferred) ---

    fun observeMissed(limit: Int = 10): Flow<List<MissedWorkout>> = missed.observeRecent(limit)

    suspend fun latestMissed(): MissedWorkout? = missed.latest()

    suspend fun recordMissed(
        planId: Long? = null,
        reason: String = "",
        now: Long = System.currentTimeMillis(),
    ): Long = missed.insert(
        MissedWorkout(planId = planId, recordedAt = now, reason = reason.trim()),
    )

    /** User-reported misses inside the current 7-day window. */
    suspend fun missedInWindow(now: Long = System.currentTimeMillis()): Int =
        missed.countSince(now - AdaptationLimits.WEEK_WINDOW_MS)

    // --- Applying completed sessions ---

    /**
     * Reads every completed session after the checkpoint, oldest first, and
     * updates the matching adaptation rows. Returns how many sessions were
     * applied; running it again immediately applies nothing.
     */
    suspend fun applyCompletedSessions(now: Long = System.currentTimeMillis()): Int {
        var expected = lastAppliedSessionId()
        var applied = 0
        for (session in sessions.completedSessionsAfter(expected)) {
            applySession(session.id, session.endedAt ?: session.startedAt, now)
            // Compare-and-set: if someone else moved the checkpoint, stop
            // rather than replay the same session twice.
            if (!adaptations.advanceCheckpoint(expected, session.id, now)) return applied
            expected = session.id
            applied++
        }
        return applied
    }

    private suspend fun applySession(sessionId: Long, endedAt: Long, now: Long) {
        val detail = sessions.getDetail(sessionId) ?: return
        // The check-in that existed when the session ended, not today's answer.
        val readiness = ReadinessRepository.toSnapshot(
            readinessReports.latestAtOrBefore(endedAt),
        )
        val context = AdaptationContext(missedSessions = missedInWindow(endedAt))

        for (exercise in detail.exercises) {
            val exerciseId = exercise.sessionExercise.exerciseId
            val record = performanceOf(exercise)
            val existing = adaptations.getByExercise(exerciseId)

            // What this session actually asked for, clamped into the safety
            // bounds. A plan edited since the last session therefore wins.
            val target = WorkoutAdaptationEngine.initialTarget(
                exerciseId = exerciseId,
                sets = exercise.sets.size,
                reps = exercise.sets.firstOrNull { it.targetReps != null }?.targetReps,
                durationSec = exercise.sets.firstOrNull { it.targetDurationSec != null }
                    ?.targetDurationSec,
                restSec = existing?.restSec ?: DEFAULT_REST_SEC,
            )

            val movedTarget = existing != null && (
                target.sets != existing.currentSets ||
                    target.reps != existing.currentReps ||
                    target.durationSec != existing.currentDurationSec
                )
            // A different target starts a fresh streak; the same one keeps it.
            val priorStreak = if (movedTarget) 0 else existing?.sessionsAtTarget ?: 0

            val progress = ExerciseProgress(
                exerciseId = exerciseId,
                currentSets = target.sets,
                currentReps = target.reps,
                currentDurationSec = target.durationSec,
                restSec = target.restSec,
                sessionsAtTarget = priorStreak,
                lastResult = record,
                state = existing?.state ?: ProgressionState.UNTRACKED,
            )
            val decision = WorkoutAdaptationEngine.nextTarget(progress, readiness, context)

            val cleanSession = record.completionRatio >= 1.0
            val sessionsAtTarget = when {
                // A new target must be proven before it can be raised again.
                decision.isProgression || !cleanSession -> 0
                else -> priorStreak + 1
            }
            val adjusted = decision.sets != target.sets ||
                decision.reps != target.reps ||
                decision.durationSec != target.durationSec

            val base = existing ?: ExerciseAdaptation(
                exerciseId = exerciseId,
                currentSets = target.sets,
                currentReps = target.reps,
                currentDurationSec = target.durationSec,
                restSec = target.restSec,
                state = ProgressionState.UNTRACKED,
                lastReasonCode = decision.reason.code,
                lastReasonText = decision.reason.message,
                lastAdjustmentAt = endedAt,
                updatedAt = now,
            )

            adaptations.upsert(
                base.copy(
                    currentSets = decision.sets,
                    currentReps = decision.reps,
                    currentDurationSec = decision.durationSec,
                    restSec = decision.restSec,
                    state = decision.state,
                    sessionsAtTarget = sessionsAtTarget,
                    lastCompletedSets = record.completedSets,
                    lastTargetSets = record.targetSets,
                    lastActualReps = record.actualReps,
                    lastTargetReps = record.targetReps,
                    lastActualDurationSec = record.actualDurationSec,
                    lastTargetDurationSec = record.targetDurationSec,
                    lastReasonCode = decision.reason.code,
                    lastReasonText = decision.reason.message,
                    // Only stamp a real adjustment when the target moved.
                    lastAdjustmentAt = if (adjusted) endedAt else base.lastAdjustmentAt,
                    updatedAt = now,
                ),
            )
        }
    }

    /** Best completed set of the exercise, plus what was asked for. */
    private fun performanceOf(exercise: SessionExerciseDetail): PerformanceRecord {
        val completed = exercise.sets.filter { it.isCompleted }
        return PerformanceRecord(
            completedSets = completed.size,
            targetSets = exercise.sets.size,
            actualReps = completed.maxOfOrNull { it.actualReps ?: 0 } ?: 0,
            targetReps = exercise.sets.firstOrNull { it.targetReps != null }?.targetReps,
            actualDurationSec = completed.maxOfOrNull { it.actualDurationSec ?: 0 } ?: 0,
            targetDurationSec = exercise.sets.firstOrNull { it.targetDurationSec != null }
                ?.targetDurationSec,
        )
    }

    private fun toProgress(row: ExerciseAdaptation): ExerciseProgress = ExerciseProgress(
        exerciseId = row.exerciseId,
        currentSets = row.currentSets,
        currentReps = row.currentReps,
        currentDurationSec = row.currentDurationSec,
        restSec = row.restSec,
        sessionsAtTarget = row.sessionsAtTarget,
        lastResult = row.lastTargetSets?.let { targetSets ->
            PerformanceRecord(
                completedSets = row.lastCompletedSets ?: 0,
                targetSets = targetSets,
                actualReps = row.lastActualReps,
                targetReps = row.lastTargetReps,
                actualDurationSec = row.lastActualDurationSec,
                targetDurationSec = row.lastTargetDurationSec,
            )
        },
        state = row.state,
    )

    companion object {
        /** Rest assumed for a session snapshot that did not record one. */
        const val DEFAULT_REST_SEC = 60
    }
}
