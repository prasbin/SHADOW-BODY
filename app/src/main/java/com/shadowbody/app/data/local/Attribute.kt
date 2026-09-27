package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 7 player attributes.
 * Single row (id = 1) with current attribute values.
 * Updated deterministically by the progression engine.
 */
@Entity(tableName = "attribute")
data class Attribute(
    @PrimaryKey
    val id: Long = 1L,
    val strength: Int = 0,
    val endurance: Int = 0,
    val discipline: Int = 0,
    val recovery: Int = 0,
    val nutrition: Int = 0,
    val updatedAt: Long = 0L,
)