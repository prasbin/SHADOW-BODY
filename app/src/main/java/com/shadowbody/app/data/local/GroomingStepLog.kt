package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 8: one recorded step outcome inside a grooming run.
 *
 * Immutable once written. The snapshot captures what was on screen at the time,
 * so editing the routine later cannot rewrite recorded history.
 */
@Entity(
    tableName = "grooming_step_log",
    foreignKeys = [
        ForeignKey(
            entity = GroomingLog::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GroomingRoutineStep::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["logId", "position"], unique = true),
        Index(value = ["stepId"]),
    ],
)
data class GroomingStepLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logId: Long,
    val stepId: Long? = null,
    val position: Int,
    val title: String,
    val category: String, // GroomingStepCategory name
    val targetDurationSec: Int? = null,
    val outcome: String, // COMPLETED, SKIPPED
    val elapsedSec: Int? = null,
    val recordedAt: Long,
)