package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.model.MorningStepOutcome

/**
 * Phase 5: the recorded outcome of one step inside a run.
 *
 * Written only when the user acts on a step (complete or skip), so a run that is
 * walked away from never claims progress. The title/category/target columns are
 * a snapshot: reordering, retiming or deleting a step later cannot change what
 * this run recorded.
 *
 * `(logId, position)` is unique, which makes re-marking a step idempotent
 * instead of duplicating it.
 */
@Entity(
    tableName = "morning_routine_step_log",
    foreignKeys = [
        ForeignKey(
            entity = MorningRoutineLog::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MorningRoutineStep::class,
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
data class MorningRoutineStepLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logId: Long,
    /** Null once the step is deleted; the snapshot below still describes it. */
    val stepId: Long? = null,
    val position: Int,
    val title: String,
    val category: MorningStepCategory,
    val targetDurationSec: Int? = null,
    val targetReps: Int? = null,
    val outcome: MorningStepOutcome,
    /** Seconds actually spent, when the step was timed. */
    val elapsedSec: Int? = null,
    val recordedAt: Long = System.currentTimeMillis(),
)
