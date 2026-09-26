package com.shadowbody.app.data.local

import androidx.room.TypeConverter
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal

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
}
