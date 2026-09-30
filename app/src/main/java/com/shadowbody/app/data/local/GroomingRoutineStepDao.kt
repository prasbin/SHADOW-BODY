package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroomingRoutineStepDao {
    @Query("SELECT * FROM grooming_routine_step WHERE routineId = :routineId AND isEnabled = 1 ORDER BY position")
    fun observeEnabledByRoutine(routineId: Long): Flow<List<GroomingRoutineStep>>

    @Query("SELECT * FROM grooming_routine_step WHERE routineId = :routineId ORDER BY position")
    fun observeAllByRoutine(routineId: Long): Flow<List<GroomingRoutineStep>>

    @Query("SELECT * FROM grooming_routine_step WHERE routineId = :routineId AND isEnabled = 1 ORDER BY position")
    suspend fun getEnabledByRoutine(routineId: Long): List<GroomingRoutineStep>

    @Query("SELECT * FROM grooming_routine_step WHERE routineId = :routineId ORDER BY position")
    suspend fun getAllByRoutine(routineId: Long): List<GroomingRoutineStep>

    @Query("SELECT * FROM grooming_routine_step WHERE id = :id")
    suspend fun getById(id: Long): GroomingRoutineStep?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(step: GroomingRoutineStep): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(step: GroomingRoutineStep)

    @Update
    suspend fun update(step: GroomingRoutineStep)

    @Delete
    suspend fun delete(step: GroomingRoutineStep)

    @Query("DELETE FROM grooming_routine_step WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun reorderSteps(routineId: Long, orderedSteps: List<GroomingRoutineStep>) {
        for ((index, step) in orderedSteps.withIndex()) {
            update(step.copy(position = index))
        }
    }
}