package com.shadowbody.app.domain.model

/**
 * Practical training goals. Labels describe intent only — no outcome is
 * promised and nothing here is medical advice.
 */
enum class Goal(val label: String) {
    BUILD_STRENGTH("Build strength"),
    BUILD_MUSCLE("Build muscle"),
    IMPROVE_ENDURANCE("Improve endurance"),
    GENERAL_FITNESS("Improve general fitness"),
    LOSE_WEIGHT("Lose weight"),
    GAIN_WEIGHT("Gain weight"),
    IMPROVE_CONSISTENCY("Improve consistency"),
    HOME_FITNESS("Home fitness"),
}
