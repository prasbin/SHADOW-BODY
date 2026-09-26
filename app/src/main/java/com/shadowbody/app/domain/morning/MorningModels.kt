package com.shadowbody.app.domain.morning

import com.shadowbody.app.data.local.MorningRoutineLog
import com.shadowbody.app.data.local.MorningRoutineStep
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.model.MorningStepOutcome

/** Where "today" stands for one routine. */
enum class MorningDayStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}

/**
 * Phase 5 daily state, resolved from the day's run rows.
 *
 * [resolve] is the single place that decides precedence, so the dashboard card,
 * the activation screen and the run screen can never disagree about today.
 */
data class MorningDayState(
    val dayKey: String,
    val status: MorningDayStatus = MorningDayStatus.NOT_STARTED,
    val routineId: Long? = null,
    val routineName: String = "",
    val activeLogId: Long? = null,
    val completedSteps: Int = 0,
    val skippedSteps: Int = 0,
    val totalSteps: Int = 0,
) {
    /** Steps dealt with, completed or skipped, over the step total. */
    val progress: Float
        get() = if (totalSteps <= 0) 0f else (completedSteps + skippedSteps).toFloat() / totalSteps

    val isFinished: Boolean get() = status == MorningDayStatus.COMPLETED

    companion object {
        /**
         * An unfinished run always wins: it is what the user is in the middle
         * of. Otherwise a completion beats an abandoned attempt, and an
         * abandoned attempt still counts as "started".
         */
        fun resolve(
            dayKey: String,
            routineId: Long?,
            logs: List<MorningRoutineLog>,
        ): MorningDayState {
            if (routineId == null || logs.isEmpty()) {
                return MorningDayState(dayKey = dayKey)
            }
            val inProgress = logs.firstOrNull {
                it.routineId == routineId && it.status == MorningLogStatus.IN_PROGRESS
            }
            val pick = inProgress
                ?: logs.firstOrNull {
                    it.routineId == routineId && it.status == MorningLogStatus.COMPLETED
                }
                ?: logs.firstOrNull { it.routineId == routineId }
            ?: return MorningDayState(dayKey = dayKey)
            val status = when (pick.status) {
                MorningLogStatus.IN_PROGRESS -> MorningDayStatus.IN_PROGRESS
                MorningLogStatus.COMPLETED -> MorningDayStatus.COMPLETED
                MorningLogStatus.ABANDONED -> MorningDayStatus.ABANDONED
            }
            return MorningDayState(
                dayKey = dayKey,
                status = status,
                routineId = pick.routineId,
                routineName = pick.routineName,
                activeLogId = pick.id,
                completedSteps = pick.completedSteps,
                skippedSteps = pick.skippedSteps,
                totalSteps = pick.totalSteps,
            )
        }
    }
}

/**
 * One step of a run, joined with whatever the user has recorded for it so far.
 * [step] is null when the routine step has since been deleted: the snapshot
 * fields still describe what was actually run.
 */
data class MorningRunStep(
    val position: Int,
    val title: String,
    val instructions: String,
    val category: MorningStepCategory,
    val targetDurationSec: Int? = null,
    val targetReps: Int? = null,
    val stepId: Long? = null,
    val outcome: MorningStepOutcome? = null,
    val elapsedSec: Int? = null,
) {
    val isTimed: Boolean get() = targetDurationSec != null && targetDurationSec > 0
    val isCounted: Boolean get() = targetReps != null && targetReps > 0
    val isPending: Boolean get() = outcome == null

    /** Human target, e.g. "60s" or "12 reps". Empty for a reminder step. */
    val targetText: String
        get() = when {
            isTimed && isCounted -> "${targetDurationSec}s · ${targetReps} reps"
            isTimed -> "${targetDurationSec}s"
            isCounted -> "$targetReps reps"
            else -> "REMINDER"
        }

    companion object {
        fun from(step: MorningRoutineStep): MorningRunStep = MorningRunStep(
            position = step.position,
            title = step.title,
            instructions = step.instructions,
            category = step.category,
            targetDurationSec = step.targetDurationSec,
            targetReps = step.targetReps,
            stepId = step.id,
        )
    }
}

/** A run plus its ordered steps: everything the run screen and result need. */
data class MorningRunDetail(
    val log: MorningRoutineLog,
    val steps: List<MorningRunStep>,
) {
    val currentStep: MorningRunStep? get() = steps.firstOrNull { it.isPending }

    val pendingCount: Int get() = steps.count { it.isPending }

    val dealtCount: Int get() = steps.size - pendingCount

    /** Every step has an explicit outcome, so the run can be closed. */
    val canFinish: Boolean get() = steps.isNotEmpty() && pendingCount == 0

    val decision: MorningFinishDecision
        get() = MorningCompletionRule.evaluate(
            totalSteps = log.totalSteps,
            completedSteps = log.completedSteps,
            skippedSteps = log.skippedSteps,
        )
}
