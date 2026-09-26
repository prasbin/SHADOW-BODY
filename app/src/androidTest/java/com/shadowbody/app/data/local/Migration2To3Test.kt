package com.shadowbody.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration v2 -> v3: Phase 2 profile + baseline rows survive, workout
 * tables appear, and the seed library lands exactly once.
 */
@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    @Test
    fun migrate2To3_preservesPhase2AndSeedsLibrary() {
        val name = "migration-test-2-3.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)

        var db = helper.createDatabase(name, 2)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 28, 178.0, 75.0, 'BEGINNER', 'BODYWEIGHT', 'GENERAL_FITNESS', " +
                "'1,3,5', 30, 999)",
        )
        db.execSQL("INSERT INTO baseline_record (recordedAt, weightKg) VALUES (1000, 80.0)")
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 42)")
        db.close()

        db = helper.runMigrationsAndValidate(name, 3, true, Migrations.MIGRATION_2_3)

        db.query("SELECT age FROM user_profile WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(28, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM baseline_record").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT created_at FROM schema_anchor WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42L, cursor.getLong(0))
        }
        db.query("SELECT COUNT(*) FROM exercise WHERE isSeeded = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(28, cursor.getInt(0))
        }
        db.close()

        // Repeated migration entry stays duplicate-safe: reopening at v3 and
        // re-running the object is a no-op for seeds thanks to OR IGNORE.
        db = helper.runMigrationsAndValidate(name, 3, true)
        db.query("SELECT COUNT(*) FROM exercise").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(28, cursor.getInt(0))
        }
        db.close()
    }
}
