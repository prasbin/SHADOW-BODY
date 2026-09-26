package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE isActive = 1 ORDER BY name")
    fun observeActive(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE isActive = 1 ORDER BY name")
    suspend fun getActive(): List<Exercise>

    @Query(
        "SELECT * FROM exercise WHERE isActive = 1 AND " +
            "(name LIKE '%' || :query || '%' OR muscleGroup = :muscle) ORDER BY name",
    )
    fun search(query: String, muscle: String?): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT COUNT(*) FROM exercise WHERE isSeeded = 1")
    suspend fun seededCount(): Int

    /** Duplicate-safe: name uniqueness resolves conflicts to IGNORE. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(exercises: List<Exercise>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Query("DELETE FROM exercise WHERE id = :id AND isSeeded = 0")
    suspend fun deleteCustom(id: Long): Int
}
