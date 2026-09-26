package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 3: one exercise slot inside a plan. Relational (never a blob).
 * The (planId, exerciseId) pair is unique: one slot per exercise per plan.
 */
@Entity(
    tableName = "workout_plan_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["planId", "position"]),
        Index(value = ["planId", "exerciseId"], unique = true),
    ],
)
data class WorkoutPlanExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val exerciseId: Long,
    /** Zero-based order inside the plan. */
    val position: Int,
    val targetSets: Int,
    /** Null when the slot is time-based (see [targetDurationSec]). */
    val targetReps: Int? = null,
    val targetDurationSec: Int? = null,
    val restSec: Int = 60,
    val notes: String = "",
)
