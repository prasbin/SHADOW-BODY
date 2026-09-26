package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.RecommendationStatus

/**
 * Phase 4: one generated workout recommendation.
 *
 * A recommendation is a *snapshot*: it stores the targets and the reason for
 * each target as they were at generation time, so later history changes never
 * rewrite what the user was actually told to do.
 */
@Entity(
    tableName = "workout_recommendation",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["adoptedPlanId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ReadinessReport::class,
            parentColumns = ["id"],
            childColumns = ["readinessReportId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = MissedWorkout::class,
            parentColumns = ["id"],
            childColumns = ["missedWorkoutId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["status"]),
    ],
)
data class WorkoutRecommendation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    /** Saved plan this was built from; null for a library-generated session. */
    val planId: Long? = null,
    /** Real plan created when the user adopts the recommendation. */
    val adoptedPlanId: Long? = null,
    /** Readiness report used while generating. */
    val readinessReportId: Long? = null,
    /** Most recent miss considered while generating. */
    val missedWorkoutId: Long? = null,
    val name: String,
    val estimatedMinutes: Int,
    /** Deterministic one-line explanation of the whole workout. */
    val summary: String,
    val status: RecommendationStatus,
)
