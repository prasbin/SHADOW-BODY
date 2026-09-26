package com.shadowbody.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.data.local.Exercise
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.local.WorkoutPlanExercise
import com.shadowbody.app.domain.model.Difficulty
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.MuscleGroup
import com.shadowbody.app.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutRepositoryTest {

    private var db: ShadowBodyDatabase? = null

    @After
    fun tearDown() {
        db?.close()
        db = null
    }

    private fun open() {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
    }

    private fun repos(): Triple<ExerciseRepository, PlanRepository, SessionRepository> {
        val database = checkNotNull(db)
        val exercises = ExerciseRepository(database.exerciseDao())
        val plans = PlanRepository(database.planDao(), database.planExerciseDao(), database.exerciseDao())
        return Triple(exercises, plans, SessionRepository(database.sessionDao(), plans))
    }

    @Test
    fun `seeding is duplicate-safe`() = runTest {
        open()
        val (exercises, _, _) = repos()
        assertEquals(28, exercises.ensureSeeded())
        assertEquals(28, exercises.ensureSeeded())
        assertEquals(28, exercises.library().first().size)
    }

    @Test
    fun `plan save normalizes order and detail resolves`() = runTest {
        open()
        val (exercises, plans, _) = repos()
        exercises.ensureSeeded()
        val library = exercises.library().first()
        val planId = plans.savePlan(
            WorkoutPlan(name = "Alpha"),
            listOf(
                WorkoutPlanExercise(planId = 0, exerciseId = library[1].id, position = 9, targetSets = 3, targetReps = 10),
                WorkoutPlanExercise(planId = 0, exerciseId = library[0].id, position = 3, targetSets = 2, targetDurationSec = 30),
            ),
        )
        val detail = plans.getDetailOnce(planId)!!
        assertEquals(listOf(0, 1), detail.slots.map { it.slot.position })
        assertEquals(library[1].id, detail.slots[0].exercise.id)
    }

    @Test
    fun `duplicate slot aborts at the constraint`() = runTest {
        open()
        val (exercises, plans, _) = repos()
        exercises.ensureSeeded()
        val first = exercises.library().first().first()
        val planId = plans.savePlan(
            WorkoutPlan(name = "Dup"),
            listOf(WorkoutPlanExercise(planId = 0, exerciseId = first.id, position = 0, targetSets = 3, targetReps = 8)),
        )
        try {
            checkNotNull(db).planExerciseDao().insert(
                WorkoutPlanExercise(planId = planId, exerciseId = first.id, position = 1, targetSets = 3, targetReps = 8),
            )
            fail("duplicate slot must abort")
        } catch (e: SQLiteConstraintException) {
            // Expected: unique(planId, exerciseId).
        }
    }

    @Test
    fun `session lifecycle persists from start to finish`() = runTest {
        open()
        val (exercises, plans, sessions) = repos()
        exercises.ensureSeeded()
        val library = exercises.library().first()
        val planId = plans.savePlan(
            WorkoutPlan(name = "Beta"),
            listOf(WorkoutPlanExercise(planId = 0, exerciseId = library[0].id, position = 0, targetSets = 2, targetReps = 10)),
        )
        val sessionId = sessions.start(planId)
        var detail = sessions.detail(sessionId)!!
        assertEquals(2, detail.totalSets)
        assertEquals(0, detail.completedSets)

        val firstSet = detail.exercises[0].sets[0]
        sessions.recordSet(firstSet, actualReps = 10, actualDurationSec = null, weightKg = null, completed = true)
        sessions.setExerciseCompleted(detail.exercises[0].sessionExercise.id, true)
        detail = sessions.detail(sessionId)!!
        assertEquals(1, detail.completedSets)
        assertEquals(1, detail.completedExercises)

        sessions.finish(sessionId)
        detail = sessions.detail(sessionId)!!
        assertEquals(SessionStatus.COMPLETED, detail.session.status)
        assertTrue(detail.session.endedAt != null)
        assertEquals(1, sessions.recent().first().size)
    }

    @Test
    fun `plan delete cascades slots but keeps session history`() = runTest {
        open()
        val (exercises, plans, sessions) = repos()
        exercises.ensureSeeded()
        val first = exercises.library().first().first()
        val planId = plans.savePlan(
            WorkoutPlan(name = "Gamma"),
            listOf(WorkoutPlanExercise(planId = 0, exerciseId = first.id, position = 0, targetSets = 1, targetReps = 5)),
        )
        val sessionId = sessions.start(planId)
        plans.deletePlan(planId)
        assertNull(plans.getDetailOnce(planId))
        assertEquals(0, checkNotNull(db).planExerciseDao().getByPlan(planId).size)
        val detail = sessions.detail(sessionId)!!
        assertNull(detail.session.planId) // SET_NULL preserved the session.
        assertEquals(1, detail.totalSets)
    }

    @Test
    fun `seeded exercise delete is refused, custom delete works`() = runTest {
        open()
        val (exercises, _, _) = repos()
        exercises.ensureSeeded()
        val seeded = exercises.library().first().first { it.isSeeded }
        assertFalse(exercises.delete(seeded.id))
        val customId = exercises.addCustom(
            Exercise(
                name = "Custom Move", muscleGroup = MuscleGroup.CORE,
                category = ExerciseCategory.BODYWEIGHT, equipment = Equipment.BODYWEIGHT,
                description = "d", instructions = "i", difficulty = Difficulty.BEGINNER,
            ),
        )
        assertTrue(exercises.delete(customId))
    }
}
