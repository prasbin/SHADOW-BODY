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
 * Migration v1 -> v2: Phase 1 data survives, new tables appear.
 * Schemas live in androidTest assets (copied from app/schemas).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesAnchorAndCreatesTables() {
        val name = "migration-test-1-2.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)

        var db = helper.createDatabase(name, 1)
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 12345)")
        db.close()

        db = helper.runMigrationsAndValidate(name, 2, true, Migrations.MIGRATION_1_2)

        db.query("SELECT id, created_at FROM schema_anchor").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(12345L, cursor.getLong(1))
        }
        // New tables accept writes.
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 28, 178.0, 75.0, 'BEGINNER', 'BODYWEIGHT', 'GENERAL_FITNESS', " +
                "'1,3,5', 30, 999)",
        )
        db.execSQL("INSERT INTO baseline_record (recordedAt, weightKg) VALUES (1000, 80.0)")
        db.query("SELECT COUNT(*) FROM user_profile").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }
}
