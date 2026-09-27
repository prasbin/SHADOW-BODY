package com.shadowbody.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shadowbody.app.data.local.Migrations
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.domain.progression.XpSource
import com.shadowbody.app.domain.nutrition.NutritionDayKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressionRepositoryTest {

    private var db: ShadowBodyDatabase? = null
    private var repo: ProgressionRepository? = null

    @Before
    fun setup() {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        repo = ProgressionRepository(
            db!!.xpTransactionDao(),
            db!!.attributeDao(),
            db!!.streakDao(),
            db!!.achievementDao(),
        )
    }

    @After
    fun tearDown() {
        db?.close()
        db = null
    }

    @Test
    fun awardXpCreatesTransaction() = runTest {
        val result = repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today())
        assertTrue(result is ProgressionRepository.AwardResult.Awarded)
        val awarded = result as ProgressionRepository.AwardResult.Awarded
        assertEquals(50, awarded.transaction.xpAmount)
        assertEquals("WORKOUT", awarded.transaction.source)
        assertEquals("workout:1", awarded.transaction.sourceRef)
    }

    @Test
    fun awardXpDuplicateProtection() = runTest {
        repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today())
        val result = repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today())
        assertTrue(result is ProgressionRepository.AwardResult.Duplicate)
        val duplicate = result as ProgressionRepository.AwardResult.Duplicate
        assertEquals(50, duplicate.existing.xpAmount)
    }

    @Test
    fun totalXpAggregates() = runTest {
        assertEquals(0, repo!!.getTotalXpSync())
        repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today())
        repo!!.awardXp(XpSource.MORNING_ACTIVATION, "morning:1", NutritionDayKey.today())
        repo!!.awardXp(XpSource.MEAL, "food:1", NutritionDayKey.today())
        assertEquals(50 + 30 + 10, repo!!.getTotalXpSync())
    }

    @Test
    fun processProgressionUpdatesAttributes() = runTest {
        repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today())
        repo!!.processProgression()
        val attr = repo!!.getAttributeSync()
        assertEquals(2, attr.strength)
        assertEquals(2, attr.endurance)
    }

    @Test
    fun processProgressionUpdatesStreak() = runTest {
        val today = NutritionDayKey.today()
        repo!!.awardXp(XpSource.WORKOUT, "workout:1", today)
        repo!!.processProgression()
        val streak = repo!!.getStreakSync()
        assertEquals(1, streak.currentStreak)
        assertEquals(1, streak.longestStreak)
        assertEquals(today, streak.lastActiveDayKey)
    }

    @Test
    fun processProgressionUnlocksAchievements() = runTest {
        repo!!.ensureAchievementsInitialized()
        // Award enough to unlock multiple achievements
        repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today()) // 50 XP
        repo!!.awardXp(XpSource.MORNING_ACTIVATION, "morning:1", NutritionDayKey.today()) // 30 XP
        repo!!.awardXp(XpSource.MEAL, "food:1", NutritionDayKey.today()) // 10 XP
        repo!!.awardXp(XpSource.HYDRATION, "hydration:1", NutritionDayKey.today()) // 5 XP
        // Total = 95 XP, not enough for XP_100
        repo!!.processProgression()

        val achievements = repo!!.getAllAchievementsSync()
        val firstStep = achievements.find { it.id == "first_step" }!!
        assertTrue(firstStep.unlocked)
        assertEquals(1, firstStep.progressCurrent)

        val workoutInitiate = achievements.find { it.id == "workout_initiate" }!!
        assertTrue(workoutInitiate.unlocked)

        val xp100 = achievements.find { it.id == "xp_100" }!!
        assertFalse(xp100.unlocked)
        assertEquals(95, xp100.progressCurrent)
    }

    @Test
    fun xp100AchievementUnlocks() = runTest {
        repo!!.ensureAchievementsInitialized()
        // Award exactly 100 XP
        repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today()) // 50
        repo!!.awardXp(XpSource.WORKOUT, "workout:2", NutritionDayKey.today()) // 50
        repo!!.processProgression()

        val achievements = repo!!.getAllAchievementsSync()
        val xp100 = achievements.find { it.id == "xp_100" }!!
        assertTrue(xp100.unlocked)
    }

    @Test
    fun observeSummary() = runTest {
        repo!!.ensureAchievementsInitialized()
        repo!!.awardXp(XpSource.WORKOUT, "workout:1", NutritionDayKey.today())
        repo!!.processProgression()

        // Use sync method for testing
        val totalXp = repo!!.getTotalXpSync()
        val level = com.shadowbody.app.domain.progression.ProgressionEngine.calculateLevel(totalXp)
        assertEquals(50, totalXp)
        assertEquals(1, level)
    }
}