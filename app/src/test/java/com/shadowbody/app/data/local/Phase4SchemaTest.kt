package com.shadowbody.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.domain.model.Difficulty
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.MuscleGroup
import com.shadowbody.app.domain.model.ProgressionState
import com.shadowbody.app.domain.model.RecommendationStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 4 relational contract, verified against a real Room database with
 * foreign keys enforced.
 *
 * These are the rules the rest of the phase relies on: a recommendation owns
 * its exercise snapshot, an adaptation row may not outlive its exercise, and
 * deleting a plan must never take recorded history with it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase4SchemaTest {

    private var db: ShadowBodyDatabase? = null

    @Before
    fun setUp() {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db?.close()
        db = null
    }

    private fun database(): ShadowBodyDatabase = checkNotNull(db)

    private suspend fun seedExercise(): Long = database().exerciseDao().insertStrict(
        Exercise(
            name = "Phase4 Move",
            muscleGroup = MuscleGroup.CHEST,
            category = ExerciseCategory.BODYWEIGHT,
            equipment = Equipment.BODYWEIGHT,
            description = "d",
            instructions = "i",
            difficulty = Difficulty.BEGINNER,
        ),
    )

    private fun adaptationRow(exerciseId: Long) = ExerciseAdaptation(
        exerciseId = exerciseId,
        currentSets = 3,
        currentReps = 10,
        restSec = 60,
        state = ProgressionState.MAINTAINING,
        lastReasonCode = "STABLE",
        lastReasonText = "held",
        lastAdjustmentAt = 1L,
        updatedAt = 1L,
    )

    private fun recommendationRow(id: Long = 0) = WorkoutRecommendation(
        id = id,
        createdAt = 1L,
        name = "ADAPTIVE SESSION",
        estimatedMinutes = 30,
        summary = "held",
        status = RecommendationStatus.ACTIVE,
    )

    @Test
    fun `one adaptation row per exercise`() = runTest {
        val exerciseId = seedExercise()
        val dao = database().adaptationDao()
        dao.upsert(adaptationRow(exerciseId))
        // exerciseId is the primary key, so tracking is upserted, never forked.
        dao.upsert(adaptationRow(exerciseId).copy(currentSets = 4))
        assertEquals(1, dao.count())
        assertEquals(4, dao.getByExercise(exerciseId)?.currentSets)

        // A hand-written second row for the same exercise is still refused.
        try {
            database().openHelper.writableDatabase.execSQL(
                "INSERT INTO exercise_adaptation (exerciseId, currentSets, currentReps, " +
                    "restSec, state, lastReasonCode, lastReasonText, lastAdjustmentAt, " +
                    "updatedAt) VALUES ($exerciseId, 5, 10, 60, 'MAINTAINING', 'X', 'x', 1, 1)",
            )
            throw AssertionError("a second row for the same exercise must be rejected")
        } catch (e: SQLiteConstraintException) {
            // Expected: unique index on exerciseId.
        }
    }

    @Test
    fun `an adaptation row may not outlive its exercise`() = runTest {
        val exerciseId = seedExercise()
        database().adaptationDao().upsert(adaptationRow(exerciseId))
        try {
            database().exerciseDao().deleteCustom(exerciseId)
            throw AssertionError("deleting a tracked exercise must be refused")
        } catch (e: SQLiteConstraintException) {
            // Expected: RESTRICT on exercise_adaptation.exerciseId.
        }
        assertEquals(1, database().adaptationDao().count())
    }

    @Test
    fun `deleting a recommendation removes its exercise snapshot`() = runTest {
        val exerciseId = seedExercise()
        val recommendations = database().recommendationDao()
        val recommendationId = recommendations.insertRecommendation(recommendationRow())
        recommendations.insertExercises(
            listOf(
                RecommendedExercise(
                    recommendationId = recommendationId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 3,
                    reps = 10,
                    restSec = 60,
                    reasonCode = "NO_DATA",
                    reasonText = "no data",
                ),
            ),
        )

        database().openHelper.writableDatabase.execSQL(
            "DELETE FROM workout_recommendation WHERE id = $recommendationId",
        )

        assertNull(database().recommendationDao().byId(recommendationId))
        val orphans = database().openHelper.writableDatabase
            .query("SELECT COUNT(*) FROM recommended_exercise")
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(0, orphans)
    }

    @Test
    fun `recommendation position is unique inside one recommendation`() = runTest {
        val exerciseId = seedExercise()
        val recommendations = database().recommendationDao()
        val recommendationId = recommendations.insertRecommendation(recommendationRow())
        val row = RecommendedExercise(
            recommendationId = recommendationId,
            exerciseId = exerciseId,
            position = 0,
            sets = 3,
            reps = 10,
            restSec = 60,
            reasonCode = "NO_DATA",
            reasonText = "no data",
        )
        recommendations.insertExercises(listOf(row))
        try {
            recommendations.insertExercises(listOf(row.copy(id = 0)))
            throw AssertionError("a duplicate position must be rejected")
        } catch (e: SQLiteConstraintException) {
            // Expected: unique(recommendationId, position).
        }
        assertEquals(1, database().recommendationDao().byId(recommendationId)?.exercises?.size)
    }

    @Test
    fun `deleting a plan nulls the miss and keeps the record`() = runTest {
        seedExercise()
        val planId = database().planDao().insert(WorkoutPlan(name = "Doomed"))
        database().missedWorkoutDao().insert(MissedWorkout(planId = planId, recordedAt = 5L))

        database().planDao().delete(planId)

        // SET_NULL: a skip is history too and is never cascaded away.
        val missed = database().missedWorkoutDao().latest()
        assertEquals(1, database().missedWorkoutDao().count())
        assertNull(missed?.planId)
    }

    @Test
    fun `the checkpoint is a single replaceable row`() = runTest {
        val dao = database().adaptationDao()
        assertEquals(0L, dao.checkpoint()?.lastAppliedSessionId ?: 0L)
        assertTrue(dao.advanceCheckpoint(expected = 0L, lastAppliedSessionId = 5L, now = 1L))
        assertEquals(5L, dao.checkpoint()?.lastAppliedSessionId)
        // A stale writer must not be able to move it backwards.
        assertEquals(
            false,
            dao.advanceCheckpoint(expected = 0L, lastAppliedSessionId = 1L, now = 2L),
        )
        assertEquals(5L, dao.checkpoint()?.lastAppliedSessionId)
    }

    @Test
    fun `readiness reports are append only and ordered newest first`() = runTest {
        val dao = database().readinessDao()
        dao.insert(ReadinessReport(recordedAt = 100L, fatigue = 1, soreness = 1))
        dao.insert(ReadinessReport(recordedAt = 200L, fatigue = 3, soreness = 2))

        assertEquals(2, dao.count())
        assertEquals(3, dao.latest()?.fatigue)
        assertEquals(3, dao.latestAtOrBefore(200L)?.fatigue)
        assertEquals(1, dao.latestAtOrBefore(150L)?.fatigue)
        assertNull(dao.latestAtOrBefore(50L))
    }
}
