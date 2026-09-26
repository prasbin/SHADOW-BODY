package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Phase 4: append-only readiness check-ins. */
@Dao
interface ReadinessDao {

    @Insert
    suspend fun insert(report: ReadinessReport): Long

    @Query("SELECT * FROM readiness_report ORDER BY recordedAt DESC, id DESC LIMIT 1")
    suspend fun latest(): ReadinessReport?

    @Query("SELECT * FROM readiness_report ORDER BY recordedAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<ReadinessReport?>

    /** The check-in that applied when a session ended, not today's answer. */
    @Query(
        "SELECT * FROM readiness_report WHERE recordedAt <= :at " +
            "ORDER BY recordedAt DESC, id DESC LIMIT 1",
    )
    suspend fun latestAtOrBefore(at: Long): ReadinessReport?

    @Query("SELECT * FROM readiness_report ORDER BY recordedAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<ReadinessReport>>

    @Query("SELECT COUNT(*) FROM readiness_report")
    suspend fun count(): Int
}
