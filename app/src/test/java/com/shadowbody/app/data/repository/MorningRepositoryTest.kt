package com.shadowbody.app.data.repository

import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.data.local.MorningRoutine
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.domain.morning.MorningDayStatus
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.model.MorningStepOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 5 behaviour: seeding, one run per day, honest completion and immutable
 * history. This is the layer the UI trusts, so every rule is asserted here
 * against a real database rather than a mock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MorningRepositoryTest {

    private var db: ShadowBodyDatabase? = null
    private lateinit var routines: MorningRoutineRepository
    private lateinit var activation: MorningActivationRepository

    private val day = "2026-09-26"
    private val otherDay = "2026-09-27"

    @Before
    fun setUp() {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        routines = MorningRoutineRepository(
            checkNotNull(db).morningRoutineDao(),
            checkNotNull(db).morningRoutineStepDao(),
        )
        activation = MorningActivationRepository(
            checkNotNull(db).morningRoutineLogDao(),
            checkNotNull(db).morningRoutineDao(),
            checkNotNull(db).morningRoutineStepDao(),
        )
    }

    @After
    fun tearDown() {
        db?.close()
        db = null
    }

    private suspend fun seedThreeStepRoutine(): Long {
        val database = checkNotNull(db)
        val routineId = database.morningRoutineDao().insert(
            MorningRoutine(name = "SHORT ROUTINE", createdAt = 1L, updatedAt = 1L),
        )
        listOf(
            "Water" to null,
            "Breathe" to 60,
            "Squat" to null,
        ).forEachIndexed { index, (title, duration) ->
            database.morningRoutineStepDao().insert(
                databaseStep(routineId, index, title, duration),
            )
        }
        return routineId
    }

    private fun databaseStep(
        routineId: Long,
        position: Int,
        title: String,
        durationSec: Int?,
    ) = com.shadowbody.app.data.local.MorningRoutineStep(
        routineId = routineId,
        title = title,
        instructions = "Do $title",
        category = MorningStepCategory.MOBILITY,
        targetDurationSec = durationSec,
        targetReps = if (durationSec == null) 10 else null,
        position = position,
    )

    private suspend fun startLog(routineId: Long, dayKey: String = day): Long =
        when (val result = activation.startRun(routineId, dayKey, now = 100L)) {
            is MorningStartResult.Run -> result.logId
            else -> error("expected a run, got $result")
        }

    private suspend fun runToEnd(logId: Long, completed: Int, skipped: Int) {
        val detail = checkNotNull(activation.runDetail(logId))
        detail.steps.forEachIndexed { index, step ->
            val outcome = if (index < completed) {
                MorningStepOutcome.COMPLETED
            } else {
                MorningStepOutcome.SKIPPED
            }
            activation.recordStep(logId, step, outcome, now = 200L)
        }
        assertEquals(skipped, detail.steps.size - completed)
    }

    // --- Seeding ---

    @Test
    fun `seeding is duplicate-safe across repeated calls`() = runTest {
        val first = routines.ensureSeeded(now = 1L)
        val second = routines.ensureSeeded(now = 2L)
        assertEquals(first, second)
        assertEquals(1, checkNotNull(db).morningRoutineDao().count())
        assertEquals(
            com.shadowbody.app.data.local.MorningRoutineSeeds.STEPS.size,
            checkNotNull(db).morningRoutineStepDao().countByRoutine(first),
        )
    }

    @Test
    fun `a fresh install has no history until something is run`() = runTest {
        val routineId = routines.ensureSeeded()
        assertEquals(0, activation.logCount())
        assertEquals(MorningDayStatus.NOT_STARTED, activation.dayState(routineId, day).status)
    }

    // --- Starting a run ---

    @Test
    fun `starting a run creates an in-progress log with the step total fixed`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        val log = checkNotNull(activation.runDetail(logId)).log
        assertEquals(MorningLogStatus.IN_PROGRESS, log.status)
        assertEquals(3, log.totalSteps)
        assertEquals(1, log.attempt)
        assertEquals(day, log.dayKey)
    }

    @Test
    fun `starting twice on the same day resumes the open run`() = runTest {
        val routineId = seedThreeStepRoutine()
        val first = startLog(routineId)
        val again = activation.startRun(routineId, day, now = 200L)
        assertTrue(again is MorningStartResult.Run)
        assertEquals(first, (again as MorningStartResult.Run).logId)
        assertTrue(again.resumed)
        assertEquals(1, activation.logCount())
    }

    @Test
    fun `a completed day cannot be started twice`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        runToEnd(logId, completed = 3, skipped = 0)
        val result = activation.finish(logId, now = 300L)
        assertTrue(result is MorningFinishResult.Closed)

        val second = activation.startRun(routineId, day, now = 400L)
        assertTrue(second is MorningStartResult.AlreadyCompleted)
        assertEquals(1, activation.logCount())
    }

    @Test
    fun `a new day is a new run`() = runTest {
        val routineId = seedThreeStepRoutine()
        startLog(routineId, day)
        val next = activation.startRun(routineId, otherDay, now = 500L)
        assertTrue(next is MorningStartResult.Run)
        assertEquals(2, activation.logCount())
    }

    @Test
    fun `an abandoned run may be retried the same day as a new attempt`() = runTest {
        val routineId = seedThreeStepRoutine()
        val first = startLog(routineId)
        assertTrue(activation.abandon(first, now = 300L))

        val second = activation.startRun(routineId, day, now = 400L)
        assertTrue(second is MorningStartResult.Run)
        assertNotEquals(first, (second as MorningStartResult.Run).logId)
        assertEquals(2, checkNotNull(activation.runDetail(second.logId)).log.attempt)
    }

    @Test
    fun `a routine without enabled steps cannot be run`() = runTest {
        val routineId = seedThreeStepRoutine()
        checkNotNull(db).morningRoutineStepDao().deleteByRoutine(routineId)
        assertEquals(MorningStartResult.NoEnabledSteps, activation.startRun(routineId, day))
    }

    @Test
    fun `a missing routine is reported, not crashed on`() = runTest {
        assertEquals(MorningStartResult.RoutineMissing, activation.startRun(9_999L, day))
    }

    // --- Recording steps ---

    @Test
    fun `recorded steps update the counters and never claim a skip as done`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        val steps = checkNotNull(activation.runDetail(logId)).steps

        val afterSkip = activation.recordStep(logId, steps[0], MorningStepOutcome.SKIPPED)
        assertTrue(afterSkip is MorningStepResult.Recorded)
        assertEquals(0, (afterSkip as MorningStepResult.Recorded).log.completedSteps)
        assertEquals(1, afterSkip.log.skippedSteps)

        val afterComplete = activation.recordStep(logId, steps[1], MorningStepOutcome.COMPLETED)
        assertEquals(1, (afterComplete as MorningStepResult.Recorded).log.completedSteps)
        assertEquals(1, afterComplete.log.skippedSteps)
    }

    @Test
    fun `a closed run does not accept new step outcomes`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        runToEnd(logId, completed = 3, skipped = 0)
        activation.finish(logId, now = 300L)

        val steps = checkNotNull(activation.runDetail(logId)).steps
        assertEquals(
            MorningStepResult.RunClosed,
            activation.recordStep(logId, steps[0], MorningStepOutcome.SKIPPED),
        )
    }

    @Test
    fun `history is observable`() = runTest {
        val routineId = seedThreeStepRoutine()
        startLog(routineId, day)
        startLog(routineId, otherDay)
        val observed = activation.observeHistory(10).first()
        assertEquals(2, observed.size)
        assertEquals(otherDay, observed.first().dayKey)
    }

    // --- Finishing ---

    @Test
    fun `finishing with untouched steps is refused and writes nothing`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        assertEquals(MorningFinishResult.NotReady, activation.finish(logId, now = 300L))
        assertEquals(
            MorningLogStatus.IN_PROGRESS,
            checkNotNull(activation.runDetail(logId)).log.status,
        )
    }

    @Test
    fun `a run with nothing completed is stored as abandoned, never as a success`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        runToEnd(logId, completed = 0, skipped = 3)

        val result = activation.finish(logId, now = 300L)
        assertTrue(result is MorningFinishResult.Closed)
        assertEquals(MorningLogStatus.ABANDONED, (result as MorningFinishResult.Closed).log.status)
        assertTrue(!result.completed)
    }

    @Test
    fun `abandoning an unfinished run records it honestly`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        val steps = checkNotNull(activation.runDetail(logId)).steps
        activation.recordStep(logId, steps[0], MorningStepOutcome.COMPLETED, now = 200L)

        assertTrue(activation.abandon(logId, now = 300L))
        val log = checkNotNull(activation.runDetail(logId)).log
        assertEquals(MorningLogStatus.ABANDONED, log.status)
        assertEquals(300L, log.completedAt)
        assertEquals(1, log.completedSteps)
        // It is not a completion, so the day may be attempted again.
        assertEquals(MorningDayStatus.ABANDONED, activation.dayState(routineId, day).status)
        assertTrue(activation.startRun(routineId, day, now = 400L) is MorningStartResult.Run)
    }

    @Test
    fun `abandoning twice is a no-op`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        assertTrue(activation.abandon(logId, now = 300L))
        assertTrue(!activation.abandon(logId, now = 400L))
    }

    @Test
    fun `skipping everything is never a completion`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        runToEnd(logId, completed = 0, skipped = 3)
        // Still open: simply walking through the steps claiming nothing done is
        // not a completion either.
        assertEquals(MorningDayStatus.IN_PROGRESS, activation.dayState(routineId, day).status)

        val result = activation.finish(logId, now = 300L)
        assertEquals(MorningLogStatus.ABANDONED, (result as MorningFinishResult.Closed).log.status)
        assertEquals(MorningDayStatus.ABANDONED, activation.dayState(routineId, day).status)
    }

    @Test
    fun `a fully dealt-with run is stored as completed`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        runToEnd(logId, completed = 2, skipped = 1)
        val result = activation.finish(logId, now = 300L)
        assertEquals(MorningLogStatus.COMPLETED, (result as MorningFinishResult.Closed).log.status)
        assertTrue(result.completed)
    }

    @Test
    fun `finishing a finished run is a no-op`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        runToEnd(logId, completed = 3, skipped = 0)
        activation.finish(logId, now = 300L)
        assertEquals(MorningFinishResult.NotFound, activation.finish(logId, now = 400L))
    }

    // --- Immutability ---

    @Test
    fun `editing the routine later does not rewrite a recorded run`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        val before = checkNotNull(activation.runDetail(logId))
        before.steps.forEach { step ->
            activation.recordStep(logId, step, MorningStepOutcome.COMPLETED, now = 200L)
        }
        activation.finish(logId, now = 300L)

        val routine = checkNotNull(routines.getRoutine(routineId))
        val renamed = checkNotNull(routines.getSteps(routineId)).map { it.copy(title = "REWRITTEN") }
        routines.saveRoutine(routine.copy(name = "RENAMED ROUTINE"), renamed)

        val after = checkNotNull(activation.runDetail(logId))
        // The run's own snapshot of the name survives the rename.
        assertEquals("SHORT ROUTINE", after.log.routineName)
        assertEquals(
            listOf("Water", "Breathe", "Squat"),
            after.steps.map { it.title },
        )
        assertEquals(3, after.log.completedSteps)
        assertEquals(MorningLogStatus.COMPLETED, after.log.status)
    }

    @Test
    fun `steps not yet recorded still reflect the current routine`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        val steps = checkNotNull(activation.runDetail(logId)).steps
        activation.recordStep(logId, steps[0], MorningStepOutcome.COMPLETED, now = 200L)

        // Rename only the step that has not been dealt with yet.
        val routine = checkNotNull(routines.getRoutine(routineId))
        val edited = checkNotNull(routines.getSteps(routineId))
            .map { if (it.position == 1) it.copy(title = "DEEP BREATHING") else it }
        routines.saveRoutine(routine, edited)

        val after = checkNotNull(activation.runDetail(logId))
        assertEquals("Water", after.steps[0].title)
        assertEquals("DEEP BREATHING", after.steps[1].title)
        assertTrue(after.steps[1].isPending)
    }

    @Test
    fun `recorded elapsed time is stored with the step outcome`() = runTest {
        val routineId = seedThreeStepRoutine()
        val logId = startLog(routineId)
        val steps = checkNotNull(activation.runDetail(logId)).steps
        activation.recordStep(logId, steps[1], MorningStepOutcome.COMPLETED, elapsedSec = 58, now = 200L)
        val recorded = checkNotNull(activation.runDetail(logId)).steps[1]
        assertEquals(58, recorded.elapsedSec)
        assertEquals(MorningStepOutcome.COMPLETED, recorded.outcome)
    }

    // --- Routine management ---

    @Test
    fun `a new routine normalizes step positions`() = runTest {
        val id = routines.createRoutine(
            name = "MINE",
            description = "custom",
            entries = listOf(
                databaseStep(0L, 7, "A", null),
                databaseStep(0L, 9, "B", 30),
            ),
        )
        val stored = routines.getSteps(id)
        assertEquals(listOf(0, 1), stored.map { it.position })
        assertTrue(stored.all { it.routineId == id })
        assertTrue(stored.none { it.isSeeded })
    }

    @Test
    fun `a routine list is ordered deterministically`() = runTest {
        routines.createRoutine("B", "", listOf(databaseStep(0L, 0, "B", null)))
        routines.createRoutine("A", "", listOf(databaseStep(0L, 0, "A", null)))
        val list = routines.observeRoutines().first()
        // Both share sortOrder 0, so creation order breaks the tie.
        assertEquals(listOf("B", "A"), list.map { it.name })
    }
}
