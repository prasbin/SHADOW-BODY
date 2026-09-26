package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanExerciseDao {
    @Query("SELECT * FROM workout_plan_exercise WHERE planId = :planId ORDER BY position")
    fun observeByPlan(planId: Long): Flow<List<WorkoutPlanExercise>>

    @Query("SELECT * FROM workout_plan_exercise WHERE planId = :planId ORDER BY position")
    suspend fun getByPlan(planId: Long): List<WorkoutPlanExercise>

    /** Duplicate slot (same exercise twice) aborts at the DB constraint. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(slot: WorkoutPlanExercise): Long

    @Update
    suspend fun update(slot: WorkoutPlanExercise)

    @Query("DELETE FROM workout_plan_exercise WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workout_plan_exercise WHERE planId = :planId")
    suspend fun deleteByPlan(planId: Long)
}
