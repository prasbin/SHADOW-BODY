package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.Achievement
import com.shadowbody.app.data.local.AchievementDao
import com.shadowbody.app.data.local.Attribute
import com.shadowbody.app.data.local.AttributeDao
import com.shadowbody.app.data.local.Streak
import com.shadowbody.app.data.local.StreakDao
import com.shadowbody.app.data.local.XpTransaction
import com.shadowbody.app.data.local.XpTransactionDao
import com.shadowbody.app.domain.progression.AchievementDef
import com.shadowbody.app.domain.progression.ProgressionEngine
import com.shadowbody.app.domain.progression.ProgressionEngine.ProgressionSummary
import com.shadowbody.app.domain.progression.XpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ProgressionRepository(
    private val xpDao: XpTransactionDao,
    private val attributeDao: AttributeDao,
    private val streakDao: StreakDao,
    private val achievementDao: AchievementDao,
) {

    // --- XP ---

    fun observeTotalXp(): Flow<Int> = xpDao.observeTotalXp().map { it ?: 0 }

    suspend fun getTotalXpSync(): Int = xpDao.getTotalXpSync() ?: 0

    fun observeRecentTransactions(limit: Int = 50): Flow<List<XpTransaction>> =
        xpDao.observeRecent(limit)

    suspend fun getRecentTransactionsSync(limit: Int = 50): List<XpTransaction> =
        xpDao.getRecent(limit)

    fun observeTransactionsForDay(dayKey: String): Flow<List<XpTransaction>> =
        xpDao.observeForDay(dayKey)

    // --- Attributes ---

    fun observeAttribute(): Flow<Attribute> =
        attributeDao.observe().map { it ?: Attribute() }

    suspend fun getAttributeSync(): Attribute = attributeDao.getSync() ?: Attribute()

    // --- Streak ---

    fun observeStreak(): Flow<Streak> =
        streakDao.observe().map { it ?: Streak() }

    suspend fun getStreakSync(): Streak = streakDao.getSync() ?: Streak()

    // --- Achievements ---

    fun observeAllAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAll()

    fun observeUnlockedAchievements(): Flow<List<Achievement>> =
        achievementDao.observeUnlocked()

    suspend fun getAllAchievementsSync(): List<Achievement> =
        achievementDao.getAllSync()

    // --- Progression Summary ---

    fun observeSummary(): Flow<ProgressionSummary> = combine(
        observeTotalXp(),
        observeAttribute(),
        observeStreak(),
        observeAllAchievements(),
        observeRecentTransactions(20),
    ) { totalXp, attribute, streak, achievements, recent ->
        ProgressionEngine.computeSummary(totalXp, attribute, streak, achievements, recent)
    }

    // --- XP Award (with duplicate protection) ---

    sealed interface AwardResult {
        data class Awarded(val transaction: XpTransaction) : AwardResult
        data class Duplicate(val existing: XpTransaction) : AwardResult
    }

    /**
     * Attempts to award XP for a completed activity.
     * Uses [sourceRef] for idempotency - same source+ref will not award twice.
     * Returns Awarded with new transaction, or Duplicate with existing transaction.
     */
    suspend fun awardXp(
        source: XpSource,
        sourceRef: String,
        dayKey: String = com.shadowbody.app.domain.nutrition.NutritionDayKey.today(),
        now: Long = System.currentTimeMillis(),
    ): AwardResult {
        // Check for duplicate
        val existing = xpDao.findBySourceRef(source.name, sourceRef)
        if (existing != null) {
            return AwardResult.Duplicate(existing)
        }

        // Create and insert new transaction
        val transaction = ProgressionEngine.createXpTransaction(source, sourceRef, dayKey, now)
        val id = xpDao.insert(transaction)
            ?: return AwardResult.Duplicate(
                xpDao.findBySourceRef(source.name, sourceRef)!!
            )

        val awarded = transaction.copy(id = id!!)
        return AwardResult.Awarded(awarded)
    }

    // --- Progression Processing (called after XP award) ---

    /**
     * Recomputes and persists all derived progression state from XP transactions.
     * Should be called after any XP award to update level, attributes, streaks, achievements.
     */
    suspend fun processProgression(now: Long = System.currentTimeMillis()) {
        val totalXp = getTotalXpSync()
        val recentTxs = getRecentTransactionsSync(1000) // Get all for aggregation

        // Count activities by source for achievements
        val completedWorkouts = recentTxs.count { it.source == XpSource.WORKOUT.name }
        val completedMornings = recentTxs.count { it.source == XpSource.MORNING_ACTIVATION.name }
        val loggedMeals = recentTxs.count { it.source == XpSource.MEAL.name }
        val loggedHydrations = recentTxs.count { it.source == XpSource.HYDRATION.name }

        // Determine active days (days with at least one transaction)
        val activeDays = recentTxs.map { it.dayKey }.distinct().toSet()

        // Update streak - check today
        val today = com.shadowbody.app.domain.nutrition.NutritionDayKey.today()
        val todayHasActivity = today in activeDays
        val currentStreak = getStreakSync()
        val updatedStreak = ProgressionEngine.updateStreak(currentStreak, todayHasActivity, today, now)
        streakDao.update(updatedStreak)

        // Update attributes from all transactions
        var attribute = getAttributeSync()
        val existingAchievements = getAllAchievementsSync().associateBy { it.id }

        recentTxs.forEach { tx ->
            val source = XpSource.valueOf(tx.source)
            val increments = ProgressionEngine.attributeIncrements(source)
            attribute = ProgressionEngine.applyAttributeIncrements(attribute, increments, tx.loggedAt)
        }
        attributeDao.update(attribute.copy(updatedAt = now))

        // Evaluate achievements
        val evaluations = ProgressionEngine.evaluateAchievements(
            totalXp = totalXp,
            completedWorkouts = completedWorkouts,
            completedMornings = completedMornings,
            loggedMeals = loggedMeals,
            loggedHydrations = loggedHydrations,
            currentStreak = updatedStreak.currentStreak,
            existingAchievements = existingAchievements,
        )
        val updatedAchievements = ProgressionEngine.buildAchievements(evaluations, existingAchievements, now)
        updatedAchievements.forEach { achievementDao.upsert(it) }
    }

    /** Initializes default achievements if table is empty */
    suspend fun ensureAchievementsInitialized() {
        val existing = getAllAchievementsSync()
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            AchievementDef.values().forEach { def ->
                achievementDao.upsert(Achievement(
                    id = def.id,
                    name = def.name,
                    description = def.description,
                    unlocked = false,
                    unlockedAt = null,
                    progressCurrent = 0,
                    progressTarget = def.target,
                ))
            }
        }
    }
}