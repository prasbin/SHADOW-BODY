package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationLogDao {
    @Query("SELECT * FROM hydration_log WHERE dayKey = :dayKey ORDER BY loggedAt DESC")
    fun getLogsForDay(dayKey: String): Flow<List<HydrationLog>>

    @Query("SELECT * FROM hydration_log WHERE dayKey = :dayKey ORDER BY loggedAt DESC")
    suspend fun getLogsForDaySync(dayKey: String): List<HydrationLog>

    @Insert
    suspend fun insert(log: HydrationLog): Long

    @Query("DELETE FROM hydration_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT SUM(amountMl) FROM hydration_log WHERE dayKey = :dayKey")
    fun getTotalMlForDay(dayKey: String): Flow<Int?>

    @Query("SELECT SUM(amountMl) FROM hydration_log WHERE dayKey = :dayKey")
    suspend fun getTotalMlForDaySync(dayKey: String): Int?
}
