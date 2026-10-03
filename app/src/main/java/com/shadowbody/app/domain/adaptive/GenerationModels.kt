package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import com.shadowbody.app.domain.model.MuscleGroup

/**
 * Phase 4: everything the generator needs, already gathered from Room.
 *
 * Keeping the inputs as plain data is what makes generation testable without
 * a database, and keeps the decision reproducible from the stored snapshot.
 */
data class GenerationContext(
    val now: Long,
    val equipment: Set<Equipment>,
    val goals: Set<Goal> = emptySet(),
    val fitnessLevel: FitnessLevel? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val age: Int? = null,
    val aggressionLevel: Int = 3,
    /** Planned session length; null when no profile exists yet. */
    val sessionMinutes: Int? = null,
    /** ISO day numbers (1 = Monday) the user wants to train. */
    val trainingDays: Set<Int> = emptySet(),
    val plans: List<PlanCandidate> = emptyList(),
    /** Library fallback when the user has no usable plan. */
    val library: List<SlotCandidate> = emptyList(),
    /** Current per-exercise adaptation state keyed by exercise id. */
    val adaptations: Map<Long, ExerciseProgress> = emptyMap(),
    val readiness: ReadinessSnapshot? = null,
    /** Completed sessions in the last [AdaptationLimits.WEEK_WINDOW_MS]. */
    val completedThisWeek: Int = 0,
    /** Consecutive user-reported missed sessions. */
    val missedSessions: Int = 0,
)

/** A saved plan reduced to what generation needs. */
data class PlanCandidate(
    val id: Long,
    val name: String,
    val targetDurationMin: Int? = null,
    /** Completed sessions referencing this plan (all time). */
    val completedCount: Int = 0,
    /** Ended-at of the most recent completed session, 0 when never trained. */
    val lastCompletedAt: Long = 0,
    val slots: List<SlotCandidate>,
)

/** One exercise as generation sees it. */
data class SlotCandidate(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val category: ExerciseCategory,
    val sets: Int,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val restSec: Int = 60,
)

/** One exercise inside a generated workout, with its reason attached. */
data class GeneratedExercise(
    val exerciseId: Long,
    val exerciseName: String,
    val position: Int,
    val sets: Int,
    val reps: Int?,
    val durationSec: Int?,
    val restSec: Int,
    val reason: AdaptationReason,
    /** Target this decision moved away from, for the explanation line. */
    val previousTargetText: String,
) {
    fun targetText(): String = if (reps != null) "$sets x $reps" else "$sets x ${durationSec ?: 0}s"
}

/** The generator's output: a complete, explainable next workout. */
data class GeneratedWorkout(
    val name: String,
    val sourcePlanId: Long?,
    val estimatedMinutes: Int,
    val summary: String,
    val exercises: List<GeneratedExercise>,
    val progressed: Int,
    val maintained: Int,
    val reduced: Int,
) {
    val isEmpty: Boolean get() = exercises.isEmpty()
}
