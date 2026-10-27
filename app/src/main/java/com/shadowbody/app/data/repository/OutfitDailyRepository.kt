package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.OutfitDailyDecision
import com.shadowbody.app.data.local.OutfitDailyDecisionDao
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.data.local.WardrobePhotoCombinationDao
import com.shadowbody.app.domain.outfit.OutfitDayKey
import com.shadowbody.app.domain.outfit.OutfitRecommenderEngine
import kotlinx.coroutines.flow.Flow

/**
 * Feature C repository. Reuses the existing photographed wardrobe options
 * (`wardrobe_photo_combination`) — no duplicate photo model. Persists at most
 * one decision per day (unique `dayKey`). The persisted explanation is always
 * the honest, rule-based text produced by [OutfitRecommenderEngine]; nothing
 * here claims the photos were visually analysed.
 */
class OutfitDailyRepository(
    private val decisionDao: OutfitDailyDecisionDao,
    private val combinationDao: WardrobePhotoCombinationDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    /** All photographed options, in stable creation order. */
    suspend fun photographedOptions(): List<WardrobePhotoCombination> =
        combinationDao.snapshotOrderedByPosition().filter { it.photoPath != null }

    fun observeDecisions(): Flow<List<OutfitDailyDecision>> = decisionDao.observeAll()

    suspend fun todayDecision(): OutfitDailyDecision? =
        decisionDao.getByDay(OutfitDayKey.today())

    /**
     * Runs the honest rule-based engine and persists the result for today.
     * Returns null when there are no photographed options (empty state).
     */
    suspend fun recommendForToday(occasion: String): OutfitDailyDecision? {
        val options = photographedOptions()
        val result = OutfitRecommenderEngine.recommend(options, occasion) ?: return null
        val record = OutfitDailyDecision(
            dayKey = OutfitDayKey.today(),
            selectedCombinationId = result.selected.id,
            candidateIds = result.consideredIds.joinToString(","),
            occasion = occasion,
            explanation = result.explanation,
            decidedAt = nowMillis(),
            updatedAt = nowMillis(),
        )
        decisionDao.upsert(record)
        return decisionDao.getByDay(OutfitDayKey.today()) ?: record
    }

    /** Resolves a decision back to the combination it selected, if it still exists. */
    suspend fun resolveSelected(decision: OutfitDailyDecision): WardrobePhotoCombination? =
        combinationDao.getById(decision.selectedCombinationId)

    suspend fun deleteDecision(id: Long) = decisionDao.deleteById(id)

    suspend fun decisionsCount(): Int = decisionDao.count()
}
