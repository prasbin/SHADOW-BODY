package com.shadowbody.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration7To8Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion7(name: String) {
        val db = helper.createDatabase(name, 7)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 29, 175.0, 79.0, 'BEGINNER', 'BODYWEIGHT', 'LOSE_FAT', '1,2,3', 40, 4242)",
        )
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 77)")
        db.close()
    }

    @Test
    fun migrate7To8_preservesPhase7DataAndCreatesGroomingTables() {
        val name = "migration-test-7-8.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion7(name)

        val db = helper.runMigrationsAndValidate(name, 8, true, Migrations.MIGRATION_7_8)

        // Verify Phase 1-7 data survives
        db.query("SELECT sessionMinutes FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(40, it.getInt(0))
        }

        // Verify grooming tables exist
        listOf(
            "grooming_preferences",
            "grooming_routine",
            "grooming_routine_step",
            "grooming_log",
            "grooming_step_log",
        ).forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                val count = it.getInt(0)
                if (table == "grooming_preferences") {
                    assertEquals(1, count) // seeded with default prefs
                } else if (table == "grooming_routine") {
                    assertEquals(1, count) // seeded with daily-essentials routine
                } else if (table == "grooming_routine_step") {
                    assertTrue(count >= 8) // seeded with 8 steps
                } else {
                    assertEquals(0, count) // logs start empty
                }
            }
        }

        // Verify unique indexes
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_grooming_routine_seedKey'").use {
            assertTrue(it.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_grooming_routine_step_routineId_position'").use {
            assertTrue(it.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_grooming_log_routineId_dayKey_attempt'").use {
            assertTrue(it.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_grooming_step_log_logId_position'").use {
            assertTrue(it.moveToFirst())
        }

        db.close()
    }
}