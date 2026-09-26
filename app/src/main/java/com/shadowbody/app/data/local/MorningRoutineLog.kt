package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.MorningLogStatus

/**
 * Phase 5: one performed morning routine.
 *
 * History rules:
 * - [routineName], [completedSteps], [skippedSteps] and [totalSteps] are
 *   snapshots, so later routine edits never rewrite what actually happened.
 * - [routineId] is SET_NULL: deleting a routine keeps the record.
 * - `(routineId, dayKey, attempt)` is unique, and [attempt] lets a user retry
 *   after abandoning a run without ever creating a duplicate row for the same
 *   attempt. "One completion per routine per day" is enforced in the repository,
 *   where a second COMPLETED row is refused.
 */
@Entity(
    tableName = "morning_routine_log",
    foreignKeys = [
        ForeignKey(
            entity = MorningRoutine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["routineId", "dayKey", "attempt"], unique = true),
        Index(value = ["dayKey"]),
        Index(value = ["startedAt"]),
    ],
)
data class MorningRoutineLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long? = null,
    val routineName: String,
    /** Local calendar day in ISO form, e.g. `2026-09-26`. */
    val dayKey: String,
    val attempt: Int = 1,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: MorningLogStatus = MorningLogStatus.IN_PROGRESS,
    val completedSteps: Int = 0,
    val skippedSteps: Int = 0,
    val totalSteps: Int = 0,
    val notes: String = "",
)
