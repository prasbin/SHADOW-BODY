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
class Migration6To7Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion6(name: String) {
        val db = helper.createDatabase(name, 6)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 29, 175.0, 79.0, 'BEGINNER', 'BODYWEIGHT', 'LOSE_FAT', '1,2,3', 40, 4242)",
        )
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 77)")
        db.close()
    }

    @Test
    fun migrate6To7_preservesPhase6DataAndCreatesProgressionTables() {
        val name = "migration-test-6-7.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion6(name)

        val db = helper.runMigrationsAndValidate(name, 7, true, Migrations.MIGRATION_6_7)

        // Verify Phase 1-6 data survives
        db.query("SELECT sessionMinutes FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(40, it.getInt(0))
        }

        // Verify progression tables exist and start empty/initialized
        listOf("xp_transaction", "attribute", "streak", "achievement").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                // achievement table has 7 seeded rows
                val expectedCount = if (table == "achievement") 7 else 0
                assertEquals("table $table must have correct initial count", expectedCount, it.getInt(0))
            }
        }

        // Verify achievement definitions are seeded
        db.query("SELECT id, name, progressTarget FROM achievement ORDER BY id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("first_step", cursor.getString(0))
            assertEquals("First Step", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
        }

        // Verify unique index on xp_transaction(source, sourceRef)
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_xp_transaction_source_sourceRef'").use {
            assertTrue(it.moveToFirst())
        }

        db.close()
    }
}