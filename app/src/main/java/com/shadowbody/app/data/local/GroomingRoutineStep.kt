package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 8: one step of a grooming routine.
 *
 * `(routineId, position)` is unique so ordering is deterministic and a
 * duplicate seed insert is ignored instead of duplicating a step. Deleting a
 * routine cascades to its steps; deleting a single step never touches recorded
 * logs, which keep their own snapshot.
 */
@Entity(
    tableName = "grooming_routine_step",
    foreignKeys = [
        ForeignKey(
            entity = GroomingRoutine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["routineId", "position"], unique = true),
        Index(value = ["routineId"]),
    ],
)
data class GroomingRoutineStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val title: String,
    val instructions: String,
    val category: String, // GroomingStepCategory name
    /** Optional duration target in seconds; null for steps without timed component. */
    val targetDurationSec: Int? = null,
    val position: Int,
    val isEnabled: Boolean = true,
    val isSeeded: Boolean = false,
)