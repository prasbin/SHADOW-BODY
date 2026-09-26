package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 4: one exercise inside a [WorkoutRecommendation], with the reason the
 * engine gave for its target. (recommendationId, position) is unique so the
 * displayed order is always deterministic.
 */
@Entity(
    tableName = "recommended_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRecommendation::class,
            parentColumns = ["id"],
            childColumns = ["recommendationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["recommendationId", "position"], unique = true)],
)
data class RecommendedExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recommendationId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val restSec: Int,
    val reasonCode: String,
    val reasonText: String,
)
