package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 2: one body-baseline snapshot. Every field except the timestamp is
 * optional so users record only what they want. Rows accumulate over time;
 * transformation history (Phase 7+) reads this table. Values are exactly
 * what the user entered — the app never adjusts or "corrects" them.
 */
@Entity(tableName = "baseline_record")
data class BaselineRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordedAt: Long = System.currentTimeMillis(),
    val weightKg: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val bicepsCm: Double? = null,
    val thighCm: Double? = null,
    /** User-reported value only; the app cannot verify its accuracy. */
    val bodyFatPct: Double? = null,
    val notes: String? = null,
)
