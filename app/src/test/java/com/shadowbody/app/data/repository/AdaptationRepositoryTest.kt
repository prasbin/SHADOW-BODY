package com.shadowbody.app.data.repository

import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.data.local.ExerciseAdaptation
import com.shadowbody.app.data.local.ReadinessReport
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.local.WorkoutPlanExercise
import com.shadowbody.app.domain.adaptive.AdaptationLimits
import com.shadowbody.app.domain.model.ProgressionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 4 persistence behaviour against a real (in-memory) database.
 *
 * The guarantees under test are the ones the UI depends on: applying
 * completed sessions is idempotent, history is never rewritten, and the
 * engine only ever sees evidence the user actually produced.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdaptationRepositoryTest {

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

    private fun adaptation(): AdaptationRepository = AdaptationRepository(
        database().adaptationDao(),
        database().sessionDao(),
        database().readinessDao(),
        database().missedWorkoutDao(),
    )

    private suspend fun seedPlan(
        name: String,
        sets: Int = 3,
        reps: Int = 10,
    ): Pair<Long, Long> {
        val database = database()
        val exercises = ExerciseRepository(database.exerciseDao())
        exercises.ensureSeeded()
        val exercise = exercises.library().first().first()
        val plans = PlanRepository(
            database.planDao(),
            database.planExerciseDao(),
            database.exerciseDao(),
        )
        val planId = plans.savePlan(
            WorkoutPlan(name = name),
            listOf(
                WorkoutPlanExercise(
                    planId = 0,
                    exerciseId = exercise.id,
                    position = 0,
                    targetSets = sets,
                    targetReps = reps,
                ),
            ),
        )
        return planId to exercise.id
    }

    /** Completes every set of the session exactly at target. */
    private suspend fun completeSessionCleanly(planId: Long) {
        val sessions = SessionRepository(database().sessionDao(), planRepository())
        val sessionId = sessions.start(planId)
        val detail = sessions.detail(sessionId)!!
        detail.exercises.forEach { exercise ->
            exercise.sets.forEach { set ->
                sessions.recordSet(
                    set = set,
                    actualReps = set.targetReps,
                    actualDurationSec = set.targetDurationSec,
                    weightKg = null,
                    completed = true,
                )
            }
            sessions.setExerciseCompleted(exercise.sessionExercise.id, true)
        }
        sessions.finish(sessionId)
    }

    private fun planRepository(): PlanRepository = PlanRepository(
        database().planDao(),
        database().planExerciseDao(),
        database().exerciseDao(),
    )

    @Test
    fun `applying completed sessions is idempotent`() = runTest {
        val (planId, exerciseId) = seedPlan("Idempotent")
        completeSessionCleanly(planId)

        val repository = adaptation()
        assertEquals(1, repository.applyCompletedSessions())
        val afterFirst = repository.getByExercise(exerciseId)!!

        // Replaying history must not count the same session twice.
        assertEquals(0, repository.applyCompletedSessions())
        assertEquals(0, repository.applyCompletedSessions())

        val afterReplay = repository.getByExercise(exerciseId)!!
        assertEquals(afterFirst.currentSets, afterReplay.currentSets)
        assertEquals(afterFirst.sessionsAtTarget, afterReplay.sessionsAtTarget)
        assertEquals(afterFirst.lastReasonCode, afterReplay.lastReasonCode)
    }

    @Test
    fun `one clean session establishes state without progressing`() = runTest {
        val (planId, exerciseId) = seedPlan("First")
        completeSessionCleanly(planId)

        val repository = adaptation()
        repository.applyCompletedSessions()

        val row = repository.getByExercise(exerciseId)!!
        assertEquals(3, row.currentSets)
        assertEquals(1, row.sessionsAtTarget)
        assertEquals(ProgressionState.MAINTAINING, row.state)
    }

    @Test
    fun `two clean sessions progress the target by one set`() = runTest {
        val (planId, exerciseId) = seedPlan("Progress")
        val repository = adaptation()

        completeSessionCleanly(planId)
        repository.applyCompletedSessions()
        assertEquals(3, repository.getByExercise(exerciseId)!!.currentSets)

        completeSessionCleanly(planId)
        assertEquals(1, repository.applyCompletedSessions())

        val row = repository.getByExercise(exerciseId)!!
        assertEquals(4, row.currentSets)
        assertEquals(ProgressionState.PROGRESSING, row.state)
        // The raised target has to be proven from scratch.
        assertEquals(0, row.sessionsAtTarget)
    }

    @Test
    fun `an unfinished session reduces the target and says why`() = runTest {
        val (planId, exerciseId) = seedPlan("Short")
        val sessions = SessionRepository(database().sessionDao(), planRepository())
        val sessionId = sessions.start(planId)
        val detail = sessions.detail(sessionId)!!
        val first = detail.exercises[0].sets[0]
        sessions.recordSet(first, actualReps = 10, actualDurationSec = null, weightKg = null, completed = true)
        sessions.finish(sessionId)

        val repository = adaptation()
        repository.applyCompletedSessions()

        val row = repository.getByExercise(exerciseId)!!
        assertEquals(2, row.currentSets)
        assertEquals(ProgressionState.REGRESSING, row.state)
        assertEquals("SETS_REDUCED", row.lastReasonCode)
        assertTrue(row.lastReasonText.isNotBlank())
    }

    @Test
    fun `completed sessions are never rewritten`() = runTest {
        val (planId, _) = seedPlan("Immutable")
        completeSessionCleanly(planId)
        val before = SessionRepository(database().sessionDao(), planRepository())
            .recent().first()
            .single()

        adaptation().applyCompletedSessions()

        val after = SessionRepository(database().sessionDao(), planRepository())
            .recent().first()
            .single()
        assertEquals(before.id, after.id)
        assertEquals(before.startedAt, after.startedAt)
        assertEquals(before.endedAt, after.endedAt)
        assertEquals(before.status, after.status)
    }

    @Test
    fun `the session snapshot outranks remembered state`() = runTest {
        val (planId, exerciseId) = seedPlan("Edited", sets = 3)
        val repository = adaptation()
        completeSessionCleanly(planId)
        repository.applyCompletedSessions()
        assertEquals(3, repository.getByExercise(exerciseId)!!.currentSets)

        // The user lowers the plan by hand; the next session is the truth.
        planRepository().savePlan(
            WorkoutPlan(id = planId, name = "Edited"),
            listOf(
                WorkoutPlanExercise(
                    planId = 0,
                    exerciseId = exerciseId,
                    position = 0,
                    targetSets = 2,
                    targetReps = 10,
                ),
            ),
        )
        completeSessionCleanly(planId)
        repository.applyCompletedSessions()

        val row = repository.getByExercise(exerciseId)!!
        assertEquals(2, row.currentSets)
    }

    @Test
    fun `high readiness at session time lowers demand`() = runTest {
        val (planId, exerciseId) = seedPlan("Tired")
        database().readinessDao().insert(
            ReadinessReport(recordedAt = 1L, fatigue = 5, soreness = 1),
        )
        completeSessionCleanly(planId)
        adaptation().applyCompletedSessions()

        val row = adaptation().getByExercise(exerciseId)!!
        assertEquals(2, row.currentSets)
        assertEquals(ProgressionState.REGRESSING, row.state)
        assertEquals("FATIGUE_REDUCED", row.lastReasonCode)
    }

    @Test
    fun `readiness lookup returns the report that existed at that moment`() = runTest {
        val reports = database().readinessDao()
        reports.insert(ReadinessReport(recordedAt = 1_000L, fatigue = 5, soreness = 4))
        reports.insert(ReadinessReport(recordedAt = 5_000L, fatigue = 1, soreness = 1))

        assertEquals(5, reports.latestAtOrBefore(1_500L)?.fatigue)
        assertEquals(1, reports.latestAtOrBefore(9_000L)?.fatigue)
        // Nothing existed yet before the first check-in.
        assertNull(reports.latestAtOrBefore(500L))
    }

    @Test
    fun `missed sessions are only recorded on request and never touch history`() = runTest {
        val (planId, exerciseId) = seedPlan("Skip")
        val sessionsBefore = database().sessionDao().observeRecent().first().size
        val repository = adaptation()

        assertEquals(0, repository.missedInWindow())
        repository.recordMissed(planId = planId, reason = "no time")
        repository.recordMissed(planId = planId, reason = "  ")
        assertEquals(2, repository.missedInWindow())
        // Reasons are stored trimmed, never padded.
        assertEquals("", repository.latestMissed()?.reason)

        assertEquals(sessionsBefore, database().sessionDao().observeRecent().first().size)
        assertNull(repository.getByExercise(exerciseId))
    }

    @Test
    fun `missed sessions older than the window are ignored`() = runTest {
        val repository = adaptation()
        repository.recordMissed(now = 1_000L)
        val now = 1_000L + AdaptationLimits.WEEK_WINDOW_MS + 1
        assertEquals(0, repository.missedInWindow(now))
    }

    @Test
    fun `readiness check-ins are validated before they are stored`() = runTest {
        val readiness = ReadinessRepository(database().readinessDao())
        assertNull(readiness.record(fatigue = 0, soreness = 3))
        assertNull(readiness.record(fatigue = 3, soreness = 9))
        assertEquals(0, readiness.count())

        assertNotNull(readiness.record(fatigue = 4, soreness = 2, notes = "  "))
        assertEquals(1, readiness.count())
        assertEquals("", readiness.latestOnce()?.notes)
    }

    @Test
    fun `adaptation rows are unique per exercise and survive a re-read`() = runTest {
        val (planId, exerciseId) = seedPlan("Unique")
        completeSessionCleanly(planId)
        val repository = adaptation()
        repository.applyCompletedSessions()

        val first = repository.getByExercise(exerciseId)!!
        val updated: ExerciseAdaptation = first.copy(currentSets = 5)
        database().adaptationDao().upsert(updated)

        assertEquals(1, repository.count())
        assertEquals(5, repository.getByExercise(exerciseId)!!.currentSets)
    }
}
