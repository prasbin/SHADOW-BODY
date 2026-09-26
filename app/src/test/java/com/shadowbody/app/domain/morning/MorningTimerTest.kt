package com.shadowbody.app.domain.morning

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 step countdown.
 *
 * The clock is injected so a test can move "device uptime" independently of
 * virtual time, which is what proves the deadline-based semantics: remaining
 * time comes from the deadline, not from a decrementing counter.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MorningTimerTest {

    @Test
    fun `start counts down to finished`() = runTest {
        var now = 0L
        val timer = MorningTimer(backgroundScope) { now }
        timer.start(3)
        assertEquals(MorningTimerPhase.RUNNING, timer.state.value.phase)
        assertEquals(3, timer.state.value.remainingSec)

        now += 1_000
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, timer.state.value.remainingSec)

        now += 2_000
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(MorningTimerPhase.FINISHED, timer.state.value.phase)
        assertEquals(0, timer.state.value.remainingSec)
    }

    @Test
    fun `pause freezes the countdown and resume continues it`() = runTest {
        var now = 0L
        val timer = MorningTimer(backgroundScope) { now }
        timer.start(10)
        now += 3_000
        advanceTimeBy(3_000)
        runCurrent()

        timer.pause()
        assertEquals(MorningTimerPhase.PAUSED, timer.state.value.phase)
        assertEquals(7, timer.state.value.remainingSec)

        // Time passing while paused must not be charged to the countdown.
        now += 5_000
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(7, timer.state.value.remainingSec)

        timer.resume()
        now += 7_000
        advanceTimeBy(7_000)
        runCurrent()
        assertEquals(MorningTimerPhase.FINISHED, timer.state.value.phase)
    }

    @Test
    fun `reset returns to idle`() = runTest {
        var now = 0L
        val timer = MorningTimer(backgroundScope) { now }
        timer.start(10)
        now += 2_000
        advanceTimeBy(2_000)
        runCurrent()
        timer.reset()
        assertEquals(MorningTimerState(), timer.state.value)
    }

    @Test
    fun `addSeconds extends a running countdown`() = runTest {
        var now = 0L
        val timer = MorningTimer(backgroundScope) { now }
        timer.start(10)
        timer.addSeconds(30)
        assertEquals(40, timer.state.value.totalSec)
        assertEquals(40, timer.state.value.remainingSec)
    }

    @Test
    fun `addSeconds extends a paused countdown`() = runTest {
        var now = 0L
        val timer = MorningTimer(backgroundScope) { now }
        timer.start(10)
        now += 4_000
        advanceTimeBy(4_000)
        runCurrent()
        timer.pause()
        timer.addSeconds(15)
        assertEquals(MorningTimerPhase.PAUSED, timer.state.value.phase)
        assertEquals(21, timer.state.value.remainingSec)
    }

    @Test
    fun `non-positive start is rejected`() = runTest {
        val timer = MorningTimer(backgroundScope) { 0L }
        var thrown = false
        try {
            timer.start(0)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
        assertEquals(MorningTimerPhase.IDLE, timer.state.value.phase)
    }

    @Test
    fun `a running timer survives recreation without gaining or losing time`() = runTest {
        var now = 0L
        val original = MorningTimer(backgroundScope) { now }
        original.start(60)
        now += 20_000
        advanceTimeBy(20_000)
        runCurrent()
        val snapshot = original.snapshot()

        // "Recreation": a brand new timer on the same device clock.
        val restored = MorningTimer(backgroundScope) { now }
        restored.restore(snapshot)
        assertEquals(MorningTimerPhase.RUNNING, restored.state.value.phase)
        assertEquals(40, restored.state.value.remainingSec)

        now += 5_000
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(35, restored.state.value.remainingSec)
    }

    @Test
    fun `a countdown that expired while away comes back finished`() = runTest {
        var now = 0L
        val original = MorningTimer(backgroundScope) { now }
        original.start(30)
        now += 10_000
        advanceTimeBy(10_000)
        runCurrent()
        val snapshot = original.snapshot()

        // The process was gone for longer than the step itself.
        now += 120_000
        val restored = MorningTimer(backgroundScope) { now }
        restored.restore(snapshot)
        assertEquals(MorningTimerPhase.FINISHED, restored.state.value.phase)
        assertEquals(0, restored.state.value.remainingSec)
    }

    @Test
    fun `a paused snapshot is restored as paused`() = runTest {
        var now = 0L
        val original = MorningTimer(backgroundScope) { now }
        original.start(45)
        now += 5_000
        advanceTimeBy(5_000)
        runCurrent()
        original.pause()

        val restored = MorningTimer(backgroundScope) { now }
        restored.restore(original.snapshot())
        assertEquals(MorningTimerPhase.PAUSED, restored.state.value.phase)
        assertEquals(40, restored.state.value.remainingSec)
    }
}
