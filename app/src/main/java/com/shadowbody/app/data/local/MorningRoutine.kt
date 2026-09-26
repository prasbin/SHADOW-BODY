package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 5: a morning activation routine — an ordered list of short steps the
 * user runs at the start of the day.
 *
 * [seedKey] is a stable idempotency key for the built-in routine and is null for
 * routines the user creates. The unique index on it is what makes seeding
 * duplicate-safe across fresh installs, migrations and repeated app starts
 * (SQLite allows any number of NULLs in a unique index).
 *
 * Deleting a routine never destroys recorded runs: the log keeps its own
 * snapshot and nulls the reference.
 */
@Entity(
    tableName = "morning_routine",
    indices = [
        Index(value = ["seedKey"], unique = true),
        Index(value = ["sortOrder"]),
    ],
)
data class MorningRoutine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seedKey: String? = null,
    val name: String,
    val description: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
