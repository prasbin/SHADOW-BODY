package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Phase 4: user-reported skipped workouts. Only ever written by an explicit
 * user action — nothing is inferred.
 */
@Dao
interface MissedWorkoutDao {

    @Insert
    suspend fun insert(missed: MissedWorkout): Long

    @Query("SELECT * FROM missed_workout ORDER BY recordedAt DESC, id DESC LIMIT 1")
    suspend fun latest(): MissedWorkout?

    @Query("SELECT * FROM missed_workout ORDER BY recordedAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<MissedWorkout>>

    @Query("SELECT COUNT(*) FROM missed_workout WHERE recordedAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM missed_workout")
    suspend fun count(): Int
}
