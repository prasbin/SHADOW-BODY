package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.ProgressionState
import kotlin.math.min

/**
 * Phase 4: deterministic, local, explainable target selection.
 *
 * The engine is a pure function — no clock, no randomness, no I/O — so the
 * same inputs always produce the same decision. Rule order is fixed and *is*
 * the specification:
 *
 * 1. Clamp the incoming target into [AdaptationLimits].
 * 2. High reported fatigue or soreness reduces demand (never progresses).
 * 3. This week's planned sessions already done => maintain.
 * 4. Several consecutive missed sessions => one small bounded reduction.
 * 5. No completed session yet => maintain (insufficient data).
 * 6. Completed sets clearly below target => reduce sets.
 * 7. Recorded reps/time clearly below target => reduce reps/time.
 * 8. Enough consecutive successes at this target => progress by one small
 *    bounded step.
 * 9. Otherwise maintain.
 *
 * Missing data always falls through to maintenance, never to progression.
 * This is training-load bookkeeping, not medical advice.
 */
object WorkoutAdaptationEngine {

    private data class Target(
        val sets: Int,
        val reps: Int?,
        val durationSec: Int?,
        val restSec: Int,
    )

    /** Computes the next target for one exercise. Pure and side-effect free. */
    fun nextTarget(
        progress: ExerciseProgress,
        readiness: ReadinessSnapshot? = null,
        context: AdaptationContext = AdaptationContext(),
    ): AdaptationDecision {
        val incoming = Target(
            progress.currentSets,
            progress.currentReps,
            progress.currentDurationSec,
            progress.restSec,
        )
        val target = clamp(incoming)
        val wasClamped = target != incoming
        val id = progress.exerciseId

        // 2. Readiness first: a tired or sore user must never be progressed.
        fatigueDecision(id, readiness, target, wasClamped)?.let { return it }

        // 3. The week's planned work is already done.
        if (context.weeklyTargetMet) {
            return maintain(id, target, AdaptationReason.SCHEDULE_MET, wasClamped)
        }

        // 4. Missed sessions lower demand slightly, never drastically.
        if (context.missedSessions >= AdaptationLimits.MISSED_SESSIONS_TO_REDUCE) {
            return reduceVolume(id, target, AdaptationReason.MISSED_REDUCED, wasClamped)
        }

        // 5-9. Evidence from the last completed session.
        val result = progress.lastResult
            ?: return maintain(id, target, AdaptationReason.NO_DATA, wasClamped)

        if (result.completionRatio < AdaptationLimits.REGRESSION_COMPLETION_RATIO) {
            return reduceVolume(id, target, AdaptationReason.SETS_REDUCED, wasClamped)
        }

        val expectedReps = result.targetReps ?: target.reps
        if (target.reps != null && expectedReps != null) {
            val ratio = (result.actualReps ?: 0).toDouble() / expectedReps.toDouble()
            if (ratio < AdaptationLimits.REGRESSION_REP_RATIO) {
                return reduceRepsOrTime(id, target, AdaptationReason.REPS_REDUCED, wasClamped)
            }
        }

        val expectedDuration = result.targetDurationSec ?: target.durationSec
        if (target.durationSec != null && expectedDuration != null) {
            val ratio = (result.actualDurationSec ?: 0).toDouble() / expectedDuration.toDouble()
            if (ratio < AdaptationLimits.REGRESSION_DURATION_RATIO) {
                return reduceRepsOrTime(id, target, AdaptationReason.TIME_REDUCED, wasClamped)
            }
        }

        // Success. Two clean sessions at this target are required before any
        // increase, so one lucky session can never escalate the load.
        if (progress.sessionsAtTarget + 1 < AdaptationLimits.SESSIONS_TO_PROGRESS) {
            return maintain(id, target, AdaptationReason.SINGLE_SUCCESS, wasClamped)
        }
        return progressOneStep(id, target, wasClamped)
    }

    /**
     * Baseline target for an exercise the engine has never seen. Clamped, and
     * always reported as "no data" so the UI never implies earned progress.
     */
    fun initialTarget(
        exerciseId: Long,
        sets: Int,
        reps: Int?,
        durationSec: Int?,
        restSec: Int,
    ): AdaptationDecision {
        val target = clamp(
            Target(
                sets = sets,
                reps = reps,
                durationSec = durationSec,
                restSec = restSec,
            ),
        )
        return AdaptationDecision(
            exerciseId = exerciseId,
            sets = target.sets,
            reps = target.reps,
            durationSec = target.durationSec,
            restSec = target.restSec,
            state = ProgressionState.UNTRACKED,
            reason = AdaptationReason.NO_DATA,
            clamped = target != Target(sets, reps, durationSec, restSec),
        )
    }

    // --- Rule 1: bounds -----------------------------------------------------

    private fun clamp(target: Target): Target {
        val sets = target.sets.coerceIn(AdaptationLimits.MIN_SETS, AdaptationLimits.MAX_SETS)
        val rest = target.restSec.coerceIn(AdaptationLimits.MIN_REST_SEC, AdaptationLimits.MAX_REST_SEC)
        // Reps null means time-based; a zero/absent target is lifted to the
        // floor rather than left impossible.
        return if (target.reps == null) {
            Target(
                sets = sets,
                reps = null,
                durationSec = (target.durationSec ?: AdaptationLimits.MIN_DURATION_SEC)
                    .coerceIn(AdaptationLimits.MIN_DURATION_SEC, AdaptationLimits.MAX_DURATION_SEC),
                restSec = rest,
            )
        } else {
            Target(
                sets = sets,
                reps = target.reps.coerceIn(AdaptationLimits.MIN_REPS, AdaptationLimits.MAX_REPS),
                durationSec = null,
                restSec = rest,
            )
        }
    }

    // --- Rule 2: readiness --------------------------------------------------

    private fun fatigueDecision(
        exerciseId: Long,
        readiness: ReadinessSnapshot?,
        target: Target,
        wasClamped: Boolean,
    ): AdaptationDecision? {
        if (readiness == null) return null
        val highFatigue = readiness.fatigue >= AdaptationLimits.HIGH_FATIGUE_THRESHOLD
        val highSoreness = readiness.soreness >= AdaptationLimits.HIGH_SORENESS_THRESHOLD
        if (!highFatigue && !highSoreness) return null
        return reduceVolume(
            exerciseId = exerciseId,
            target = target,
            reason = if (highFatigue) {
                AdaptationReason.FATIGUE_REDUCED
            } else {
                AdaptationReason.SORENESS_REDUCED
            },
            wasClamped = wasClamped,
        )
    }

    // --- Rules 6-8: evidence ------------------------------------------------

    private fun progressOneStep(exerciseId: Long, target: Target, wasClamped: Boolean): AdaptationDecision {
        if (target.sets < AdaptationLimits.MAX_SETS) {
            return AdaptationDecision(
                exerciseId = exerciseId,
                sets = target.sets + AdaptationLimits.SETS_STEP,
                reps = target.reps,
                durationSec = target.durationSec,
                restSec = target.restSec,
                state = ProgressionState.PROGRESSING,
                reason = AdaptationReason.SETS_PROGRESSED,
                clamped = wasClamped,
            )
        }
        val reps = target.reps
        if (reps != null && reps < AdaptationLimits.MAX_REPS) {
            return AdaptationDecision(
                exerciseId = exerciseId,
                sets = target.sets,
                reps = min(AdaptationLimits.MAX_REPS, reps + AdaptationLimits.MAX_REP_STEP),
                durationSec = null,
                restSec = target.restSec,
                state = ProgressionState.PROGRESSING,
                reason = AdaptationReason.REPS_PROGRESSED,
                clamped = wasClamped,
            )
        }
        val duration = target.durationSec
        if (duration != null && duration < AdaptationLimits.MAX_DURATION_SEC) {
            return AdaptationDecision(
                exerciseId = exerciseId,
                sets = target.sets,
                reps = null,
                durationSec = min(
                    AdaptationLimits.MAX_DURATION_SEC,
                    duration + AdaptationLimits.MAX_DURATION_STEP_SEC,
                ),
                restSec = target.restSec,
                state = ProgressionState.PROGRESSING,
                reason = AdaptationReason.TIME_PROGRESSED,
                clamped = wasClamped,
            )
        }
        // Everything is already at its ceiling: hold, do not push.
        return maintain(exerciseId, target, AdaptationReason.AT_LIMIT, wasClamped)
    }

    // --- Reductions ---------------------------------------------------------

    /**
     * One bounded step down: sets first, and only once sets are at the floor
     * does reps/time come down. A little extra rest is granted so the
     * reduction is also a recovery, never just less work.
     */
    private fun reduceVolume(
        exerciseId: Long,
        target: Target,
        reason: AdaptationReason,
        wasClamped: Boolean,
    ): AdaptationDecision {
        val canDropSet = target.sets > AdaptationLimits.MIN_SETS
        return AdaptationDecision(
            exerciseId = exerciseId,
            sets = if (canDropSet) {
                (target.sets - AdaptationLimits.SETS_STEP).coerceAtLeast(AdaptationLimits.MIN_SETS)
            } else {
                target.sets
            },
            reps = if (!canDropSet) lowerReps(target.reps) else target.reps,
            durationSec = if (!canDropSet) lowerDuration(target.durationSec) else target.durationSec,
            restSec = addRest(target.restSec),
            state = ProgressionState.REGRESSING,
            reason = reason,
            clamped = wasClamped,
        )
    }

    /** Reps/time specific reduction; falls back to volume when impossible. */
    private fun reduceRepsOrTime(
        exerciseId: Long,
        target: Target,
        reason: AdaptationReason,
        wasClamped: Boolean,
    ): AdaptationDecision {
        val lowerReps = lowerReps(target.reps)
        val lowerDuration = lowerDuration(target.durationSec)
        val changed = lowerReps != target.reps || lowerDuration != target.durationSec
        if (!changed) return reduceVolume(exerciseId, target, reason, wasClamped)
        return AdaptationDecision(
            exerciseId = exerciseId,
            sets = target.sets,
            reps = lowerReps,
            durationSec = lowerDuration,
            restSec = addRest(target.restSec),
            state = ProgressionState.REGRESSING,
            reason = reason,
            clamped = wasClamped,
        )
    }

    private fun lowerReps(reps: Int?): Int? = reps?.let {
        (it - AdaptationLimits.MAX_REP_STEP).coerceAtLeast(AdaptationLimits.MIN_REPS)
    }

    private fun lowerDuration(durationSec: Int?): Int? = durationSec?.let {
        (it - AdaptationLimits.MAX_DURATION_STEP_SEC).coerceAtLeast(AdaptationLimits.MIN_DURATION_SEC)
    }

    private fun addRest(restSec: Int): Int =
        (restSec + AdaptationLimits.REST_STEP_SEC).coerceAtMost(AdaptationLimits.MAX_REST_SEC)

    private fun maintain(
        exerciseId: Long,
        target: Target,
        reason: AdaptationReason,
        wasClamped: Boolean,
    ) = AdaptationDecision(
        exerciseId = exerciseId,
        sets = target.sets,
        reps = target.reps,
        durationSec = target.durationSec,
        restSec = target.restSec,
        state = when (reason) {
            AdaptationReason.NO_DATA -> ProgressionState.UNTRACKED
            else -> ProgressionState.MAINTAINING
        },
        reason = reason,
        clamped = wasClamped,
    )
}
