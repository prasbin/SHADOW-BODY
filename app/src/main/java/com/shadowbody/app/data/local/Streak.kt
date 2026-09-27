package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 7 streak tracking.
 * Single row (id = 1) with current and longest streaks.
 * Updated deterministically by the progression engine.
 */
@Entity(tableName = "streak")
data class Streak(
    @PrimaryKey
    val id: Long = 1L,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDayKey: String? = null,
    val updatedAt: Long = 0L,
)