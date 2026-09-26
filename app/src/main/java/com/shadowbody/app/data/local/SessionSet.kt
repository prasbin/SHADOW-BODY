package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 3: one performed set. Weight is optional so bodyweight work never
 * requires a weight. (sessionExerciseId, setNumber) is unique.
 */
@Entity(
    tableName = "session_set",
    foreignKeys = [
        ForeignKey(
            entity = SessionExercise::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionExerciseId", "setNumber"], unique = true)],
)
data class SessionSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val targetReps: Int? = null,
    val targetDurationSec: Int? = null,
    val actualReps: Int? = null,
    val actualDurationSec: Int? = null,
    val weightKg: Double? = null,
    val isCompleted: Boolean = false,
)
