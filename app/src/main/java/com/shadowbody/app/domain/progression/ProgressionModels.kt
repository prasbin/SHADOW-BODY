package com.shadowbody.app.domain.progression

import com.shadowbody.app.data.local.XpTransaction

/**
 * Phase 7 XP source types.
 * Each has a fixed XP reward value and attribute mappings.
 */
enum class XpSource(val xpReward: Int, val displayName: String) {
    WORKOUT(50, "Workout"),
    MORNING_ACTIVATION(30, "Morning Activation"),
    MEAL(10, "Meal"),
    HYDRATION(5, "Hydration"),
}

/**
 * Phase 7 attributes.
 */
enum class AttributeType(val displayName: String) {
    STRENGTH("Strength"),
    ENDURANCE("Endurance"),
    DISCIPLINE("Discipline"),
    RECOVERY("Recovery"),
    NUTRITION("Nutrition"),
}

/**
 * Phase 7 achievement definitions.
 */
enum class AchievementDef(
    val id: String,
    val displayName: String,
    val description: String,
    val target: Int,
) {
    FIRST_STEP(
        "first_step",
        "First Step",
        "Complete your first qualifying activity",
        1,
    ),
    WORKOUT_INITIATE(
        "workout_initiate",
        "Workout Initiate",
        "Complete your first workout",
        1,
    ),
    MORNING_AWAKENED(
        "morning_awakened",
        "Morning Awakened",
        "Complete your first Morning Activation",
        1,
    ),
    NUTRITION_LOGGED(
        "nutrition_logged",
        "Nutrition Logged",
        "Log your first qualifying meal",
        1,
    ),
    HYDRATION_HABIT(
        "hydration_habit",
        "Hydration Habit",
        "Log your first qualifying hydration",
        1,
    ),
    WEEK_WARRIOR(
        "week_warrior",
        "Week Warrior",
        "7 consecutive qualifying days",
        7,
    ),
    XP_100(
        "xp_100",
        "XP 100",
        "Reach 100 total XP",
        100,
    ),
}

/**
 * Result of an XP award attempt.
 */
sealed interface XpAwardResult {
    data class Awarded(val transaction: XpTransaction) : XpAwardResult
    data class Duplicate(val existing: XpTransaction) : XpAwardResult
    data object InvalidSource : XpAwardResult
}