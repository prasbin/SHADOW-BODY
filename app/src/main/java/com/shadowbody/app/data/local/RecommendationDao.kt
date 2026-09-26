package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Recommendation header plus its ordered exercise snapshot. */
data class RecommendationDetail(
    @Embedded val recommendation: WorkoutRecommendation,
    @Relation(parentColumn = "id", entityColumn = "recommendationId")
    val exercises: List<RecommendedExercise>,
)

/** Phase 4: generated recommendations and their frozen exercise targets. */
@Dao
interface RecommendationDao {

    @Insert
    suspend fun insertRecommendation(recommendation: WorkoutRecommendation): Long

    @Insert
    suspend fun insertExercises(exercises: List<RecommendedExercise>)

    @Transaction
    @Query("SELECT * FROM workout_recommendation ORDER BY createdAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<RecommendationDetail?>

    @Transaction
    @Query("SELECT * FROM workout_recommendation ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun latestOnce(): RecommendationDetail?

    @Transaction
    @Query("SELECT * FROM workout_recommendation WHERE id = :id")
    suspend fun byId(id: Long): RecommendationDetail?

    @Query("SELECT * FROM workout_recommendation ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<WorkoutRecommendation>>

    /**
     * Status changes go through [update] rather than a raw UPDATE: the enum is
     * stored via a type converter, which Room cannot bind as a query argument.
     */
    @Update
    suspend fun update(recommendation: WorkoutRecommendation)

    @Query("SELECT COUNT(*) FROM workout_recommendation")
    suspend fun count(): Int
}
