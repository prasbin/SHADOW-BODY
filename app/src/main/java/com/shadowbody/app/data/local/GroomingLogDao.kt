package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GroomingLogDao {

    @Insert
    protected abstract suspend fun insertLog(log: GroomingLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putStepLog(stepLog: GroomingStepLog): Long

    @Update
    protected abstract suspend fun updateLog(log: GroomingLog)

    @Query("DELETE FROM grooming_log WHERE id = :id")
    abstract suspend fun deleteLog(id: Long)

    /** Creates a run row with its step total already fixed. */
    @Transaction
    open suspend fun startRun(log: GroomingLog, totalSteps: Int): Long =
        insertLog(log.copy(totalSteps = totalSteps))

    /**
     * Records one step outcome and refreshes the parent counters from the rows
     * themselves, so the summary can never claim work that was not recorded.
     */
    @Transaction
    open suspend fun recordStepOutcome(
        log: GroomingLog,
        stepLog: GroomingStepLog,
    ): GroomingLog {
        putStepLog(stepLog)
        val rows = getStepLogs(log.id)
        val refreshed = log.copy(
            completedSteps = rows.count { it.outcome == "COMPLETED" },
            skippedSteps = rows.count { it.outcome == "SKIPPED" },
        )
        updateLog(refreshed)
        return refreshed
    }

    /** Stores the outcome of finishing or abandoning a run. */
    @Transaction
    open suspend fun closeRun(
        log: GroomingLog,
        status: String,
        completedAt: Long,
    ): GroomingLog {
        val closed = log.copy(
            status = status,
            completedAt = completedAt,
            completedSteps = getStepLogs(log.id).count { it.outcome == "COMPLETED" },
            skippedSteps = getStepLogs(log.id).count { it.outcome == "SKIPPED" },
        )
        updateLog(closed)
        return closed
    }

    // --- Reads ---

    @Query("SELECT * FROM grooming_log ORDER BY startedAt DESC, id DESC LIMIT :limit")
    abstract fun observeHistory(limit: Int): Flow<List<GroomingLog>>

    @Query("SELECT * FROM grooming_log ORDER BY startedAt DESC, id DESC LIMIT :limit")
    abstract suspend fun getHistory(limit: Int): List<GroomingLog>

    @Query("SELECT * FROM grooming_log WHERE id = :id")
    abstract suspend fun getLog(id: Long): GroomingLog?

    @Query("SELECT * FROM grooming_log WHERE id = :id")
    abstract fun observeLog(id: Long): Flow<GroomingLog?>

    @Query("SELECT * FROM grooming_step_log WHERE logId = :logId ORDER BY position")
    abstract fun observeStepLogs(logId: Long): Flow<List<GroomingStepLog>>

    @Query("SELECT * FROM grooming_step_log WHERE logId = :logId ORDER BY position")
    abstract suspend fun getStepLogs(logId: Long): List<GroomingStepLog>

    @Query("SELECT * FROM grooming_log WHERE dayKey = :dayKey ORDER BY startedAt DESC, id DESC")
    abstract fun observeForDay(dayKey: String): Flow<List<GroomingLog>>

    @Query("SELECT * FROM grooming_log WHERE dayKey = :dayKey ORDER BY startedAt DESC, id DESC")
    abstract suspend fun getForDay(dayKey: String): List<GroomingLog>

    @Query(
        "SELECT MAX(attempt) FROM grooming_log " +
            "WHERE routineId = :routineId AND dayKey = :dayKey",
    )
    abstract suspend fun maxAttempt(routineId: Long, dayKey: String): Int?

    /** Completions already recorded for this routine today, ignoring [exceptLogId]. */
    @Query(
        "SELECT COUNT(*) FROM grooming_log " +
            "WHERE routineId = :routineId AND dayKey = :dayKey AND status = 'COMPLETED' " +
            "AND id != :exceptLogId",
    )
    abstract suspend fun completionsForDay(routineId: Long, dayKey: String, exceptLogId: Long): Int

    @Query("SELECT COUNT(*) FROM grooming_log")
    abstract suspend fun count(): Int
}