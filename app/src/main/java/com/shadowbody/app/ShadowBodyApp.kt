package com.shadowbody.app

import android.app.Application
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.data.preferences.AppPreferences

/**
 * Phase 1 application entry point.
 *
 * Owns the single [ShadowBodyDatabase] instance and the [AppPreferences]
 * DataStore wrapper. Both are local-first: the app launches and works
 * fully offline.
 */
class ShadowBodyApp : Application() {

    val database: ShadowBodyDatabase by lazy {
        ShadowBodyDatabase.build(this)
    }

    val preferences: AppPreferences by lazy {
        AppPreferences(this)
    }
}
