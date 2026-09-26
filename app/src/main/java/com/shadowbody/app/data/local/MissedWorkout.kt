package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 4: a workout the user explicitly said they skipped.
 *
 * Missed sessions are recorded separately from [WorkoutSession] on purpose:
 * a miss must never touch completed history, and it can never be inferred
 * automatically — only the user records it.
 */
@Entity(
    tableName = "missed_workout",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["recordedAt"])],
)
data class MissedWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Plan that was skipped; null when the plan has since been deleted. */
    val planId: Long? = null,
    val recordedAt: Long,
    val reason: String = "",
)
