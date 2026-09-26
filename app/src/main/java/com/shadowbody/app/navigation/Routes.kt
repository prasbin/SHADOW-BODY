package com.shadowbody.app.navigation

/** Single source of truth for navigation routes. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile_edit"
    const val BASELINE_HISTORY = "baseline_history"

    /** Every route registered in the NavHost. Used by tests to catch typos. */
    val all: List<String> = listOf(DASHBOARD, SETTINGS, PROFILE, PROFILE_EDIT, BASELINE_HISTORY)

    const val START = DASHBOARD
}
