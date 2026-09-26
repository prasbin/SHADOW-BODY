package com.shadowbody.app.data.local

import com.shadowbody.app.domain.model.MorningStepCategory

/**
 * Phase 5 built-in morning routine.
 *
 * Twelve short, low-friction actions that take roughly ten minutes in total.
 * Content rules, on purpose:
 * - everyday, beginner-friendly movement only;
 * - no medical, diagnostic or preventive claims about any action;
 * - a stop note, because pain is information, not a challenge.
 *
 * The list is the single source of truth: [Migrations.MIGRATION_4_5] seeds the
 * very same rows from this object, so an upgraded database and a fresh install
 * always end up with the identical routine.
 */
object MorningRoutineSeeds {

    /** Stable idempotency key; the unique index on it prevents duplicates. */
    const val DEFAULT_SEED_KEY = "MORNING_ACTIVATION_V1"

    const val DEFAULT_NAME = "MORNING ACTIVATION"

    const val DEFAULT_DESCRIPTION =
        "A short, low-effort start to the day. Move gently, breathe, then get on with your plan."

    /** One seed step, without a routine id: used by both seeding paths. */
    data class SeedStep(
        val title: String,
        val instructions: String,
        val category: MorningStepCategory,
        val targetDurationSec: Int? = null,
        val targetReps: Int? = null,
    )

    val STEPS: List<SeedStep> = listOf(
        SeedStep(
            title = "Drink a glass of water",
            instructions = "Fill a glass and drink it slowly. A simple morning cue, nothing more.",
            category = MorningStepCategory.HYDRATION,
        ),
        SeedStep(
            title = "Slow breathing reset",
            instructions = "Inhale through the nose for 4 counts, exhale for 6. Keep it easy.",
            category = MorningStepCategory.BREATHING,
            targetDurationSec = 60,
        ),
        SeedStep(
            title = "Wall posture reset",
            instructions = "Stand with your back lightly against a wall. Lengthen up, relax the shoulders.",
            category = MorningStepCategory.POSTURE,
            targetDurationSec = 60,
        ),
        SeedStep(
            title = "Neck and shoulder rolls",
            instructions = "Roll the shoulders back slowly, then turn the head left and right. Never force it.",
            category = MorningStepCategory.MOBILITY,
            targetReps = 10,
        ),
        SeedStep(
            title = "Hip circles",
            instructions = "Stand tall and draw slow circles with one knee, then the other.",
            category = MorningStepCategory.JOINT_MOBILITY,
            targetReps = 8,
        ),
        SeedStep(
            title = "Leg swings",
            instructions = "Hold support and swing one leg forward and back, 10 each side.",
            category = MorningStepCategory.JOINT_MOBILITY,
            targetReps = 20,
        ),
        SeedStep(
            title = "Bodyweight squats",
            instructions = "Feet about shoulder width. Sit back and down, then stand. Stop while it is still controlled.",
            category = MorningStepCategory.ACTIVATION,
            targetReps = 12,
        ),
        SeedStep(
            title = "Incline push-ups",
            instructions = "Hands on a desk or step. Lower the chest slowly, then press away.",
            category = MorningStepCategory.ACTIVATION,
            targetReps = 8,
        ),
        SeedStep(
            title = "Marching in place",
            instructions = "March with relaxed arms and easy breathing, somewhere you can still talk.",
            category = MorningStepCategory.MOVEMENT,
            targetDurationSec = 120,
        ),
        SeedStep(
            title = "Easy walk",
            instructions = "Walk at a comfortable pace. Movement for its own sake, not a workout.",
            category = MorningStepCategory.MOVEMENT,
            targetDurationSec = 180,
        ),
        SeedStep(
            title = "Calf and quad stretch",
            instructions = "Hold a wall. Stretch each calf, then each quad, about 30 seconds per side.",
            category = MorningStepCategory.COOLDOWN,
            targetDurationSec = 120,
        ),
        SeedStep(
            title = "Closing breath",
            instructions = "Three slow breaths, longer out than in. Then get on with the day.",
            category = MorningStepCategory.COOLDOWN,
            targetDurationSec = 60,
        ),
    )

    /** The routine row as it should exist in storage. */
    fun routine(now: Long = 0L): MorningRoutine = MorningRoutine(
        seedKey = DEFAULT_SEED_KEY,
        name = DEFAULT_NAME,
        description = DEFAULT_DESCRIPTION,
        isActive = true,
        sortOrder = 0,
        createdAt = now,
        updatedAt = now,
    )

    /** The seed steps bound to [routineId], with normalized positions. */
    fun steps(routineId: Long, now: Long = 0L): List<MorningRoutineStep> =
        STEPS.mapIndexed { index, step ->
            MorningRoutineStep(
                routineId = routineId,
                title = step.title,
                instructions = step.instructions,
                category = step.category,
                targetDurationSec = step.targetDurationSec,
                targetReps = step.targetReps,
                position = index,
                isEnabled = true,
                isSeeded = true,
            )
        }
}
