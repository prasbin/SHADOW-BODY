package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shadowbody.app.domain.model.Difficulty
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.MuscleGroup

/**
 * Phase 3: exercise library entry.
 *
 * Names are unique (duplicate-safe seeding relies on it). Seeded rows are
 * flagged via [isSeeded] and must not be deleted while referenced by
 * history — the repository refuses seeded deletes.
 */
@Entity(
    tableName = "exercise",
    indices = [Index(value = ["name"], unique = true)],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val category: ExerciseCategory,
    val equipment: Equipment,
    val description: String,
    val instructions: String,
    val difficulty: Difficulty,
    val isActive: Boolean = true,
    val isSeeded: Boolean = false,
)
