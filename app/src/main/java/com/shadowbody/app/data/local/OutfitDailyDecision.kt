package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Feature C: the persisted decision for "which photographed outfit today".
 *
 * One row per calendar day (unique [dayKey]). Reuses the actual photographed
 * options already stored in `wardrobe_photo_combination` by id reference —
 * no duplicate photo model is created. [explanation] is generated from
 * deterministic, rule-based matching over the user's own labels/notes and the
 * chosen occasion; it always states that no image/pixel analysis happened.
 */
@Entity(
    tableName = "outfit_daily_decision",
    indices = [Index(value = ["dayKey"], unique = true)],
)
data class OutfitDailyDecision(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Local date in ISO format, e.g. "2026-10-27". */
    val dayKey: String,
    /** The `wardrobe_photo_combination.id` of the recommended outfit. */
    val selectedCombinationId: Long,
    /** Comma-separated ids of the options considered that day. */
    val candidateIds: String,
    /** Occasion key the user selected, e.g. "TRAINING". */
    val occasion: String,
    /** Honest, rule-based explanation text. */
    val explanation: String,
    val decidedAt: Long,
    val updatedAt: Long,
)
