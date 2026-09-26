package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 4: a single user-reported readiness check-in.
 *
 * Deliberately minimal: two 1..5 self-ratings and an optional note. No
 * biometrics, no health records, nothing the user did not type themselves.
 * Reports are append-only history; the engine reads the most recent one.
 */
@Entity(
    tableName = "readiness_report",
    indices = [Index(value = ["recordedAt"])],
)
data class ReadinessReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordedAt: Long,
    /** 1 = fresh, 5 = exhausted. */
    val fatigue: Int,
    /** 1 = none, 5 = severe. */
    val soreness: Int,
    val notes: String = "",
)
