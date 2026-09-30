package com.shadowbody.app.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardModulesTest {

    @Test
    fun `dashboard starts with default state`() {
        val state = DashboardUiState()
        assertEquals(1, state.level)
        assertEquals(0, state.totalXp)
        assertEquals(0, state.currentStreak)
        assertFalse(state.profileConfigured)
    }

    @Test
    fun `dashboard state holds progression data`() {
        val state = DashboardUiState(
            level = 5,
            totalXp = 450,
            currentStreak = 7,
            profileConfigured = true,
        )
        assertEquals(5, state.level)
        assertEquals(450, state.totalXp)
        assertEquals(7, state.currentStreak)
        assertTrue(state.profileConfigured)
    }

    @Test
    fun `dashboard state holds today status`() {
        val state = DashboardUiState(
            morningStatus = "COMPLETED",
            groomingStatus = "IN_PROGRESS",
            hydrationMl = 1500,
            hydrationGoalMl = 3000,
        )
        assertEquals("COMPLETED", state.morningStatus)
        assertEquals("IN_PROGRESS", state.groomingStatus)
        assertEquals(1500, state.hydrationMl)
        assertEquals(3000, state.hydrationGoalMl)
    }
}
