package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Phase 4: adaptation state per exercise, plus the single-row checkpoint that
 * makes applying completed sessions idempotent.
 */
@Dao
interface AdaptationDao {

    // --- Adaptation state ---

    @Query("SELECT * FROM exercise_adaptation ORDER BY exerciseId")
    fun observeAll(): Flow<List<ExerciseAdaptation>>

    @Query("SELECT * FROM exercise_adaptation ORDER BY exerciseId")
    suspend fun getAll(): List<ExerciseAdaptation>

    @Query("SELECT * FROM exercise_adaptation WHERE exerciseId = :exerciseId")
    suspend fun getByExercise(exerciseId: Long): ExerciseAdaptation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(adaptation: ExerciseAdaptation): Long

    @Query("DELETE FROM exercise_adaptation WHERE exerciseId = :exerciseId")
    suspend fun deleteByExercise(exerciseId: Long): Int

    @Query("SELECT COUNT(*) FROM exercise_adaptation")
    suspend fun count(): Int

    // --- Checkpoint ---

    @Query("SELECT * FROM adaptation_checkpoint WHERE id = 1")
    suspend fun checkpoint(): AdaptationCheckpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCheckpoint(checkpoint: AdaptationCheckpoint)

    /** Atomic compare-and-set: only a newer checkpoint can be written. */
    @Transaction
    suspend fun advanceCheckpoint(expected: Long, lastAppliedSessionId: Long, now: Long): Boolean {
        val current = checkpoint()
        if ((current?.lastAppliedSessionId ?: 0) != expected) return false
        putCheckpoint(
            AdaptationCheckpoint(
                id = 1,
                lastAppliedSessionId = lastAppliedSessionId,
                updatedAt = now,
            ),
        )
        return true
    }
}
