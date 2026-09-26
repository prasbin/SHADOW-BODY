package com.shadowbody.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.model.MorningStepOutcome
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 5 relational contract, verified against a real Room database with
 * foreign keys enforced.
 *
 * The rules the whole phase leans on: a seeded routine can never be duplicated,
 * a step order is unique inside its routine, a recorded step belongs to its run,
 * and deleting a routine or a run never rewrites history — it only detaches it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase5SchemaTest {

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

    private fun step(
        routineId: Long,
        position: Int,
        title: String = "Step $position",
        durationSec: Int? = null,
        reps: Int? = null,
    ) = MorningRoutineStep(
        routineId = routineId,
        title = title,
        instructions = "Do step $position",
        category = MorningStepCategory.MOBILITY,
        targetDurationSec = durationSec,
        targetReps = reps,
        position = position,
    )

    private suspend fun seedRoutine(name: String = "TEST ROUTINE"): Long =
        database().morningRoutineDao().insert(
            MorningRoutine(name = name, createdAt = 1L, updatedAt = 1L),
        )

    private suspend fun seedRun(
        routineId: Long?,
        dayKey: String = "2026-09-26",
        attempt: Int = 1,
        status: MorningLogStatus = MorningLogStatus.IN_PROGRESS,
        totalSteps: Int = 2,
        startedAt: Long = 100L,
    ) = MorningRoutineLog(
        routineId = routineId,
        routineName = "TEST ROUTINE",
        dayKey = dayKey,
        attempt = attempt,
        startedAt = startedAt,
        status = status,
        totalSteps = totalSteps,
    )

    @Test
    fun `the seed key is unique so a built-in routine can never be duplicated`() = runTest {
        val dao = database().morningRoutineDao()
        val first = dao.insertIgnore(
            MorningRoutine(seedKey = "SEED", name = "A", createdAt = 1L, updatedAt = 1L),
        )
        val second = dao.insertIgnore(
            MorningRoutine(seedKey = "SEED", name = "B", createdAt = 1L, updatedAt = 1L),
        )
        assertTrue(first > 0)
        assertEquals(-1L, second)
        assertEquals(1, dao.count())
        assertEquals("A", dao.getBySeedKey("SEED")?.name)
    }

    @Test
    fun `user routines have a null seed key and may share it`() = runTest {
        val dao = database().morningRoutineDao()
        val first = dao.insert(MorningRoutine(name = "One"))
        val second = dao.insert(MorningRoutine(name = "Two"))
        assertTrue(first > 0 && second > 0)
        // SQLite allows any number of NULLs in a unique index.
        assertEquals(2, dao.count())
    }

    @Test
    fun `step order is unique inside one routine`() = runTest {
        val routineId = seedRoutine()
        val steps = database().morningRoutineStepDao()
        steps.insertIgnore(step(routineId, 0))
        val duplicate = steps.insertIgnore(step(routineId, 0, title = "Other"))
        assertEquals(-1L, duplicate)
        assertEquals(1, steps.countByRoutine(routineId))
    }

    @Test
    fun `the same position in a different routine is fine`() = runTest {
        val first = seedRoutine("A")
        val second = seedRoutine("B")
        val steps = database().morningRoutineStepDao()
        steps.insert(step(first, 0))
        steps.insert(step(second, 0))
        assertEquals(1, steps.countByRoutine(first))
        assertEquals(1, steps.countByRoutine(second))
    }

    @Test
    fun `deleting a routine cascades to its steps`() = runTest {
        val routineId = seedRoutine()
        val steps = database().morningRoutineStepDao()
        steps.insert(step(routineId, 0))
        steps.insert(step(routineId, 1))

        database().morningRoutineDao().delete(routineId)

        assertEquals(0, steps.countByRoutine(routineId))
    }

    @Test
    fun `deleting a routine keeps its recorded runs and detaches them`() = runTest {
        val routineId = seedRoutine()
        val logs = database().morningRoutineLogDao()
        val runId = logs.startRun(seedRun(routineId), totalSteps = 2)

        database().morningRoutineDao().delete(routineId)

        val kept = logs.getLog(runId)
        assertNotNull("history must survive a routine deletion", kept)
        assertNull(kept?.routineId)
        assertEquals("TEST ROUTINE", kept?.routineName)
    }

    @Test
    fun `deleting a run removes its step outcomes`() = runTest {
        val routineId = seedRoutine()
        val logs = database().morningRoutineLogDao()
        val runId = logs.startRun(seedRun(routineId), totalSteps = 2)
        logs.recordStepOutcome(
            log = checkNotNull(logs.getLog(runId)),
            stepLog = MorningRoutineStepLog(
                logId = runId,
                stepId = null,
                position = 0,
                title = "Step 0",
                category = MorningStepCategory.MOBILITY,
                outcome = MorningStepOutcome.COMPLETED,
            ),
        )
        assertEquals(1, logs.getStepLogs(runId).size)

        database().openHelper.writableDatabase
            .execSQL("DELETE FROM morning_routine_log WHERE id = $runId")

        assertEquals(0, logs.getStepLogs(runId).size)
    }

    @Test
    fun `deleting a step detaches the snapshot instead of losing the run`() = runTest {
        val routineId = seedRoutine()
        val steps = database().morningRoutineStepDao()
        val stepId = steps.insert(step(routineId, 0, title = "Doomed step"))
        steps.insert(step(routineId, 1))

        val logs = database().morningRoutineLogDao()
        val runId = logs.startRun(seedRun(routineId), totalSteps = 2)
        logs.recordStepOutcome(
            log = checkNotNull(logs.getLog(runId)),
            stepLog = MorningRoutineStepLog(
                logId = runId,
                stepId = stepId,
                position = 0,
                title = "Doomed step",
                category = MorningStepCategory.MOBILITY,
                outcome = MorningStepOutcome.COMPLETED,
            ),
        )

        steps.delete(stepId)

        val rows = logs.getStepLogs(runId)
        assertEquals(1, rows.size)
        assertNull("the step reference is cleared", rows.first().stepId)
        assertEquals("the snapshot still describes it", "Doomed step", rows.first().title)
    }

    @Test
    fun `re-marking a step replaces its outcome instead of duplicating it`() = runTest {
        val routineId = seedRoutine()
        val stepId = database().morningRoutineStepDao().insert(step(routineId, 0))
        val logs = database().morningRoutineLogDao()
        val runId = logs.startRun(seedRun(routineId), totalSteps = 1)
        val log = checkNotNull(logs.getLog(runId))

        suspend fun record(outcome: MorningStepOutcome) = logs.recordStepOutcome(
            log = log,
            stepLog = MorningRoutineStepLog(
                logId = runId,
                stepId = stepId,
                position = 0,
                title = "Step 0",
                category = MorningStepCategory.MOBILITY,
                outcome = outcome,
            ),
        )

        assertEquals(0, record(MorningStepOutcome.SKIPPED).completedSteps)
        assertEquals(1, record(MorningStepOutcome.SKIPPED).skippedSteps)
        val corrected = record(MorningStepOutcome.COMPLETED)
        assertEquals(1, logs.getStepLogs(runId).size)
        assertEquals(1, corrected.completedSteps)
        assertEquals(0, corrected.skippedSteps)
    }

    @Test
    fun `parent counters always match the recorded step rows`() = runTest {
        val routineId = seedRoutine()
        val steps = database().morningRoutineStepDao()
        val firstId = steps.insert(step(routineId, 0))
        steps.insert(step(routineId, 1))
        val logs = database().morningRoutineLogDao()
        val runId = logs.startRun(seedRun(routineId), totalSteps = 2)
        val log = checkNotNull(logs.getLog(runId))

        val afterSkip = logs.recordStepOutcome(
            log = log,
            stepLog = MorningRoutineStepLog(
                logId = runId,
                stepId = firstId,
                position = 0,
                title = "Step 0",
                category = MorningStepCategory.MOBILITY,
                outcome = MorningStepOutcome.SKIPPED,
            ),
        )
        assertEquals(0, afterSkip.completedSteps)
        assertEquals(1, afterSkip.skippedSteps)

        // A hand-written duplicate is still refused by the unique index.
        try {
            database().openHelper.writableDatabase.execSQL(
                "INSERT INTO morning_routine_step_log (logId, stepId, position, title, " +
                    "category, outcome, recordedAt) VALUES " +
                    "($runId, $firstId, 0, 'Step 0', 'MOBILITY', 'COMPLETED', 5)",
            )
            throw AssertionError("a duplicate (logId, position) must be rejected")
        } catch (e: SQLiteConstraintException) {
            // Expected: unique(logId, position).
        }
    }

    @Test
    fun `one attempt per routine per day is unique`() = runTest {
        val routineId = seedRoutine()
        val logs = database().morningRoutineLogDao()
        logs.startRun(seedRun(routineId, attempt = 1), totalSteps = 2)
        try {
            database().openHelper.writableDatabase.execSQL(
                "INSERT INTO morning_routine_log (routineId, routineName, dayKey, attempt, " +
                    "startedAt, status, completedSteps, skippedSteps, totalSteps, notes) " +
                    "VALUES ($routineId, 'T', '2026-09-26', 1, 1, 'ABANDONED', 0, 0, 2, '')",
            )
            throw AssertionError("a second attempt 1 for the same day must be rejected")
        } catch (e: SQLiteConstraintException) {
            // Expected: unique(routineId, dayKey, attempt).
        }
        // A retry on the same day is a different attempt and is allowed.
        logs.startRun(seedRun(routineId, attempt = 2, status = MorningLogStatus.ABANDONED), totalSteps = 2)
        assertEquals(2, logs.count())
    }

    @Test
    fun `history is ordered newest first`() = runTest {
        val routineId = seedRoutine()
        val logs = database().morningRoutineLogDao()
        logs.startRun(seedRun(routineId, "2026-09-24", attempt = 1, startedAt = 100L), 2)
        logs.startRun(seedRun(routineId, "2026-09-25", attempt = 1, startedAt = 200L), 2)
        logs.startRun(seedRun(routineId, "2026-09-26", attempt = 1, startedAt = 300L), 2)

        val history = logs.getHistory(limit = 10)
        assertEquals(listOf(300L, 200L, 100L), history.map { it.startedAt })
        assertEquals(1, logs.getHistory(limit = 1).size)
    }

    @Test
    fun `completions for a day exclude the log being checked`() = runTest {
        val routineId = seedRoutine()
        val logs = database().morningRoutineLogDao()
        val completed = logs.startRun(
            seedRun(routineId, attempt = 1, status = MorningLogStatus.COMPLETED),
            2,
        )
        val open = logs.startRun(
            seedRun(routineId, attempt = 2, status = MorningLogStatus.IN_PROGRESS),
            2,
        )
        // Excluding the completed row itself leaves nothing counted.
        assertEquals(0, logs.completionsForDay(routineId, "2026-09-26", completed))
        // Excluding the open row counts the other one.
        assertEquals(1, logs.completionsForDay(routineId, "2026-09-26", open))
        // A different day has no completions at all.
        assertEquals(0, logs.completionsForDay(routineId, "2026-09-27", open))
    }

    @Test
    fun `a run is only ever closed once`() = runTest {
        val routineId = seedRoutine()
        val logs = database().morningRoutineLogDao()
        val runId = logs.startRun(seedRun(routineId), 1)
        val log = checkNotNull(logs.getLog(runId))
        val closed = logs.closeRun(log, MorningLogStatus.ABANDONED, completedAt = 500L)

        assertEquals(MorningLogStatus.ABANDONED, closed.status)
        assertEquals(500L, closed.completedAt)
        assertFalse(logs.getLog(runId)!!.status == MorningLogStatus.IN_PROGRESS)
    }
}
