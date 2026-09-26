package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 4: single-row bookkeeping for which completed sessions have already
 * been folded into adaptation state (`id` is always 1).
 *
 * This is what makes adaptation idempotent: replaying the same completed
 * sessions can never double-count a session, and completed sessions are never
 * rewritten — only new ones are read.
 */
@Entity(tableName = "adaptation_checkpoint")
data class AdaptationCheckpoint(
    @PrimaryKey
    val id: Int = 1,
    val lastAppliedSessionId: Long = 0,
    val updatedAt: Long = 0,
)
