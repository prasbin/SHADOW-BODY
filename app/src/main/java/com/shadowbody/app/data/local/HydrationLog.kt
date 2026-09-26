package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 6 hydration record.
 */
@Entity(
    tableName = "hydration_log",
    indices = [
        Index("dayKey"),
        Index("loggedAt"),
    ],
)
data class HydrationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dayKey: String, // "YYYY-MM-DD"
    val amountMl: Int,
    val loggedAt: Long,
)
