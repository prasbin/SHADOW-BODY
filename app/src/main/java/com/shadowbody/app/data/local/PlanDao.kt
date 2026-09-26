package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM workout_plan WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun observePlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plan WHERE id = :id")
    suspend fun getById(id: Long): WorkoutPlan?

    @Query("SELECT * FROM workout_plan WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutPlan?>

    @Insert
    suspend fun insert(plan: WorkoutPlan): Long

    @Update
    suspend fun update(plan: WorkoutPlan)

    @Query("UPDATE workout_plan SET isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    /** Deleting a plan cascades to its slots only — never profile/baseline. */
    @Query("DELETE FROM workout_plan WHERE id = :id")
    suspend fun delete(id: Long)
}
