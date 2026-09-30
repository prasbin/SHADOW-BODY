package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 8: a grooming routine — an ordered list of steps the user runs.
 *
 * [seedKey] is a stable idempotency key for built-in routines and is null for
 * routines the user creates. The unique index on it makes seeding duplicate-safe.
 *
 * Deleting a routine never destroys recorded logs: the log keeps its own
 * snapshot and nulls the reference.
 */
@Entity(
    tableName = "grooming_routine",
    indices = [
        Index(value = ["seedKey"], unique = true),
        Index(value = ["sortOrder"]),
    ],
)
data class GroomingRoutine(
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