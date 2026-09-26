package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 6 nutrition goal configuration (daily targets).
 * Single row active configuration (id = 1).
 */
@Entity(tableName = "nutrition_goal")
data class NutritionGoal(
    @PrimaryKey
    val id: Long = 1L,
    val calorieTarget: Int,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int,
    val hydrationMlTarget: Int,
    val goalType: String, // e.g. "MAINTENANCE", "BULK", "CUT"
    val updatedAt: Long,
)
