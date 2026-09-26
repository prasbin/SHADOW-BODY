package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 6 logged food item with macros and calories.
 */
@Entity(
    tableName = "food_log",
    indices = [
        Index("dayKey"),
        Index("loggedAt"),
    ],
)
data class FoodLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dayKey: String, // "YYYY-MM-DD"
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbGrams: Double,
    val fatGrams: Double,
    val servingText: String, // e.g. "1 bowl", "150g"
    val notes: String,
    val loggedAt: Long,
)
