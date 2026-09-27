package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 7 immutable XP transaction.
 * Each transaction records a single XP award with its source and reference.
 * Duplicate protection uses the unique [sourceRef] index.
 */
@Entity(
    tableName = "xp_transaction",
    indices = [
        Index("dayKey"),
        Index("loggedAt"),
        Index(value = ["source", "sourceRef"], unique = true),
    ],
)
data class XpTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val xpAmount: Int,
    val source: String, // WORKOUT, MORNING_ACTIVATION, MEAL, HYDRATION
    val sourceRef: String, // unique reference per source (e.g., sessionId, logId, foodLogId)
    val dayKey: String, // YYYY-MM-DD
    val reason: String,
    val loggedAt: Long,
)