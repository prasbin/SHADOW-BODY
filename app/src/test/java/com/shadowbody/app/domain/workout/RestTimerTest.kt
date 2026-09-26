package com.shadowbody.app.domain.workout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerTest {

    @Test
    fun `start counts down to finished`() = runTest {
        val timer = RestTimer(this)
        timer.start(3)
        assertEquals(RestPhase.RUNNING, timer.state.value.phase)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, timer.state.value.remainingSec)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(RestPhase.FINISHED, timer.state.value.phase)
        assertEquals(0, timer.state.value.remainingSec)
    }

    @Test
    fun `pause freezes and resume continues`() = runTest {
        val timer = RestTimer(this)
        timer.start(10)
        advanceTimeBy(3_000)
        runCurrent()
        timer.pause()
        assertEquals(RestPhase.PAUSED, timer.state.value.phase)
        assertEquals(7, timer.state.value.remainingSec)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(7, timer.state.value.remainingSec)
        timer.resume()
        advanceTimeBy(7_000)
        runCurrent()
        assertEquals(RestPhase.FINISHED, timer.state.value.phase)
    }

    @Test
    fun `reset returns to idle`() = runTest {
        val timer = RestTimer(this)
        timer.start(10)
        advanceTimeBy(2_000)
        runCurrent()
        timer.reset()
        assertEquals(RestState(), timer.state.value)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(RestPhase.IDLE, timer.state.value.phase)
    }

    @Test
    fun `addSeconds extends running rest`() = runTest {
        val timer = RestTimer(this)
        timer.start(10)
        timer.addSeconds(15)
        assertEquals(25, timer.state.value.remainingSec)
        assertEquals(25, timer.state.value.totalSec)
    }

    @Test
    fun `non-positive start is rejected`() = runTest {
        val timer = RestTimer(this)
        var thrown = false
        try {
            timer.start(0)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
        assertEquals(RestPhase.IDLE, timer.state.value.phase)
    }
}
