package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.model.MorningStepOutcome
import kotlinx.coroutines.flow.Flow

/**
 * Phase 5 run history.
 *
 * The two multi-table writes ([startRun], [recordStepOutcome]) are transactions
 * so a run row and its step outcomes can never disagree.
 */
@Dao
abstract class MorningRoutineLogDao {

    // --- Writes ---

    @Insert
    protected abstract suspend fun insertLog(log: MorningRoutineLog): Long

    /**
     * REPLACE on the unique `(logId, position)` index makes re-marking a step
     * idempotent instead of duplicating it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putStepLog(stepLog: MorningRoutineStepLog): Long

    @Update
    protected abstract suspend fun updateLog(log: MorningRoutineLog)

    @Query("DELETE FROM morning_routine_log WHERE id = :id")
    abstract suspend fun deleteLog(id: Long)

    /** Creates a run row with its step total already fixed. */
    @Transaction
    open suspend fun startRun(log: MorningRoutineLog, totalSteps: Int): Long =
        insertLog(log.copy(totalSteps = totalSteps))

    /**
     * Records one step outcome and refreshes the parent counters from the rows
     * themselves, so the summary can never claim work that was not recorded.
     */
    @Transaction
    open suspend fun recordStepOutcome(
        log: MorningRoutineLog,
        stepLog: MorningRoutineStepLog,
    ): MorningRoutineLog {
        putStepLog(stepLog)
        val rows = getStepLogs(log.id)
        val refreshed = log.copy(
            completedSteps = rows.count { it.outcome == MorningStepOutcome.COMPLETED },
            skippedSteps = rows.count { it.outcome == MorningStepOutcome.SKIPPED },
        )
        updateLog(refreshed)
        return refreshed
    }

    /** Stores the outcome of finishing or abandoning a run. */
    @Transaction
    open suspend fun closeRun(
        log: MorningRoutineLog,
        status: MorningLogStatus,
        completedAt: Long,
    ): MorningRoutineLog {
        val closed = log.copy(
            status = status,
            completedAt = completedAt,
            completedSteps = getStepLogs(log.id).count { it.outcome == MorningStepOutcome.COMPLETED },
            skippedSteps = getStepLogs(log.id).count { it.outcome == MorningStepOutcome.SKIPPED },
        )
        updateLog(closed)
        return closed
    }

    // --- Reads ---

    @Query("SELECT * FROM morning_routine_log ORDER BY startedAt DESC, id DESC LIMIT :limit")
    abstract fun observeHistory(limit: Int): Flow<List<MorningRoutineLog>>

    @Query("SELECT * FROM morning_routine_log ORDER BY startedAt DESC, id DESC LIMIT :limit")
    abstract suspend fun getHistory(limit: Int): List<MorningRoutineLog>

    @Query("SELECT * FROM morning_routine_log WHERE id = :id")
    abstract suspend fun getLog(id: Long): MorningRoutineLog?

    @Query("SELECT * FROM morning_routine_log WHERE id = :id")
    abstract fun observeLog(id: Long): Flow<MorningRoutineLog?>

    @Query("SELECT * FROM morning_routine_step_log WHERE logId = :logId ORDER BY position")
    abstract fun observeStepLogs(logId: Long): Flow<List<MorningRoutineStepLog>>

    @Query("SELECT * FROM morning_routine_step_log WHERE logId = :logId ORDER BY position")
    abstract suspend fun getStepLogs(logId: Long): List<MorningRoutineStepLog>

    @Query("SELECT * FROM morning_routine_log WHERE dayKey = :dayKey ORDER BY startedAt DESC, id DESC")
    abstract fun observeForDay(dayKey: String): Flow<List<MorningRoutineLog>>

    @Query("SELECT * FROM morning_routine_log WHERE dayKey = :dayKey ORDER BY startedAt DESC, id DESC")
    abstract suspend fun getForDay(dayKey: String): List<MorningRoutineLog>

    @Query(
        "SELECT MAX(attempt) FROM morning_routine_log " +
            "WHERE routineId = :routineId AND dayKey = :dayKey",
    )
    abstract suspend fun maxAttempt(routineId: Long, dayKey: String): Int?

    /** Completions already recorded for this routine today, ignoring [exceptLogId]. */
    @Query(
        "SELECT COUNT(*) FROM morning_routine_log " +
            "WHERE routineId = :routineId AND dayKey = :dayKey AND status = 'COMPLETED' " +
            "AND id != :exceptLogId",
    )
    abstract suspend fun completionsForDay(routineId: Long, dayKey: String, exceptLogId: Long): Int

    @Query("SELECT COUNT(*) FROM morning_routine_log")
    abstract suspend fun count(): Int
}
