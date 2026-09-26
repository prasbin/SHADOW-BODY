package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Phase 5 routine steps. `(routineId, position)` is unique, so a repeated seed
 * insert is ignored rather than duplicating a step.
 */
@Dao
interface MorningRoutineStepDao {

    @Query("SELECT * FROM morning_routine_step WHERE routineId = :routineId ORDER BY position")
    fun observeByRoutine(routineId: Long): Flow<List<MorningRoutineStep>>

    @Query("SELECT * FROM morning_routine_step WHERE routineId = :routineId ORDER BY position")
    suspend fun getByRoutine(routineId: Long): List<MorningRoutineStep>

    @Query(
        "SELECT * FROM morning_routine_step WHERE routineId = :routineId " +
            "AND isEnabled = 1 ORDER BY position",
    )
    suspend fun getEnabledByRoutine(routineId: Long): List<MorningRoutineStep>

    @Query("SELECT COUNT(*) FROM morning_routine_step WHERE routineId = :routineId")
    suspend fun countByRoutine(routineId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(step: MorningRoutineStep): Long

    @Insert
    suspend fun insert(step: MorningRoutineStep): Long

    @Update
    suspend fun update(step: MorningRoutineStep)

    @Query("DELETE FROM morning_routine_step WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM morning_routine_step WHERE routineId = :routineId")
    suspend fun deleteByRoutine(routineId: Long)
}
