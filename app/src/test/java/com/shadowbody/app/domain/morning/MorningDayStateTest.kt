package com.shadowbody.app.domain.morning

import com.shadowbody.app.data.local.MorningRoutineLog
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.model.MorningStepOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** Phase 5 "today" is a local calendar day, not a UTC one. */
class MorningDayKeyTest {

    @Test
    fun `key is the local ISO date`() {
        val zone = ZoneId.of("Europe/Prague")
        val millis = ZonedDateTime.of(2026, 9, 26, 7, 30, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals("2026-09-26", MorningDayKey.of(millis, zone))
    }

    @Test
    fun `the same instant can be two different days in two zones`() {
        val utcNoon = ZonedDateTime.of(2026, 9, 26, 23, 30, 0, 0, ZoneId.of("UTC"))
            .toInstant().toEpochMilli()
        // 23:30 UTC is already the next morning in Tokyo.
        assertEquals("2026-09-26", MorningDayKey.of(utcNoon, ZoneId.of("UTC")))
        assertEquals("2026-09-27", MorningDayKey.of(utcNoon, ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun `keys sort chronologically as text`() {
        val keys = listOf("2026-01-02", "2026-10-01", "2026-02-28").sorted()
        assertEquals(listOf("2026-01-02", "2026-02-28", "2026-10-01"), keys)
    }

    @Test
    fun `parse accepts a produced key and rejects junk`() {
        assertEquals("2026-09-26", MorningDayKey.parse("2026-09-26").toString())
        assertNull(MorningDayKey.parse("26/09/2026"))
        assertNull(MorningDayKey.parse(""))
    }

    @Test
    fun `today defaults to the system zone`() {
        assertEquals(10, MorningDayKey.today().length)
    }
}

/**
 * Daily state resolution.
 *
 * An open run must always win, because that is what the user is in the middle
 * of; a completion must beat an abandoned attempt; and an abandoned attempt is
 * still "started" rather than "not started".
 */
class MorningDayStateTest {

    private fun log(
        id: Long,
        status: MorningLogStatus,
        completed: Int = 0,
        skipped: Int = 0,
        total: Int = 3,
        startedAt: Long = id * 10,
    ) = MorningRoutineLog(
        id = id,
        routineId = 7L,
        routineName = "MORNING",
        dayKey = "2026-09-26",
        attempt = id.toInt(),
        startedAt = startedAt,
        completedAt = if (status == MorningLogStatus.IN_PROGRESS) null else startedAt + 60,
        status = status,
        completedSteps = completed,
        skippedSteps = skipped,
        totalSteps = total,
    )

    @Test
    fun `no logs means not started`() {
        val state = MorningDayState.resolve("2026-09-26", 7L, emptyList())
        assertEquals(MorningDayStatus.NOT_STARTED, state.status)
        assertNull(state.activeLogId)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `an open run outranks a completed one`() {
        val state = MorningDayState.resolve(
            "2026-09-26",
            7L,
            listOf(log(1, MorningLogStatus.COMPLETED, completed = 3), log(2, MorningLogStatus.IN_PROGRESS)),
        )
        assertEquals(MorningDayStatus.IN_PROGRESS, state.status)
        assertEquals(2L, state.activeLogId)
    }

    @Test
    fun `a completion outranks an abandoned attempt`() {
        val state = MorningDayState.resolve(
            "2026-09-26",
            7L,
            listOf(log(1, MorningLogStatus.ABANDONED, skipped = 3), log(2, MorningLogStatus.COMPLETED, completed = 3)),
        )
        assertEquals(MorningDayStatus.COMPLETED, state.status)
        assertTrue(state.isFinished)
    }

    @Test
    fun `an abandoned attempt still counts as started`() {
        val state = MorningDayState.resolve(
            "2026-09-26",
            7L,
            listOf(log(1, MorningLogStatus.ABANDONED, completed = 1, skipped = 1, total = 3)),
        )
        assertEquals(MorningDayStatus.ABANDONED, state.status)
    }

    @Test
    fun `another routine's run never leaks into this state`() {
        val foreign = log(1, MorningLogStatus.COMPLETED, completed = 3).copy(routineId = 99L)
        val state = MorningDayState.resolve("2026-09-26", 7L, listOf(foreign))
        assertEquals(MorningDayStatus.NOT_STARTED, state.status)
    }

    @Test
    fun `progress counts skipped steps as dealt with`() {
        val state = MorningDayState.resolve(
            "2026-09-26",
            7L,
            listOf(log(1, MorningLogStatus.IN_PROGRESS, completed = 2, skipped = 1, total = 4)),
        )
        assertEquals(0.75f, state.progress, 0.001f)
    }
}

/** Step display rules: targets are labelled honestly, including "no target". */
class MorningRunStepTest {

    private fun step(
        durationSec: Int? = null,
        reps: Int? = null,
        outcome: MorningStepOutcome? = null,
    ) = MorningRunStep(
        position = 0,
        title = "Step",
        instructions = "Do it",
        category = MorningStepCategory.MOBILITY,
        targetDurationSec = durationSec,
        targetReps = reps,
        outcome = outcome,
    )

    @Test
    fun `a timed step is labelled in seconds`() {
        assertEquals("60s", step(durationSec = 60).targetText)
        assertTrue(step(durationSec = 60).isTimed)
    }

    @Test
    fun `a counted step is labelled in reps`() {
        assertEquals("12 reps", step(reps = 12).targetText)
        assertTrue(step(reps = 12).isCounted)
    }

    @Test
    fun `a step with both targets shows both`() {
        assertEquals("45s · 10 reps", step(durationSec = 45, reps = 10).targetText)
    }

    @Test
    fun `a step with no target is a reminder, not a failure`() {
        assertEquals("REMINDER", step().targetText)
        assertFalse(step().isTimed)
        assertFalse(step().isCounted)
    }

    @Test
    fun `a step is pending until an outcome is recorded`() {
        assertTrue(step().isPending)
        assertFalse(step(outcome = MorningStepOutcome.SKIPPED).isPending)
    }
}
