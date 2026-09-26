package com.shadowbody.app.navigation

/** Single source of truth for navigation routes. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"

    /** Every route registered in the NavHost. Used by tests to catch typos. */
    val all: List<String> = listOf(DASHBOARD, SETTINGS)

    const val START = DASHBOARD
}
