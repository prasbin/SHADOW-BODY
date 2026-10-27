package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDailyDecisionDao {
    @Query("SELECT * FROM outfit_daily_decision ORDER BY dayKey DESC")
    fun observeAll(): Flow<List<OutfitDailyDecision>>

    @Query("SELECT * FROM outfit_daily_decision WHERE dayKey = :dayKey")
    suspend fun getByDay(dayKey: String): OutfitDailyDecision?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(decision: OutfitDailyDecision): Long

    @Query("DELETE FROM outfit_daily_decision WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM outfit_daily_decision")
    suspend fun count(): Int
}
