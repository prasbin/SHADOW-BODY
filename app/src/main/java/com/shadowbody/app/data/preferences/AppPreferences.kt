package com.shadowbody.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App-level appearance preference. Dark-first; SYSTEM currently = dark. */
enum class ThemeMode {
    SYSTEM,
    DARK,
}

private val Context.phase1DataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppPreferences.STORE_NAME,
)

/**
 * Phase 1 DataStore foundation for **app-level preferences only**
 * (theme, etc.). User profile/body data belongs in Room entities
 * (Phases 2+), never here.
 */
class AppPreferences(
    context: Context,
    private val store: DataStore<Preferences> = context.applicationContext.phase1DataStore,
) {
    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        runCatching { if (raw == null) ThemeMode.SYSTEM else ThemeMode.valueOf(raw) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    companion object {
        const val STORE_NAME = "shadow_body_prefs"
    }
}
