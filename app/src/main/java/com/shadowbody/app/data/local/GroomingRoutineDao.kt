package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroomingRoutineDao {
    @Query("SELECT * FROM grooming_routine WHERE isActive = 1 ORDER BY sortOrder, id")
    fun observeActiveRoutines(): Flow<List<GroomingRoutine>>

    @Query("SELECT * FROM grooming_routine ORDER BY sortOrder, id")
    fun observeAllRoutines(): Flow<List<GroomingRoutine>>

    @Query("SELECT * FROM grooming_routine WHERE id = :id")
    suspend fun getById(id: Long): GroomingRoutine?

    @Query("SELECT * FROM grooming_routine WHERE seedKey = :seedKey")
    suspend fun getBySeedKey(seedKey: String): GroomingRoutine?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(routine: GroomingRoutine): Long

    @Update
    suspend fun update(routine: GroomingRoutine)

    @Delete
    suspend fun delete(routine: GroomingRoutine)

    @Query("DELETE FROM grooming_routine WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT MAX(sortOrder) FROM grooming_routine")
    suspend fun maxSortOrder(): Int?

    @Query("SELECT * FROM grooming_routine WHERE seedKey IS NOT NULL")
    suspend fun getSeededRoutines(): List<GroomingRoutine>
}