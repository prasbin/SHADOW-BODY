package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 8: a recorded grooming run for a specific day.
 *
 * One open run per routine per day, resumed rather than duplicated.
 * Completed runs are immutable; editing the routine later cannot rewrite history.
 */
@Entity(
    tableName = "grooming_log",
    foreignKeys = [
        ForeignKey(
            entity = GroomingRoutine::class,
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
data class GroomingLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long? = null,
    val routineName: String,
    val dayKey: String, // YYYY-MM-DD
    val attempt: Int,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: String = "IN_PROGRESS", // IN_PROGRESS, COMPLETED, ABANDONED
    val completedSteps: Int = 0,
    val skippedSteps: Int = 0,
    val totalSteps: Int,
    val notes: String = "",
)