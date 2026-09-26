package com.shadowbody.app.data.preferences

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DataStore foundation test: theme preference defaults, writes, and
 * re-reads. Corrupt/unknown stored values must fall back to SYSTEM,
 * never crash startup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPreferencesTest {

    @Test
    fun `default theme is SYSTEM`() = runTest {
        val prefs = AppPreferences(ApplicationProvider.getApplicationContext())
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())
    }

    @Test
    fun `written theme persists on a fresh instance`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppPreferences(context).setThemeMode(ThemeMode.DARK)
        val fresh = AppPreferences(context)
        assertEquals(ThemeMode.DARK, fresh.themeMode.first())
        // Restore default so tests stay order-independent.
        fresh.setThemeMode(ThemeMode.SYSTEM)
    }
}
