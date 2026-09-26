package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.RecommendationDao
import com.shadowbody.app.data.local.RecommendationDetail
import com.shadowbody.app.data.local.RecommendedExercise
import com.shadowbody.app.data.local.WorkoutRecommendation
import com.shadowbody.app.domain.model.RecommendationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Phase 4: stores generated recommendations as immutable snapshots. Each
 * exercise keeps the reason the engine gave, so the explanation survives even
 * after history changes.
 */
class RecommendationRepository(private val recommendations: RecommendationDao) {

    fun observeLatest(): Flow<RecommendationDetail?> = recommendations.observeLatest()

    fun observeRecent(limit: Int = 10): Flow<List<WorkoutRecommendation>> =
        recommendations.observeRecent(limit)

    suspend fun latestOnce(): RecommendationDetail? = recommendations.latestOnce()

    suspend fun byId(id: Long): RecommendationDetail? = recommendations.byId(id)

    suspend fun count(): Int = recommendations.count()

    suspend fun insert(
        header: WorkoutRecommendation,
        exercises: List<RecommendedExercise>,
    ): Long {
        val id = recommendations.insertRecommendation(header)
        if (exercises.isNotEmpty()) {
            recommendations.insertExercises(
                exercises.map { it.copy(id = 0, recommendationId = id) },
            )
        }
        return id
    }

    suspend fun updateStatus(id: Long, status: RecommendationStatus) {
        val current = recommendations.byId(id) ?: return
        recommendations.update(
            current.recommendation.copy(
                status = status,
                adoptedPlanId = if (status == RecommendationStatus.ADOPTED) {
                    current.recommendation.adoptedPlanId
                } else {
                    null
                },
            ),
        )
    }

    suspend fun markAdopted(id: Long, planId: Long) {
        val current = recommendations.byId(id) ?: return
        recommendations.update(
            current.recommendation.copy(
                status = RecommendationStatus.ADOPTED,
                adoptedPlanId = planId,
            ),
        )
    }
}
