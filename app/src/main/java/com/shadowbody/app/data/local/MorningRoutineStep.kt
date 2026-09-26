package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.MorningStepCategory

/**
 * Phase 5: one step of a morning routine.
 *
 * `(routineId, position)` is unique so ordering is deterministic and a
 * duplicate seed insert is ignored instead of duplicating a step. Deleting a
 * routine cascades to its steps; deleting a single step never touches recorded
 * runs, which keep their own snapshot.
 */
@Entity(
    tableName = "morning_routine_step",
    foreignKeys = [
        ForeignKey(
            entity = MorningRoutine::class,
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
data class MorningRoutineStep(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val title: String,
    val instructions: String,
    val category: MorningStepCategory,
    /** Countdown target; null for counted and reminder steps. */
    val targetDurationSec: Int? = null,
    /** Repetition target; null for timed and reminder steps. */
    val targetReps: Int? = null,
    val position: Int,
    val isEnabled: Boolean = true,
    val isSeeded: Boolean = false,
)
