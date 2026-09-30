package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.GroomingLog
import com.shadowbody.app.data.local.GroomingLogDao
import com.shadowbody.app.data.local.GroomingPreferences
import com.shadowbody.app.data.local.GroomingPreferencesDao
import com.shadowbody.app.data.local.GroomingRoutine
import com.shadowbody.app.data.local.GroomingRoutineDao
import com.shadowbody.app.data.local.GroomingRoutineStep
import com.shadowbody.app.data.local.GroomingRoutineStepDao
import com.shadowbody.app.data.local.GroomingStepLog
import com.shadowbody.app.domain.grooming.GroomingDayKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class GroomingDayState(
    val dayKey: String,
    val routineId: Long?,
    val status: String, // NOT_STARTED, IN_PROGRESS, COMPLETED
    val activeLogId: Long? = null,
    val completedSteps: Int = 0,
    val skippedSteps: Int = 0,
    val totalSteps: Int = 0,
) {
    companion object {
        fun resolve(dayKey: String, routineId: Long?, logs: List<GroomingLog>): GroomingDayState {
            val todayLogs = logs.filter { it.dayKey == dayKey && (routineId == null || it.routineId == routineId) }
            if (todayLogs.isEmpty()) {
                return GroomingDayState(dayKey = dayKey, routineId = routineId, status = "NOT_STARTED")
            }
            val inProgress = todayLogs.firstOrNull { it.status == "IN_PROGRESS" }
            if (inProgress != null) {
                return GroomingDayState(
                    dayKey = dayKey,
                    routineId = routineId,
                    status = "IN_PROGRESS",
                    activeLogId = inProgress.id,
                    completedSteps = inProgress.completedSteps,
                    skippedSteps = inProgress.skippedSteps,
                    totalSteps = inProgress.totalSteps,
                )
            }
            val completed = todayLogs.firstOrNull { it.status == "COMPLETED" }
            if (completed != null) {
                return GroomingDayState(
                    dayKey = dayKey,
                    routineId = routineId,
                    status = "COMPLETED",
                    activeLogId = completed.id,
                    completedSteps = completed.completedSteps,
                    skippedSteps = completed.skippedSteps,
                    totalSteps = completed.totalSteps,
                )
            }
            return GroomingDayState(dayKey = dayKey, routineId = routineId, status = "NOT_STARTED")
        }
    }
}

sealed interface GroomingStartResult {
    data class Run(val logId: Long, val resumed: Boolean) : GroomingStartResult
    data class AlreadyCompleted(val logId: Long) : GroomingStartResult
    data object NoEnabledSteps : GroomingStartResult
    data object RoutineMissing : GroomingStartResult
}

sealed interface GroomingStepResult {
    data class Recorded(val log: GroomingLog) : GroomingStepResult
    data object RunClosed : GroomingStepResult
    data object NotFound : GroomingStepResult
}

sealed interface GroomingFinishResult {
    data class Closed(val log: GroomingLog, val completed: Boolean) : GroomingFinishResult
    data object NotReady : GroomingFinishResult
    data object AlreadyCompleted : GroomingFinishResult
    data object NotFound : GroomingFinishResult
}

class GroomingRepository(
    private val prefsDao: GroomingPreferencesDao,
    private val routineDao: GroomingRoutineDao,
    private val stepDao: GroomingRoutineStepDao,
    private val logDao: GroomingLogDao,
) {

    // --- Preferences ---

    val preferences: Flow<GroomingPreferences?> = prefsDao.observe()

    suspend fun getPreferencesSync(): GroomingPreferences? = prefsDao.getSync()

    suspend fun savePreferences(prefs: GroomingPreferences) {
        prefsDao.upsert(prefs)
    }

    // --- Routines ---

    val activeRoutines: Flow<List<GroomingRoutine>> = routineDao.observeActiveRoutines()

    val allRoutines: Flow<List<GroomingRoutine>> = routineDao.observeAllRoutines()

    suspend fun getRoutine(id: Long): GroomingRoutine? = routineDao.getById(id)

    suspend fun getRoutineBySeedKey(seedKey: String): GroomingRoutine? = routineDao.getBySeedKey(seedKey)

    suspend fun createRoutine(routine: GroomingRoutine): Long {
        val maxOrder = routineDao.maxSortOrder() ?: 0
        return routineDao.insert(routine.copy(sortOrder = maxOrder + 1))
    }

    suspend fun updateRoutine(routine: GroomingRoutine) {
        routineDao.update(routine.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRoutine(id: Long) {
        routineDao.deleteById(id)
    }

    suspend fun reorderRoutines(orderedRoutines: List<GroomingRoutine>) {
        orderedRoutines.forEachIndexed { index, routine ->
            routineDao.update(routine.copy(sortOrder = index, updatedAt = System.currentTimeMillis()))
        }
    }

    // --- Routine Steps ---

    fun observeSteps(routineId: Long): Flow<List<GroomingRoutineStep>> =
        stepDao.observeEnabledByRoutine(routineId)

    suspend fun getSteps(routineId: Long): List<GroomingRoutineStep> =
        stepDao.getEnabledByRoutine(routineId)

    suspend fun createStep(step: GroomingRoutineStep): Long = stepDao.insert(step)

    suspend fun updateStep(step: GroomingRoutineStep) {
        stepDao.update(step)
    }

    suspend fun deleteStep(id: Long) {
        stepDao.deleteById(id)
    }

    suspend fun reorderSteps(routineId: Long, steps: List<GroomingRoutineStep>) {
        stepDao.reorderSteps(routineId, steps)
    }

    // --- Daily state ---

    fun observeDay(routineId: Long?, dayKey: String): Flow<GroomingDayState> =
        if (routineId == null) {
            kotlinx.coroutines.flow.flowOf(GroomingDayState(dayKey = dayKey, routineId = null, status = "NOT_STARTED"))
        } else {
            logDao.observeForDay(dayKey).map { GroomingDayState.resolve(dayKey, routineId, it) }
        }

    suspend fun dayState(routineId: Long?, dayKey: String): GroomingDayState =
        GroomingDayState.resolve(dayKey, routineId, logDao.getForDay(dayKey))

    suspend fun todayState(routineId: Long?, now: Long = System.currentTimeMillis()): GroomingDayState =
        dayState(routineId, GroomingDayKey.of(now))

    // --- Execution ---

    suspend fun startRun(
        routineId: Long,
        dayKey: String = GroomingDayKey.today(),
        now: Long = System.currentTimeMillis(),
    ): GroomingStartResult {
        val routine = routineDao.getById(routineId) ?: return GroomingStartResult.RoutineMissing
        val dayLogs = logDao.getForDay(dayKey)
        val existing = GroomingDayState.resolve(dayKey, routineId, dayLogs)
        when (existing.status) {
            "IN_PROGRESS" ->
                return existing.activeLogId?.let {
                    GroomingStartResult.Run(it, resumed = true)
                } ?: GroomingStartResult.RoutineMissing

            "COMPLETED" ->
                return existing.activeLogId?.let {
                    GroomingStartResult.AlreadyCompleted(it)
                } ?: GroomingStartResult.RoutineMissing

            else -> Unit
        }
        val enabled = stepDao.getEnabledByRoutine(routineId)
        if (enabled.isEmpty()) return GroomingStartResult.NoEnabledSteps
        val attempt = (logDao.maxAttempt(routineId, dayKey) ?: 0) + 1
        val logId = logDao.startRun(
            log = GroomingLog(
                routineId = routineId,
                routineName = routine.name,
                dayKey = dayKey,
                attempt = attempt,
                startedAt = now,
                totalSteps = enabled.size,
            ),
            totalSteps = enabled.size,
        )
        return GroomingStartResult.Run(logId, resumed = false)
    }

    suspend fun recordStep(
        logId: Long,
        step: GroomingRoutineStep,
        outcome: String, // COMPLETED, SKIPPED
        elapsedSec: Int? = null,
        now: Long = System.currentTimeMillis(),
    ): GroomingStepResult {
        val log = logDao.getLog(logId) ?: return GroomingStepResult.NotFound
        if (log.status != "IN_PROGRESS") return GroomingStepResult.RunClosed
        val recorded = logDao.recordStepOutcome(
            log = log,
            stepLog = GroomingStepLog(
                logId = logId,
                stepId = step.id,
                position = step.position,
                title = step.title,
                category = step.category,
                targetDurationSec = step.targetDurationSec,
                outcome = outcome,
                elapsedSec = elapsedSec,
                recordedAt = now,
            ),
        )
        return GroomingStepResult.Recorded(recorded)
    }

    suspend fun finish(
        logId: Long,
        now: Long = System.currentTimeMillis(),
    ): GroomingFinishResult {
        val log = logDao.getLog(logId) ?: return GroomingFinishResult.NotFound
        if (log.status != "IN_PROGRESS") return GroomingFinishResult.NotFound
        // For grooming, all steps must be dealt with (completed or skipped)
        val allDealtWith = log.completedSteps + log.skippedSteps >= log.totalSteps
        if (!allDealtWith) return GroomingFinishResult.NotReady
        // Check if already completed today
        val routineId = log.routineId
        val already = if (routineId == null) {
            0
        } else {
            logDao.completionsForDay(routineId, log.dayKey, logId)
        }
        if (already > 0) return GroomingFinishResult.AlreadyCompleted
        val closed = logDao.closeRun(
            log = log,
            status = "COMPLETED",
            completedAt = now,
        )
        return GroomingFinishResult.Closed(closed, completed = true)
    }

    suspend fun abandon(logId: Long, now: Long = System.currentTimeMillis()): Boolean {
        val log = logDao.getLog(logId) ?: return false
        if (log.status != "IN_PROGRESS") return false
        logDao.closeRun(log = log, status = "ABANDONED", completedAt = now)
        return true
    }

    // --- Reads ---

    fun observeRun(logId: Long): Flow<GroomingRunDetail?> =
        combine(logDao.observeLog(logId), logDao.observeStepLogs(logId)) { log, stepLogs ->
            if (log == null) null else GroomingRunDetail(log, joinSteps(log, stepLogs))
        }

    suspend fun runDetail(logId: Long): GroomingRunDetail? {
        val log = logDao.getLog(logId) ?: return null
        return GroomingRunDetail(log, joinSteps(log, logDao.getStepLogs(logId)))
    }

    fun observeHistory(limit: Int = 20): Flow<List<GroomingLog>> = logDao.observeHistory(limit)

    suspend fun history(limit: Int = 20): List<GroomingLog> = logDao.getHistory(limit)

    suspend fun logCount(): Int = logDao.count()

    // --- Seeding ---

    suspend fun ensureSeeded() {
        val seeded = routineDao.getSeededRoutines()
        if (seeded.isEmpty()) {
            // The migration seeds the built-in routine
        }
    }

    private suspend fun joinSteps(
        log: GroomingLog,
        stepLogs: List<GroomingStepLog>,
    ): List<GroomingRunStep> {
        val byStepId = stepLogs.mapNotNull { row -> row.stepId?.let { it to row } }.toMap()
        val byPosition = stepLogs.associateBy { it.position }
        val routineSteps = log.routineId
            ?.let { stepDao.getEnabledByRoutine(it) }
            .orEmpty()
        val rows = routineSteps.map { step ->
            val row = byStepId[step.id] ?: byPosition[step.position]
            if (row != null) {
                GroomingRunStep(
                    position = step.position,
                    title = row.title,
                    instructions = step.instructions,
                    category = row.category,
                    targetDurationSec = row.targetDurationSec,
                    stepId = row.stepId,
                    outcome = row.outcome,
                    elapsedSec = row.elapsedSec,
                )
            } else {
                GroomingRunStep.from(step)
            }
        }
        val knownPositions = rows.map { it.position }.toSet()
        val orphans = stepLogs.filter { it.position !in knownPositions }.map { row ->
            GroomingRunStep(
                position = row.position,
                title = row.title,
                instructions = "",
                category = row.category,
                targetDurationSec = row.targetDurationSec,
                stepId = row.stepId,
                outcome = row.outcome,
                elapsedSec = row.elapsedSec,
            )
        }
        return (rows + orphans).sortedBy { it.position }
    }
}

data class GroomingRunDetail(
    val log: GroomingLog,
    val steps: List<GroomingRunStep>,
)

data class GroomingRunStep(
    val position: Int,
    val title: String,
    val instructions: String,
    val category: String,
    val targetDurationSec: Int?,
    val stepId: Long?,
    val outcome: String?,
    val elapsedSec: Int?,
) {
    companion object {
        fun from(step: GroomingRoutineStep): GroomingRunStep = GroomingRunStep(
            position = step.position,
            title = step.title,
            instructions = step.instructions,
            category = step.category,
            targetDurationSec = step.targetDurationSec,
            stepId = step.id,
            outcome = null,
            elapsedSec = null,
        )
    }
}