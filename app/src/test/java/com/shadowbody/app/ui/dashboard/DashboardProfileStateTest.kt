package com.shadowbody.app.ui.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardProfileStateTest {

    @Test
    fun `dashboard starts with profile not configured`() {
        val state = DashboardUiState()
        assertFalse(state.profileConfigured)
    }

    @Test
    fun `dashboard reflects configured profile`() {
        val state = DashboardUiState(profileConfigured = true)
        assertTrue(state.profileConfigured)
    }
}
