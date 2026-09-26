package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.SessionStatus

/**
 * Phase 3: one performed workout. [planId] is nullable with SET_NULL so
 * deleting a plan never destroys recorded history.
 */
@Entity(
    tableName = "workout_session",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["startedAt"]), Index(value = ["status"])],
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long? = null,
    val name: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
)
