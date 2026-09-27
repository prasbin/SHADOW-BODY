package com.shadowbody.app.domain.progression

import com.shadowbody.app.data.local.Attribute
import com.shadowbody.app.data.local.Achievement
import com.shadowbody.app.data.local.Streak
import com.shadowbody.app.data.local.XpTransaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Phase 7 deterministic progression engine.
 * All progression logic lives here — no UI, no Android framework.
 * Given the same inputs, always produces the same outputs.
 */
object ProgressionEngine {

    private val DAY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    // --- Level calculation ---

    /** MVP level formula: level = floor(totalXp / 100) + 1 */
    fun calculateLevel(totalXp: Int): Int = totalXp / 100 + 1

    fun xpForLevel(level: Int): Int = (level - 1) * 100

    fun xpInCurrentLevel(totalXp: Int): Int = totalXp % 100

    fun xpToNextLevel(totalXp: Int): Int = 100 - (totalXp % 100)

    fun levelProgress(totalXp: Int): Float = (totalXp % 100).toFloat() / 100f

    // --- Attribute increments per XP source ---

    /** Returns the attribute increments for a given XP source. */
    fun attributeIncrements(source: XpSource): Map<AttributeType, Int> = when (source) {
        XpSource.WORKOUT -> mapOf(
            AttributeType.STRENGTH to 2,
            AttributeType.ENDURANCE to 2,
        )
        XpSource.MORNING_ACTIVATION -> mapOf(
            AttributeType.DISCIPLINE to 2,
            AttributeType.RECOVERY to 1,
        )
        XpSource.MEAL -> mapOf(
            AttributeType.NUTRITION to 2,
        )
        XpSource.HYDRATION -> mapOf(
            AttributeType.RECOVERY to 1,
            AttributeType.NUTRITION to 1,
        )
    }

    /** Applies attribute increments to an [Attribute] entity. */
    fun applyAttributeIncrements(
        current: Attribute,
        increments: Map<AttributeType, Int>,
        now: Long,
    ): Attribute {
        var s = current.strength
        var e = current.endurance
        var d = current.discipline
        var r = current.recovery
        var n = current.nutrition

        increments.forEach { (type, value) ->
            when (type) {
                AttributeType.STRENGTH -> s += value
                AttributeType.ENDURANCE -> e += value
                AttributeType.DISCIPLINE -> d += value
                AttributeType.RECOVERY -> r += value
                AttributeType.NUTRITION -> n += value
            }
        }

        return Attribute(
            id = 1L,
            strength = s,
            endurance = e,
            discipline = d,
            recovery = r,
            nutrition = n,
            updatedAt = now,
        )
    }

    // --- Streak calculation ---

    /**
     * Updates streak based on whether today has qualifying activity.
     * A day counts if it has at least one qualifying progression activity.
     * Multiple activities on the same day do not increase the streak.
     */
    fun updateStreak(
        current: Streak?,
        todayHasActivity: Boolean,
        dayKey: String,
        now: Long,
    ): Streak {
        val currentStreak = current?.currentStreak ?: 0
        val longestStreak = current?.longestStreak ?: 0
        val lastActive = current?.lastActiveDayKey

        return if (todayHasActivity) {
            val newCurrent = if (lastActive == dayKey) {
                // Same day, streak unchanged
                currentStreak
            } else if (lastActive == yesterday(dayKey)) {
                // Consecutive day
                currentStreak + 1
            } else {
                // Gap or first day
                1
            }
            Streak(
                id = 1L,
                currentStreak = newCurrent,
                longestStreak = maxOf(longestStreak, newCurrent),
                lastActiveDayKey = dayKey,
                updatedAt = now,
            )
        } else {
            // No activity today - streak doesn't change (we only update on activity)
            current ?: Streak(id = 1L, updatedAt = now)
        }
    }

    private fun yesterday(dayKey: String): String =
        LocalDate.parse(dayKey, DAY_FORMATTER).minusDays(1).format(DAY_FORMATTER)

    // --- Achievement evaluation ---

    /**
     * Evaluates which achievements should be unlocked based on current state.
     * Returns a list of (achievementDef, currentProgress) for all definitions.
     */
    fun evaluateAchievements(
        totalXp: Int,
        completedWorkouts: Int,
        completedMornings: Int,
        loggedMeals: Int,
        loggedHydrations: Int,
        currentStreak: Int,
        existingAchievements: Map<String, Achievement>,
    ): List<Pair<AchievementDef, Int>> {
        return AchievementDef.values().map { def ->
            val progress = when (def) {
                AchievementDef.FIRST_STEP -> if (totalXp > 0) 1 else 0
                AchievementDef.WORKOUT_INITIATE -> completedWorkouts
                AchievementDef.MORNING_AWAKENED -> completedMornings
                AchievementDef.NUTRITION_LOGGED -> loggedMeals
                AchievementDef.HYDRATION_HABIT -> loggedHydrations
                AchievementDef.WEEK_WARRIOR -> currentStreak
                AchievementDef.XP_100 -> totalXp
            }
            def to progress.coerceAtMost(def.target)
        }
    }

    /**
     * Builds or updates [Achievement] entities from evaluation results.
     */
    fun buildAchievements(
        evaluations: List<Pair<AchievementDef, Int>>,
        existing: Map<String, Achievement>,
        now: Long,
    ): List<Achievement> {
        return evaluations.map { (def, progress) ->
            val existingAch = existing[def.id]
            val wasUnlocked = existingAch?.unlocked == true
            val nowUnlocked = !wasUnlocked && progress >= def.target
            Achievement(
                id = def.id,
                name = def.name,
                description = def.description,
                unlocked = wasUnlocked || nowUnlocked,
                unlockedAt = if (nowUnlocked) now else existingAch?.unlockedAt,
                progressCurrent = progress,
                progressTarget = def.target,
            )
        }
    }

    // --- XP Transaction creation ---

    /**
     * Creates an XP transaction for the given source and reference.
     * The sourceRef must uniquely identify the activity (e.g., sessionId for workout).
     */
    fun createXpTransaction(
        source: XpSource,
        sourceRef: String,
        dayKey: String,
        now: Long,
    ): XpTransaction {
        return XpTransaction(
            id = 0L,
            xpAmount = source.xpReward,
            source = source.name,
            sourceRef = sourceRef,
            dayKey = dayKey,
            reason = source.displayName,
            loggedAt = now,
        )
    }

    // --- Progression Summary ---

    data class ProgressionSummary(
        val totalXp: Int,
        val level: Int,
        val xpInCurrentLevel: Int,
        val xpToNextLevel: Int,
        val levelProgress: Float,
        val attribute: Attribute,
        val streak: Streak,
        val achievements: List<Achievement>,
        val recentTransactions: List<XpTransaction>,
    )

    /**
     * Computes a full progression summary from raw data.
     */
    fun computeSummary(
        totalXp: Int,
        attribute: Attribute,
        streak: Streak,
        achievements: List<Achievement>,
        recentTransactions: List<XpTransaction>,
    ): ProgressionSummary {
        return ProgressionSummary(
            totalXp = totalXp,
            level = calculateLevel(totalXp),
            xpInCurrentLevel = xpInCurrentLevel(totalXp),
            xpToNextLevel = xpToNextLevel(totalXp),
            levelProgress = levelProgress(totalXp),
            attribute = attribute,
            streak = streak,
            achievements = achievements,
            recentTransactions = recentTransactions,
        )
    }

    fun defaultSummary(): ProgressionSummary = ProgressionSummary(
        totalXp = 0,
        level = 1,
        xpInCurrentLevel = 0,
        xpToNextLevel = 100,
        levelProgress = 0f,
        attribute = Attribute(),
        streak = Streak(),
        achievements = emptyList(),
        recentTransactions = emptyList(),
    )
}