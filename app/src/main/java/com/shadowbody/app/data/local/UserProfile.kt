package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal

/**
 * Phase 2: single-row user profile (`id` is always 1).
 *
 * Only training-relevant data is stored: no address, contact, or identity
 * information. Training days use ISO numbers (1 = Monday .. 7 = Sunday).
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val fitnessLevel: FitnessLevel,
    val equipment: Set<Equipment>,
    val goals: Set<Goal>,
    /** ISO day numbers 1..7 on which the user wants to train. */
    val trainingDays: Set<Int>,
    /** Planned minutes per session; one of [ALLOWED_SESSION_MINUTES]. */
    val sessionMinutes: Int,
    /** User's preferred intensity/aggression level (1-5, where 5 is most aggressive). */
    val aggressionLevel: Int = 3,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        val ALLOWED_SESSION_MINUTES = setOf(15, 30, 45, 60)

        val DAY_LABELS = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun",
        )
    }
}
