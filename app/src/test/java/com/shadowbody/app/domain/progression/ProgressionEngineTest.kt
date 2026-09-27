package com.shadowbody.app.domain.progression

import com.shadowbody.app.data.local.Achievement
import com.shadowbody.app.data.local.Attribute
import com.shadowbody.app.data.local.Streak
import com.shadowbody.app.data.local.XpTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    @Test
    fun levelCalculation() {
        assertEquals(1, ProgressionEngine.calculateLevel(0))
        assertEquals(1, ProgressionEngine.calculateLevel(99))
        assertEquals(2, ProgressionEngine.calculateLevel(100))
        assertEquals(2, ProgressionEngine.calculateLevel(199))
        assertEquals(3, ProgressionEngine.calculateLevel(200))
        assertEquals(10, ProgressionEngine.calculateLevel(900))
        assertEquals(11, ProgressionEngine.calculateLevel(1000))
    }

    @Test
    fun xpInCurrentLevel() {
        assertEquals(0, ProgressionEngine.xpInCurrentLevel(0))
        assertEquals(50, ProgressionEngine.xpInCurrentLevel(50))
        assertEquals(99, ProgressionEngine.xpInCurrentLevel(99))
        assertEquals(0, ProgressionEngine.xpInCurrentLevel(100))
        assertEquals(25, ProgressionEngine.xpInCurrentLevel(125))
    }

    @Test
    fun xpToNextLevel() {
        assertEquals(100, ProgressionEngine.xpToNextLevel(0))
        assertEquals(50, ProgressionEngine.xpToNextLevel(50))
        assertEquals(1, ProgressionEngine.xpToNextLevel(99))
        assertEquals(100, ProgressionEngine.xpToNextLevel(100))
        assertEquals(75, ProgressionEngine.xpToNextLevel(125))
    }

    @Test
    fun levelProgress() {
        assertEquals(0f, ProgressionEngine.levelProgress(0), 0.001f)
        assertEquals(0.5f, ProgressionEngine.levelProgress(50), 0.001f)
        assertEquals(0.99f, ProgressionEngine.levelProgress(99), 0.001f)
        assertEquals(0f, ProgressionEngine.levelProgress(100), 0.001f)
        assertEquals(0.25f, ProgressionEngine.levelProgress(125), 0.001f)
    }

    @Test
    fun attributeIncrementsWorkout() {
        val inc = ProgressionEngine.attributeIncrements(XpSource.WORKOUT)
        assertEquals(2, inc[AttributeType.STRENGTH])
        assertEquals(2, inc[AttributeType.ENDURANCE])
        assertEquals(0, inc[AttributeType.DISCIPLINE] ?: 0)
        assertEquals(0, inc[AttributeType.RECOVERY] ?: 0)
        assertEquals(0, inc[AttributeType.NUTRITION] ?: 0)
    }

    @Test
    fun attributeIncrementsMorning() {
        val inc = ProgressionEngine.attributeIncrements(XpSource.MORNING_ACTIVATION)
        assertEquals(2, inc[AttributeType.DISCIPLINE])
        assertEquals(1, inc[AttributeType.RECOVERY])
    }

    @Test
    fun attributeIncrementsMeal() {
        val inc = ProgressionEngine.attributeIncrements(XpSource.MEAL)
        assertEquals(2, inc[AttributeType.NUTRITION])
    }

    @Test
    fun attributeIncrementsHydration() {
        val inc = ProgressionEngine.attributeIncrements(XpSource.HYDRATION)
        assertEquals(1, inc[AttributeType.RECOVERY])
        assertEquals(1, inc[AttributeType.NUTRITION])
    }

    @Test
    fun applyAttributeIncrements() {
        val attr = Attribute(strength = 10, endurance = 5, discipline = 3, recovery = 2, nutrition = 1)
        val inc = mapOf(AttributeType.STRENGTH to 2, AttributeType.ENDURANCE to 3)
        val updated = ProgressionEngine.applyAttributeIncrements(attr, inc, 1000L)
        assertEquals(12, updated.strength)
        assertEquals(8, updated.endurance)
        assertEquals(3, updated.discipline)
        assertEquals(2, updated.recovery)
        assertEquals(1, updated.nutrition)
        assertEquals(1000L, updated.updatedAt)
    }

    @Test
    fun streakFirstDay() {
        val now = System.currentTimeMillis()
        val today = com.shadowbody.app.domain.nutrition.NutritionDayKey.today()
        val streak = ProgressionEngine.updateStreak(null, true, today, now)
        assertEquals(1, streak.currentStreak)
        assertEquals(1, streak.longestStreak)
        assertEquals(today, streak.lastActiveDayKey)
    }

    @Test
    fun streakConsecutiveDay() {
        val now = System.currentTimeMillis()
        val today = com.shadowbody.app.domain.nutrition.NutritionDayKey.today()
        val yesterday = java.time.LocalDate.parse(today).minusDays(1).toString()
        val existing = Streak(currentStreak = 3, longestStreak = 5, lastActiveDayKey = yesterday)
        val streak = ProgressionEngine.updateStreak(existing, true, today, now)
        assertEquals(4, streak.currentStreak)
        assertEquals(5, streak.longestStreak)
        assertEquals(today, streak.lastActiveDayKey)
    }

    @Test
    fun streakGapResets() {
        val now = System.currentTimeMillis()
        val today = com.shadowbody.app.domain.nutrition.NutritionDayKey.today()
        val threeDaysAgo = java.time.LocalDate.parse(today).minusDays(3).toString()
        val existing = Streak(currentStreak = 5, longestStreak = 10, lastActiveDayKey = threeDaysAgo)
        val streak = ProgressionEngine.updateStreak(existing, true, today, now)
        assertEquals(1, streak.currentStreak)
        assertEquals(10, streak.longestStreak) // longest preserved
        assertEquals(today, streak.lastActiveDayKey)
    }

    @Test
    fun streakSameDayNoChange() {
        val now = System.currentTimeMillis()
        val today = com.shadowbody.app.domain.nutrition.NutritionDayKey.today()
        val existing = Streak(currentStreak = 3, longestStreak = 5, lastActiveDayKey = today)
        val streak = ProgressionEngine.updateStreak(existing, true, today, now)
        assertEquals(3, streak.currentStreak) // unchanged
        assertEquals(5, streak.longestStreak)
    }

    @Test
    fun streakNoActivityNoChange() {
        val now = System.currentTimeMillis()
        val today = com.shadowbody.app.domain.nutrition.NutritionDayKey.today()
        val existing = Streak(currentStreak = 3, longestStreak = 5, lastActiveDayKey = today)
        val streak = ProgressionEngine.updateStreak(existing, false, today, now)
        assertEquals(3, streak.currentStreak) // unchanged when no activity
    }

    @Test
    fun createXpTransaction() {
        val now = System.currentTimeMillis()
        val today = com.shadowbody.app.domain.nutrition.NutritionDayKey.today()
        val tx = ProgressionEngine.createXpTransaction(
            XpSource.WORKOUT,
            "workout:123",
            today,
            now,
        )
        assertEquals(50, tx.xpAmount)
        assertEquals("WORKOUT", tx.source)
        assertEquals("workout:123", tx.sourceRef)
        assertEquals(today, tx.dayKey)
        assertEquals("Workout", tx.reason)
        assertEquals(now, tx.loggedAt)
    }

    @Test
    fun evaluateAchievements() {
        val existing = emptyMap<String, Achievement>()
        val evals = ProgressionEngine.evaluateAchievements(
            totalXp = 150,
            completedWorkouts = 1,
            completedMornings = 1,
            loggedMeals = 1,
            loggedHydrations = 1,
            currentStreak = 3,
            existingAchievements = existing,
        )
        assertEquals(1, evals.first { it.first.id == "first_step" }.second)
        assertEquals(1, evals.first { it.first.id == "workout_initiate" }.second)
        assertEquals(1, evals.first { it.first.id == "morning_awakened" }.second)
        assertEquals(1, evals.first { it.first.id == "nutrition_logged" }.second)
        assertEquals(1, evals.first { it.first.id == "hydration_habit" }.second)
        assertEquals(3, evals.first { it.first.id == "week_warrior" }.second)
        assertEquals(100, evals.first { it.first.id == "xp_100" }.second) // capped at target
    }

    @Test
    fun buildAchievementsUnlocks() {
        val now = System.currentTimeMillis()
        val existing = emptyMap<String, Achievement>()
        val evals = listOf(
            AchievementDef.FIRST_STEP to 1,
            AchievementDef.WEEK_WARRIOR to 3,
        )
        val built = ProgressionEngine.buildAchievements(evals, existing, now)
        assertTrue(built.first { it.id == "first_step" }.unlocked)
        assertEquals(now, built.first { it.id == "first_step" }.unlockedAt!!)
        assertFalse(built.first { it.id == "week_warrior" }.unlocked)
        assertEquals(3, built.first { it.id == "week_warrior" }.progressCurrent)
    }

    @Test
    fun buildAchievementsAlreadyUnlockedPreserved() {
        val now = System.currentTimeMillis()
        val oldUnlock = now - 100000
        val existing = mapOf(
            "first_step" to Achievement(
                id = "first_step",
                name = "First Step",
                description = "desc",
                unlocked = true,
                unlockedAt = oldUnlock,
                progressCurrent = 1,
                progressTarget = 1,
            ),
        )
        val evals = listOf(AchievementDef.FIRST_STEP to 1)
        val built = ProgressionEngine.buildAchievements(evals, existing, now)
        assertTrue(built.first { it.id == "first_step" }.unlocked)
        assertEquals(oldUnlock, built.first { it.id == "first_step" }.unlockedAt!!) // preserved
    }

    @Test
    fun defaultSummary() {
        val summary = ProgressionEngine.defaultSummary()
        assertEquals(0, summary.totalXp)
        assertEquals(1, summary.level)
        assertEquals(0, summary.xpInCurrentLevel)
        assertEquals(100, summary.xpToNextLevel)
        assertEquals(0f, summary.levelProgress, 0.001f)
    }
}