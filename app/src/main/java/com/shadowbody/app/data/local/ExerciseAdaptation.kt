package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.ProgressionState

/**
 * Phase 4: the adaptive engine's current view of one exercise.
 *
 * Stores the target that will be recommended next, the evidence behind it
 * (successful sessions at the current target, plus the most recent result),
 * and the reason code/text of the last adjustment so the UI can always show
 * why a target is what it is. One row per exercise.
 *
 * The exercise FK is RESTRICT: history rows are never orphaned by a library
 * change.
 */
@Entity(
    tableName = "exercise_adaptation",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["exerciseId"], unique = true)],
)
data class ExerciseAdaptation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    /** Next recommended volume. */
    val currentSets: Int,
    /** Null when the target is time-based. */
    val currentReps: Int? = null,
    val currentDurationSec: Int? = null,
    val restSec: Int,
    val state: ProgressionState,
    /** Clean sessions completed at [currentSets]/[currentReps]. */
    val sessionsAtTarget: Int = 0,
    // --- Last recorded result, so evidence survives app restarts ---
    val lastCompletedSets: Int? = null,
    val lastTargetSets: Int? = null,
    val lastActualReps: Int? = null,
    val lastTargetReps: Int? = null,
    val lastActualDurationSec: Int? = null,
    val lastTargetDurationSec: Int? = null,
    val lastReasonCode: String,
    val lastReasonText: String,
    val lastAdjustmentAt: Long,
    val updatedAt: Long,
)
