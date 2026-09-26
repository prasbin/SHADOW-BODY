package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Phase 5 routine headers. Reads are ordered deterministically so the list never
 * reshuffles between visits.
 */
@Dao
interface MorningRoutineDao {

    @Query("SELECT * FROM morning_routine ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<MorningRoutine>>

    @Query("SELECT * FROM morning_routine WHERE isActive = 1 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<MorningRoutine>>

    @Query("SELECT * FROM morning_routine ORDER BY sortOrder, id")
    suspend fun getAll(): List<MorningRoutine>

    @Query("SELECT * FROM morning_routine WHERE id = :id")
    suspend fun getById(id: Long): MorningRoutine?

    @Query("SELECT * FROM morning_routine WHERE id = :id")
    fun observeById(id: Long): Flow<MorningRoutine?>

    @Query("SELECT * FROM morning_routine WHERE seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(seedKey: String): MorningRoutine?

    /** Idempotent seeding: the unique seed key makes a repeat insert a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(routine: MorningRoutine): Long

    @Insert
    suspend fun insert(routine: MorningRoutine): Long

    @Update
    suspend fun update(routine: MorningRoutine)

    @Query("DELETE FROM morning_routine WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM morning_routine")
    suspend fun count(): Int
}
