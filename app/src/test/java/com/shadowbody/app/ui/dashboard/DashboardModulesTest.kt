package com.shadowbody.app.ui.dashboard

import com.shadowbody.app.domain.model.ModuleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 1 dashboard contract: every future module is listed exactly once,
 * honestly LOCKED, and mapped to its owning phase (2-10). No future
 * functionality may appear as available.
 */
class DashboardModulesTest {

    @Test
    fun `dashboard lists all future modules`() {
        val modules = defaultModules()
        assertEquals(9, modules.size)
        assertEquals(9, modules.map { it.id }.distinct().size)
    }

    @Test
    fun `every future module is locked in phase 1`() {
        defaultModules().forEach {
            assertEquals("Module ${it.id} must be LOCKED in Phase 1", ModuleState.LOCKED, it.state)
        }
    }

    @Test
    fun `modules map to phases 2 through 10`() {
        val phases = defaultModules().map { it.phase }.sorted()
        assertEquals((2..10).toList(), phases)
    }

    @Test
    fun `viewmodel exposes the module list on first emission`() {
        val state = DashboardViewModel().uiState.value
        assertEquals(1, state.level)
        assertTrue(state.modules.isNotEmpty())
        assertTrue(state.modules.all { it.state == ModuleState.LOCKED })
    }
}
