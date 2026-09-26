package com.shadowbody.app.domain.model

/**
 * Availability of a SHADOW BODY system module on the dashboard.
 *
 * Phase 1 only ships [AVAILABLE] for Dashboard/Settings; every Phase 2-10
 * module is [LOCKED] until its phase lands. Nothing here represents a
 * medical measurement.
 */
enum class ModuleState {
    AVAILABLE,
    LOCKED,
}

/** Dashboard entry for one future or present system module. */
data class SystemModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val phase: Int,
    val state: ModuleState,
)
