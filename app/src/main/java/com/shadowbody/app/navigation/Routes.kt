package com.shadowbody.app.navigation

/** Single source of truth for navigation routes. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile_edit"
    const val BASELINE_HISTORY = "baseline_history"
    const val WORKOUT = "workout"
    const val PLAN_DETAIL = "plan_detail/{planId}"
    const val PLAN_EDITOR = "plan_editor?planId={planId}"
    const val ACTIVE_WORKOUT = "active/{sessionId}"
    const val WORKOUT_RESULT = "result/{sessionId}"
    const val ADAPTIVE = "adaptive"
    const val MORNING = "morning"
    const val MORNING_RUN = "morning_run/{logId}"
    const val MORNING_EDITOR = "morning_editor?routineId={routineId}"
    const val NUTRITION = "nutrition"
    const val NUTRITION_HISTORY = "nutrition_history"
    const val NUTRITION_GOAL = "nutrition_goal"
    const val PROGRESSION = "progression"
    const val PROGRESSION_HISTORY = "progression_history"

    /** Every route registered in the NavHost. Used by tests to catch typos. */
    val all: List<String> = listOf(
        DASHBOARD, SETTINGS, PROFILE, PROFILE_EDIT, BASELINE_HISTORY,
        WORKOUT, PLAN_DETAIL, PLAN_EDITOR, ACTIVE_WORKOUT, WORKOUT_RESULT,
        ADAPTIVE, MORNING, MORNING_RUN, MORNING_EDITOR, NUTRITION, NUTRITION_HISTORY, NUTRITION_GOAL,
        PROGRESSION, PROGRESSION_HISTORY,
    )

    const val START = DASHBOARD

    fun planDetail(planId: Long) = "plan_detail/$planId"
    fun planEditor(planId: Long? = null) =
        if (planId == null) "plan_editor" else "plan_editor?planId=$planId"
    fun activeWorkout(sessionId: Long) = "active/$sessionId"
    fun workoutResult(sessionId: Long) = "result/$sessionId"
    fun morningRun(logId: Long) = "morning_run/$logId"
    fun morningEditor(routineId: Long? = null) =
        if (routineId == null) "morning_editor" else "morning_editor?routineId=$routineId"
}
