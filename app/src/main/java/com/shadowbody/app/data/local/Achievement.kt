package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 7 achievement record.
 * One row per achievement definition, tracking unlock state.
 * The [id] matches the achievement definition key.
 */
@Entity(tableName = "achievement")
data class Achievement(
    @PrimaryKey
    val id: String, // matches ProgressionEngine achievement key
    val name: String,
    val description: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1,
)