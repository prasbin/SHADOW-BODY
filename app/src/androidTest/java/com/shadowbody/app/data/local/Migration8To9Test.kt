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
class Migration8To9Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion8(name: String) {
        val db = helper.createDatabase(name, 8)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 29, 175.0, 79.0, 'BEGINNER', 'BODYWEIGHT', 'LOSE_FAT', '1,2,3', 40, 4242)",
        )
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 77)")
        db.execSQL(
            "INSERT INTO grooming_preferences (id, routineFrequencyDays, updatedAt) VALUES (1, 1, 100)",
        )
        db.close()
    }

    @Test
    fun migrate8To9_preservesPhase8DataAndCreatesWardrobeTables() {
        val name = "migration-test-8-9.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion8(name)

        val db = helper.runMigrationsAndValidate(name, 9, true, Migrations.MIGRATION_8_9)

        db.query("SELECT sessionMinutes FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(40, it.getInt(0))
        }

        db.query("SELECT routineFrequencyDays FROM grooming_preferences WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }

        listOf("wardrobe_item", "outfit_record").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
        }

        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_wardrobe_item_category'").use {
            assertTrue(it.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_outfit_record_createdAt'").use {
            assertTrue(it.moveToFirst())
        }

        db.close()
    }
}
