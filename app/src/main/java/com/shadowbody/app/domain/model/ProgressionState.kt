package com.shadowbody.app.domain.model

/**
 * Phase 4: what the adaptive engine did to one exercise's target after the
 * most recent completed session. Descriptive only — never a medical or
 * performance judgement about the person.
 */
enum class ProgressionState(val label: String) {
    /** No completed session yet; the target is the seeded/planned one. */
    UNTRACKED("Untracked"),
    MAINTAINING("Maintaining"),
    PROGRESSING("Progressing"),
    REGRESSING("Reducing"),
}
