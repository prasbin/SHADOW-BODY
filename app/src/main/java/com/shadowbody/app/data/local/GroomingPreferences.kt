package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 8: grooming preferences — single-row user configuration for the grooming module.
 * Persisted via DataStore-like pattern but in Room for consistency with other modules.
 */
@Entity(tableName = "grooming_preferences")
data class GroomingPreferences(
    @PrimaryKey
    val id: Long = 1,
    val routineFrequencyDays: Int = 1, // how often to run the routine (1 = daily)
    val preferredRoutineId: Long? = null, // currently selected routine
    val updatedAt: Long = System.currentTimeMillis(),
)