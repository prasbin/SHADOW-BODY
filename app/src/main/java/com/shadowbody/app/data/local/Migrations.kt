package com.shadowbody.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit non-destructive migrations. Phase 1 data (schema_anchor) is
 * preserved by every migration — destructive fallback is never enabled.
 */
object Migrations {

    /** v1 -> v2: adds the Phase 2 profile + baseline tables. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`age` INTEGER NOT NULL, " +
                    "`heightCm` REAL NOT NULL, " +
                    "`weightKg` REAL NOT NULL, " +
                    "`fitnessLevel` TEXT NOT NULL, " +
                    "`equipment` TEXT NOT NULL, " +
                    "`goals` TEXT NOT NULL, " +
                    "`trainingDays` TEXT NOT NULL, " +
                    "`sessionMinutes` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `baseline_record` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recordedAt` INTEGER NOT NULL, " +
                    "`weightKg` REAL, " +
                    "`chestCm` REAL, " +
                    "`waistCm` REAL, " +
                    "`hipsCm` REAL, " +
                    "`bicepsCm` REAL, " +
                    "`thighCm` REAL, " +
                    "`bodyFatPct` REAL, " +
                    "`notes` TEXT)",
            )
        }
    }
}
