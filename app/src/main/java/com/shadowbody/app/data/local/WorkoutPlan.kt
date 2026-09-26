package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Phase 3: user workout plan header. Exercises live in [WorkoutPlanExercise]. */
@Entity(tableName = "workout_plan")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val targetDurationMin: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
