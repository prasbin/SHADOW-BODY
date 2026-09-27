package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.MorningRoutineDao
import com.shadowbody.app.data.local.MorningRoutineLog
import com.shadowbody.app.data.local.MorningRoutineLogDao
import com.shadowbody.app.data.local.MorningRoutineStep
import com.shadowbody.app.data.local.MorningRoutineStepDao
import com.shadowbody.app.data.local.MorningRoutineStepLog
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.model.MorningStepOutcome
import com.shadowbody.app.domain.morning.MorningCompletionRule
import com.shadowbody.app.domain.morning.MorningDayKey
import com.shadowbody.app.domain.morning.MorningDayState
import com.shadowbody.app.domain.morning.MorningDayStatus
import com.shadowbody.app.domain.morning.MorningFinishDecision
import com.shadowbody.app.domain.morning.MorningRunDetail
import com.shadowbody.app.domain.morning.MorningRunStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Result of asking to start today's routine. */
sealed interface MorningStartResult {
    /** A run is open: either freshly created ([resumed] false) or continued. */
    data class Run(val logId: Long, val resumed: Boolean) : MorningStartResult

    /** Today's routine is already completed; no duplicate run is created. */
    data class AlreadyCompleted(val logId: Long) : MorningStartResult

    data object NoEnabledSteps : MorningStartResult

    data object RoutineMissing : MorningStartResult
}

/** Result of recording one step. */
sealed interface MorningStepResult {
    data class Recorded(val log: MorningRoutineLog) : MorningStepResult

    /** The run is closed; history is not reopened for convenience. */
    data object RunClosed : MorningStepResult

    data object NotFound : MorningStepResult
}

/** Result of trying to close a run. */
sealed interface MorningFinishResult {
    data class Closed(val log: MorningRoutineLog, val completed: Boolean) : MorningFinishResult

    /** Steps are still untouched, so nothing was written. */
    data object NotReady : MorningFinishResult

    /** Today already has a completion; a second one is refused. */
    data object AlreadyCompleted : MorningFinishResult

    data object NotFound : MorningFinishResult
}

/**
 * Phase 5 execution store: performs the daily run and owns its history rules.
 *
 * The rules it enforces, in one place:
 * - one open run per routine per day, resumed rather than duplicated;
 * - no second completion of the same routine on the same day;
 * - a skipped step is stored as SKIPPED and never counted as completed;
 * - a run is only COMPLETED when [MorningCompletionRule] says the criteria are
 *   met, otherwise a partial run is stored as ABANDONED;
 * - completed sessions and step snapshots are immutable, so editing the routine
 *   later cannot rewrite what was recorded.
 */
class MorningActivationRepository(
    private val logs: MorningRoutineLogDao,
    private val routines: MorningRoutineDao,
    private val steps: MorningRoutineStepDao,
    private val progression: ProgressionRepository? = null,
) {

    // --- Daily state ---

    fun observeDay(routineId: Long?, dayKey: String): Flow<MorningDayState> =
        if (routineId == null) {
            flowOf(MorningDayState(dayKey = dayKey))
        } else {
            logs.observeForDay(dayKey).map { MorningDayState.resolve(dayKey, routineId, it) }
        }

    suspend fun dayState(routineId: Long?, dayKey: String): MorningDayState =
        MorningDayState.resolve(dayKey, routineId, logs.getForDay(dayKey))

    suspend fun todayState(routineId: Long?, now: Long = System.currentTimeMillis()): MorningDayState =
        dayState(routineId, MorningDayKey.of(now))

    // --- Execution ---

    suspend fun startRun(
        routineId: Long,
        dayKey: String = MorningDayKey.today(),
        now: Long = System.currentTimeMillis(),
    ): MorningStartResult {
        val routine = routines.getById(routineId) ?: return MorningStartResult.RoutineMissing
        val dayLogs = logs.getForDay(dayKey)
        val existing = MorningDayState.resolve(dayKey, routineId, dayLogs)
        when (existing.status) {
            MorningDayStatus.IN_PROGRESS ->
                return existing.activeLogId?.let {
                    MorningStartResult.Run(it, resumed = true)
                } ?: MorningStartResult.RoutineMissing

            MorningDayStatus.COMPLETED ->
                return existing.activeLogId?.let {
                    MorningStartResult.AlreadyCompleted(it)
                } ?: MorningStartResult.RoutineMissing

            else -> Unit
        }
        val enabled = steps.getEnabledByRoutine(routineId)
        if (enabled.isEmpty()) return MorningStartResult.NoEnabledSteps
        val attempt = (logs.maxAttempt(routineId, dayKey) ?: 0) + 1
        val logId = logs.startRun(
            log = MorningRoutineLog(
                routineId = routineId,
                routineName = routine.name,
                dayKey = dayKey,
                attempt = attempt,
                startedAt = now,
            ),
            totalSteps = enabled.size,
        )
        return MorningStartResult.Run(logId, resumed = false)
    }

    /**
     * Records an explicit outcome for one step. [step] carries the snapshot
     * fields, so the stored history describes what was actually on screen even
     * if the routine is edited a moment later.
     */
    suspend fun recordStep(
        logId: Long,
        step: MorningRunStep,
        outcome: MorningStepOutcome,
        elapsedSec: Int? = null,
        now: Long = System.currentTimeMillis(),
    ): MorningStepResult {
        val log = logs.getLog(logId) ?: return MorningStepResult.NotFound
        if (log.status != MorningLogStatus.IN_PROGRESS) return MorningStepResult.RunClosed
        val recorded = logs.recordStepOutcome(
            log = log,
            stepLog = MorningRoutineStepLog(
                logId = logId,
                stepId = step.stepId,
                position = step.position,
                title = step.title,
                category = step.category,
                targetDurationSec = step.targetDurationSec,
                targetReps = step.targetReps,
                outcome = outcome,
                elapsedSec = elapsedSec,
                recordedAt = now,
            ),
        )
        return MorningStepResult.Recorded(recorded)
    }

    suspend fun finish(
        logId: Long,
        now: Long = System.currentTimeMillis(),
    ): MorningFinishResult {
        val log = logs.getLog(logId) ?: return MorningFinishResult.NotFound
        if (log.status != MorningLogStatus.IN_PROGRESS) return MorningFinishResult.NotFound
        val decision = MorningCompletionRule.evaluate(
            totalSteps = log.totalSteps,
            completedSteps = log.completedSteps,
            skippedSteps = log.skippedSteps,
        )
        if (decision == MorningFinishDecision.NOT_READY) {
            return MorningFinishResult.NotReady
        }
        if (decision == MorningFinishDecision.COMPLETED) {
            val routineId = log.routineId
            val already = if (routineId == null) {
                0
            } else {
                logs.completionsForDay(routineId, log.dayKey, logId)
            }
            if (already > 0) return MorningFinishResult.AlreadyCompleted
        }
        val closed = logs.closeRun(
            log = log,
            status = MorningCompletionRule.statusFor(decision),
            completedAt = now,
        )
        val completed = closed.status == MorningLogStatus.COMPLETED
        if (completed) {
            // Award XP for completed morning activation
            progression?.awardXp(
                source = com.shadowbody.app.domain.progression.XpSource.MORNING_ACTIVATION,
                sourceRef = "morning:$logId",
            )?.let { result ->
                if (result is ProgressionRepository.AwardResult.Awarded) {
                    progression.processProgression()
                }
            }
        }
        return MorningFinishResult.Closed(closed, completed)
    }

    /** Ends a partial run without pretending it was finished. */
    suspend fun abandon(logId: Long, now: Long = System.currentTimeMillis()): Boolean {
        val log = logs.getLog(logId) ?: return false
        if (log.status != MorningLogStatus.IN_PROGRESS) return false
        logs.closeRun(log = log, status = MorningLogStatus.ABANDONED, completedAt = now)
        return true
    }

    // --- Reads ---

    fun observeRun(logId: Long): Flow<MorningRunDetail?> =
        combine(logs.observeLog(logId), logs.observeStepLogs(logId)) { log, stepLogs ->
            if (log == null) {
                null
            } else {
                MorningRunDetail(log, joinSteps(log, stepLogs))
            }
        }

    suspend fun runDetail(logId: Long): MorningRunDetail? {
        val log = logs.getLog(logId) ?: return null
        return MorningRunDetail(log, joinSteps(log, logs.getStepLogs(logId)))
    }

    fun observeHistory(limit: Int = 20): Flow<List<MorningRoutineLog>> = logs.observeHistory(limit)

    suspend fun history(limit: Int = 20): List<MorningRoutineLog> = logs.getHistory(limit)

    suspend fun logCount(): Int = logs.count()

    /**
     * Joins the routine's enabled steps with what the run recorded.
     *
     * Immutability rule: once a step has an outcome, the *snapshot* from the
     * run is what gets shown, so renaming, retiming or reordering the routine
     * afterwards can never rewrite what was recorded. Steps with no outcome yet
     * are still pending, so they reflect the routine as it is now.
     *
     * A step deleted since the run is described by its snapshot, and a run whose
     * routine is gone entirely still renders from its own step logs.
     */
    private suspend fun joinSteps(
        log: MorningRoutineLog,
        stepLogs: List<MorningRoutineStepLog>,
    ): List<MorningRunStep> {
        val byStepId = stepLogs.mapNotNull { row -> row.stepId?.let { it to row } }.toMap()
        val byPosition = stepLogs.associateBy { it.position }
        val routineSteps = log.routineId
            ?.let { steps.getEnabledByRoutine(it) }
            .orEmpty()
        val rows = routineSteps.map { step ->
            val row = byStepId[step.id] ?: byPosition[step.position]
            if (row != null) {
                MorningRunStep(
                    position = step.position,
                    title = row.title,
                    instructions = step.instructions,
                    category = row.category,
                    targetDurationSec = row.targetDurationSec,
                    targetReps = row.targetReps,
                    stepId = row.stepId,
                    outcome = row.outcome,
                    elapsedSec = row.elapsedSec,
                )
            } else {
                MorningRunStep.from(step)
            }
        }
        val knownPositions = rows.map { it.position }.toSet()
        val orphans = stepLogs.filter { it.position !in knownPositions }.map { row ->
            MorningRunStep(
                position = row.position,
                title = row.title,
                instructions = "",
                category = row.category,
                targetDurationSec = row.targetDurationSec,
                targetReps = row.targetReps,
                stepId = row.stepId,
                outcome = row.outcome,
                elapsedSec = row.elapsedSec,
            )
        }
        return (rows + orphans).sortedBy { it.position }
    }
}
