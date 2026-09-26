package com.shadowbody.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the navigation contract: unique routes, sane start destination. */
class RoutesTest {

    @Test
    fun `start destination is the dashboard`() {
        assertEquals(Routes.DASHBOARD, Routes.START)
    }

    @Test
    fun `all routes are distinct and non-blank`() {
        assertTrue(Routes.all.isNotEmpty())
        assertEquals(Routes.all.size, Routes.all.distinct().size)
        Routes.all.forEach { assertTrue(it.isNotBlank()) }
    }

    @Test
    fun `start destination is registered`() {
        assertTrue(Routes.all.contains(Routes.START))
    }
}
