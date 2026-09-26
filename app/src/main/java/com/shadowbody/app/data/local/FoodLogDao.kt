package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_log WHERE dayKey = :dayKey ORDER BY loggedAt DESC")
    fun getLogsForDay(dayKey: String): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_log WHERE dayKey = :dayKey ORDER BY loggedAt DESC")
    suspend fun getLogsForDaySync(dayKey: String): List<FoodLog>

    @Insert
    suspend fun insert(log: FoodLog): Long

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT DISTINCT dayKey FROM food_log ORDER BY dayKey DESC")
    fun getAllLoggedDays(): Flow<List<String>>
}
