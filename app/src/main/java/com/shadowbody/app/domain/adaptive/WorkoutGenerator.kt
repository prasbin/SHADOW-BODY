package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.Goal
import com.shadowbody.app.domain.model.MuscleGroup
import com.shadowbody.app.domain.model.ProgressionState
import kotlin.math.ceil

/**
 * Phase 4: builds the next recommended workout on top of the Phase 3 plan and
 * exercise data. Deterministic — identical [GenerationContext] always yields
 * an identical [GeneratedWorkout], with no randomness and no network.
 *
 * Selection order (fully specified, no hidden behaviour):
 * 1. Keep only exercises the user actually has equipment for.
 * 2. Pick a plan: the least recently completed one, ties broken by fewer
 *    completions then lowest id. Falls back to the exercise library when no
 *    plan has a usable exercise.
 * 3. Ask [WorkoutAdaptationEngine] for each exercise's next target, passing
 *    readiness and the weekly-schedule state.
 * 4. Trim from the end until the estimate fits the planned session length.
 *
 * Nothing here claims a session is safe or appropriate medically; it only
 * respects the user's own recorded equipment, schedule and session length.
 */
object WorkoutGenerator {

    fun generate(context: GenerationContext): GeneratedWorkout? {
        val available = context.equipment
        val weeklyTarget = context.trainingDays.size
        val weeklyMet = weeklyTarget > 0 && context.completedThisWeek >= weeklyTarget
        val adaptationContext = AdaptationContext(
            missedSessions = context.missedSessions,
            weeklyTargetMet = weeklyMet,
        )

        val plan = selectPlan(context, available)
        val baseSlots = plan?.slots?.filter { isAvailable(it, available) }
            ?: context.library.filter { isAvailable(it, available) }

        if (baseSlots.isEmpty()) return null

        val decisions = baseSlots.map { slot ->
            val progress = context.adaptations[slot.exerciseId]
            val decision = if (progress == null) {
                // Untracked exercise: start from the slot target, but still
                // honour readiness, the weekly schedule and recorded misses.
                // With no lastResult the engine can never claim progress.
                WorkoutAdaptationEngine.nextTarget(
                    progress = ExerciseProgress(
                        exerciseId = slot.exerciseId,
                        currentSets = slot.sets,
                        currentReps = slot.reps,
                        currentDurationSec = slot.durationSec,
                        restSec = slot.restSec,
                        sessionsAtTarget = 0,
                        lastResult = null,
                        state = ProgressionState.UNTRACKED,
                    ),
                    readiness = context.readiness,
                    context = adaptationContext,
                )
            } else {
                WorkoutAdaptationEngine.nextTarget(
                    progress = progress,
                    readiness = context.readiness,
                    context = adaptationContext,
                )
            }
            val previous = progress?.let { describeTarget(it.currentSets, it.currentReps, it.currentDurationSec) }
                ?: describeTarget(slot.sets, slot.reps, slot.durationSec)
            slot to (decision to previous)
        }

        val aggressionMultiplier = when (context.aggressionLevel) {
            1 -> 0.7
            2 -> 0.85
            3 -> 1.0
            4 -> 1.15
            5 -> 1.3
            else -> 1.0
        }

        val fitnessMultiplier = when (context.fitnessLevel) {
            com.shadowbody.app.domain.model.FitnessLevel.BEGINNER -> 0.8
            com.shadowbody.app.domain.model.FitnessLevel.INTERMEDIATE -> 1.0
            com.shadowbody.app.domain.model.FitnessLevel.ADVANCED -> 1.2
            else -> 1.0
        }

        val bmi = context.heightCm?.let { h -> context.weightKg?.let { w -> w / ((h / 100) * (h / 100)) } }
        val bmiAdjustment = when {
            bmi == null -> 1.0
            bmi < 18.5 -> 0.9
            bmi > 30.0 -> 0.85
            else -> 1.0
        }

        val ageAdjustment = context.age?.let { a ->
            when {
                a < 18 -> 0.85
                a > 55 -> 0.9
                else -> 1.0
            }
        } ?: 1.0

        val intensityMultiplier = aggressionMultiplier * fitnessMultiplier * bmiAdjustment * ageAdjustment

        val kept = enforceDuration(decisions, context.sessionMinutes)
        if (kept.isEmpty()) return null

        val exercises = kept.mapIndexed { index, entry ->
            val (slot, pair) = entry
            val (decision, previous) = pair
            val adjustedSets = (decision.sets * intensityMultiplier).toInt().coerceIn(1, 10)
            GeneratedExercise(
                exerciseId = slot.exerciseId,
                exerciseName = slot.exerciseName,
                position = index,
                sets = adjustedSets,
                reps = decision.reps,
                durationSec = decision.durationSec,
                restSec = decision.restSec,
                reason = decision.reason,
                previousTargetText = previous,
            )
        }

        val progressed = exercises.count { it.reason.state == ProgressionState.PROGRESSING }
        val reduced = exercises.count { it.reason.state == ProgressionState.REGRESSING }
        val maintained = exercises.size - progressed - reduced
        val minutes = estimateMinutes(exercises)

        val intensityLabel = when {
            intensityMultiplier >= 1.2 -> "HIGH"
            intensityMultiplier >= 0.9 -> "MODERATE"
            else -> "REDUCED"
        }

        val intensityReason = buildString {
            append("Intensity: $intensityLabel. ")
            append("Aggression: ${context.aggressionLevel}/5. ")
            context.fitnessLevel?.let { append("Fitness: ${it.name}. ") }
            bmi?.let { append("BMI: ${"%.1f".format(it)}. ") }
            context.age?.let { append("Age: $it. ") }
            if (context.readiness != null) {
                if (context.readiness.fatigue >= 4) append("High fatigue reduces intensity. ")
                if (context.readiness.soreness >= 4) append("High soreness reduces intensity. ")
            }
            if (context.missedSessions >= 2) append("Missed sessions reduce volume. ")
        }

        return GeneratedWorkout(
            name = plan?.name?.let { "ADAPTIVE · ${it.uppercase()}" } ?: "ADAPTIVE SESSION",
            sourcePlanId = plan?.id,
            estimatedMinutes = minutes,
            summary = buildSummary(plan, progressed, maintained, reduced, weeklyMet, context) + " " + intensityReason,
            exercises = exercises,
            progressed = progressed,
            maintained = maintained,
            reduced = reduced,
        )
    }

    /** Total estimated minutes for a set of exercises (deterministic). */
    fun estimateMinutes(exercises: List<GeneratedExercise>): Int {
        val seconds = exercises.sumOf { exerciseSeconds(it.sets, it.reps, it.durationSec, it.restSec) }
        return ceil(seconds.toDouble() / AdaptationLimits.SECONDS_PER_MINUTE).toInt().coerceAtLeast(1)
    }

    // --- Selection ----------------------------------------------------------

    /**
     * Least recently trained plan wins; ties go to the least completed, then
     * the lowest id. Plans with no usable exercise are skipped.
     */
    private fun selectPlan(
        context: GenerationContext,
        available: Set<Equipment>,
    ): PlanCandidate? = context.plans
        .filter { plan -> plan.slots.any { isAvailable(it, available) } }
        .sortedWith(
            compareBy<PlanCandidate> { it.lastCompletedAt }
                .thenBy { it.completedCount }
                .thenBy { it.id },
        )
        .firstOrNull()

    /** An exercise needs the matching equipment; BODYWEIGHT always qualifies. */
    private fun isAvailable(slot: SlotCandidate, available: Set<Equipment>): Boolean =
        when (slot.equipment) {
            Equipment.BODYWEIGHT -> available.contains(Equipment.BODYWEIGHT) ||
                available.contains(Equipment.NONE)
            Equipment.NONE -> true
            else -> available.contains(slot.equipment)
        }

    /**
     * Caps the workout at [AdaptationLimits.MAX_GENERATED_EXERCISES], then
     * drops trailing exercises until the estimate fits [sessionMinutes]. At
     * least one exercise is always kept: an honest "this is longer than your
     * session length" is better than an empty recommendation.
     */
    private fun enforceDuration(
        decisions: List<Pair<SlotCandidate, Pair<AdaptationDecision, String>>>,
        sessionMinutes: Int?,
    ): List<Pair<SlotCandidate, Pair<AdaptationDecision, String>>> {
        // The size cap always applies, even without a session length.
        var kept = decisions.take(AdaptationLimits.MAX_GENERATED_EXERCISES)
        val limit = sessionMinutes?.times(AdaptationLimits.SECONDS_PER_MINUTE) ?: return kept
        while (kept.size > AdaptationLimits.MIN_GENERATED_EXERCISES &&
            estimateSeconds(kept) > limit
        ) {
            kept = kept.dropLast(1)
        }
        return kept
    }

    private fun estimateSeconds(
        decisions: List<Pair<SlotCandidate, Pair<AdaptationDecision, String>>>,
    ): Int = decisions.sumOf { (_, pair) ->
        val decision = pair.first
        exerciseSeconds(decision.sets, decision.reps, decision.durationSec, decision.restSec)
    }

    private fun exerciseSeconds(sets: Int, reps: Int?, durationSec: Int?, restSec: Int): Int {
        val workPerSet = reps?.times(AdaptationLimits.SECONDS_PER_REP) ?: durationSec ?: 0
        // Rest is not counted after the final set.
        val restTotal = (sets - 1).coerceAtLeast(0) * restSec
        return AdaptationLimits.SECONDS_PER_EXERCISE_OVERHEAD + sets * workPerSet + restTotal
    }

    private fun describeTarget(sets: Int, reps: Int?, durationSec: Int?): String =
        if (reps != null) "$sets x $reps" else "$sets x ${durationSec ?: 0}s"

    private fun buildSummary(
        plan: PlanCandidate?,
        progressed: Int,
        maintained: Int,
        reduced: Int,
        weeklyMet: Boolean,
        context: GenerationContext,
    ): String {
        val source = plan?.name?.let { "from your plan \"${it.uppercase()}\"" } ?: "from your exercise library"
        val parts = mutableListOf<String>()
        if (progressed > 0) parts += "$progressed progressed"
        if (maintained > 0) parts += "$maintained maintained"
        if (reduced > 0) parts += "$reduced reduced"
        val changes = if (parts.isEmpty()) "no changes" else parts.joinToString(", ")
        val schedule = when {
            context.readiness != null &&
                context.readiness.fatigue >= AdaptationLimits.HIGH_FATIGUE_THRESHOLD ->
                " High fatigue reported, so demand was lowered."
            weeklyMet -> " This week's planned sessions are already complete, so targets were held."
            context.missedSessions >= AdaptationLimits.MISSED_SESSIONS_TO_REDUCE ->
                " Missed sessions were counted, so demand was lowered conservatively."
            else -> ""
        }
        return "Generated $source: $changes.$schedule"
    }

    // --- Library ordering ---------------------------------------------------

    /**
     * Deterministic library ordering used when no plan is available: goal
     * priority first, then a stable per-muscle rotation, then exercise id.
     * Exposed for tests and for the planner's fallback query.
     */
    fun orderLibrary(slots: List<SlotCandidate>, goals: Set<Goal>): List<SlotCandidate> {
        val priority = goalMusclePriority(goals)
        return slots.sortedWith(
            compareBy<SlotCandidate> { slot ->
                priority.indexOf(slot.muscleGroup).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
                .thenBy { it.muscleGroup.ordinal }
                .thenBy { it.exerciseId },
        )
    }

    /** Muscle groups most relevant to the user's goals, most relevant first. */
    fun goalMusclePriority(goals: Set<Goal>): List<MuscleGroup> = buildList {
        if (goals.contains(Goal.BUILD_STRENGTH) || goals.contains(Goal.BUILD_MUSCLE)) {
            addAll(
                listOf(
                    MuscleGroup.CHEST,
                    MuscleGroup.BACK,
                    MuscleGroup.LEGS,
                    MuscleGroup.SHOULDERS,
                    MuscleGroup.ARMS,
                    MuscleGroup.GLUTES,
                ),
            )
        }
        if (goals.contains(Goal.IMPROVE_ENDURANCE) || goals.contains(Goal.LOSE_WEIGHT)) {
            addAll(listOf(MuscleGroup.FULL_BODY, MuscleGroup.CORE, MuscleGroup.LEGS))
        }
        if (goals.contains(Goal.HOME_FITNESS) || goals.contains(Goal.GENERAL_FITNESS)) {
            addAll(listOf(MuscleGroup.FULL_BODY, MuscleGroup.CORE))
        }
        addAll(MuscleGroup.entries.toList())
    }.distinct()

    /** Default target for a library-generated slot, inside the limits. */
    fun defaultTarget(category: ExerciseCategory): Pair<Int?, Int?> = when (category) {
        ExerciseCategory.CARDIO, ExerciseCategory.CORE -> null to 45
        else -> 10 to null
    }
}
