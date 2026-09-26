package com.shadowbody.app.data.local

import androidx.room.TypeConverter
import com.shadowbody.app.domain.model.Difficulty
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import com.shadowbody.app.domain.model.MuscleGroup
import com.shadowbody.app.domain.model.SessionStatus

/** CSV-based converters. Unknown tokens are dropped, never crash reads. */
class Converters {

    @TypeConverter
    fun fitnessLevelToString(level: FitnessLevel): String = level.name

    @TypeConverter
    fun stringToFitnessLevel(raw: String): FitnessLevel =
        runCatching { FitnessLevel.valueOf(raw) }.getOrDefault(FitnessLevel.BEGINNER)

    @TypeConverter
    fun equipmentSetToString(values: Set<Equipment>): String =
        values.joinToString(",") { it.name }

    @TypeConverter
    fun stringToEquipmentSet(raw: String): Set<Equipment> =
        raw.split(",")
            .mapNotNull { token -> runCatching { Equipment.valueOf(token) }.getOrNull() }
            .toSet()

    @TypeConverter
    fun goalSetToString(values: Set<Goal>): String =
        values.joinToString(",") { it.name }

    @TypeConverter
    fun stringToGoalSet(raw: String): Set<Goal> =
        raw.split(",")
            .mapNotNull { token -> runCatching { Goal.valueOf(token) }.getOrNull() }
            .toSet()

    @TypeConverter
    fun intSetToString(values: Set<Int>): String =
        values.joinToString(",") { it.toString() }

    @TypeConverter
    fun stringToIntSet(raw: String): Set<Int> =
        raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()

    // --- Workout engine enums (Phase 3). Same unknown-token policy. ---

    @TypeConverter
    fun muscleGroupToString(value: MuscleGroup): String = value.name

    @TypeConverter
    fun stringToMuscleGroup(raw: String): MuscleGroup =
        runCatching { MuscleGroup.valueOf(raw) }.getOrDefault(MuscleGroup.FULL_BODY)

    @TypeConverter
    fun categoryToString(value: ExerciseCategory): String = value.name

    @TypeConverter
    fun stringToCategory(raw: String): ExerciseCategory =
        runCatching { ExerciseCategory.valueOf(raw) }.getOrDefault(ExerciseCategory.BODYWEIGHT)

    @TypeConverter
    fun difficultyToString(value: Difficulty): String = value.name

    @TypeConverter
    fun stringToDifficulty(raw: String): Difficulty =
        runCatching { Difficulty.valueOf(raw) }.getOrDefault(Difficulty.BEGINNER)

    @TypeConverter
    fun equipmentToString(value: Equipment): String = value.name

    @TypeConverter
    fun stringToEquipment(raw: String): Equipment =
        runCatching { Equipment.valueOf(raw) }.getOrDefault(Equipment.BODYWEIGHT)

    @TypeConverter
    fun sessionStatusToString(value: SessionStatus): String = value.name

    @TypeConverter
    fun stringToSessionStatus(raw: String): SessionStatus =
        runCatching { SessionStatus.valueOf(raw) }.getOrDefault(SessionStatus.IN_PROGRESS)
}
