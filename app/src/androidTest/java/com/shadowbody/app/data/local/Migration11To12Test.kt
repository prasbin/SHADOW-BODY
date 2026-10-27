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
 * Migration v11 -> v12 regression: adds `skin_check_record` and
 * `outfit_daily_decision`. All Phase 1-11 data must survive, and the new
 * tables must accept/valid exactly per the exported v12 schema.
 */
@RunWith(AndroidJUnit4::class)
class Migration11To12Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion11(name: String) {
        val db = helper.createDatabase(name, 11)
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 999)")
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, aggressionLevel, updatedAt) VALUES " +
                "(1, 30, 180.0, 82.0, 'INTERMEDIATE', 'DUMBBELLS', 'BUILD_MUSCLE', '1,2,3,4', 45, 3, 5555)",
        )
        db.execSQL(
            "INSERT INTO exercise (id, name, muscleGroup, category, equipment, description, " +
                "instructions, difficulty, isActive, isSeeded) VALUES " +
                "(1, 'Bench Press', 'CHEST', 'PUSH', 'BARBELL', 'desc', 'steps', 'INTERMEDIATE', 1, 1)",
        )
        db.execSQL(
            "INSERT INTO wardrobe_photo_combination (id, position, label, photoPath, notes, createdAt, updatedAt) VALUES " +
                "(1, 0, 'Gym Combo', '/private/photos/1.jpg', 'notes', 1000, 1000)",
        )
        db.close()
    }

    @Test
    fun migrate11To12_preservesDataAndCreatesNewTables() {
        val name = "migration-test-11-12.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion11(name)

        val db = helper.runMigrationsAndValidate(name, 12, true, Migrations.MIGRATION_11_12)

        // Prior data survives.
        db.query("SELECT sessionMinutes, aggressionLevel FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(45, it.getInt(0))
            assertEquals(3, it.getInt(1))
        }
        db.query("SELECT name FROM exercise WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("Bench Press", it.getString(0))
        }
        db.query("SELECT label, photoPath FROM wardrobe_photo_combination WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("Gym Combo", it.getString(0))
            assertEquals("/private/photos/1.jpg", it.getString(1))
        }

        // New tables exist and are empty.
        db.query("SELECT COUNT(*) FROM skin_check_record").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM outfit_daily_decision").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // skin_check_record: nullable cheek paths accept NULL, dayKey unique.
        db.execSQL(
            "INSERT INTO skin_check_record (id, dayKey, notes, recordedAt, updatedAt) VALUES " +
                "(1, '2026-01-01', '', 1000, 1000)",
        )
        db.query("SELECT leftCheekPhotoPath, rightCheekCaptureMethod FROM skin_check_record WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertTrue(it.isNull(0))
            assertTrue(it.isNull(1))
        }
        db.execSQL(
            "INSERT INTO skin_check_record (id, dayKey, leftCheekPhotoPath, leftCheekCaptureMethod, " +
                "rightCheekPhotoPath, rightCheekCaptureMethod, notes, recordedAt, updatedAt) VALUES " +
                "(2, '2026-01-02', '/p/l.jpg', 'CAMERA', '/p/r.jpg', 'GALLERY', 'note', 1000, 1000)",
        )
        var duplicateRejected = false
        try {
            db.execSQL(
                "INSERT INTO skin_check_record (id, dayKey, notes, recordedAt, updatedAt) VALUES " +
                    "(3, '2026-01-02', '', 1000, 1000)",
            )
        } catch (_: Exception) {
            duplicateRejected = true
        }
        assertTrue("Unique dayKey must reject duplicate daily records", duplicateRejected)

        // Empty gallery-file copy columns behave.
        var missingNotesRejected = false
        try {
            db.execSQL(
                "INSERT INTO skin_check_record (id, dayKey, recordedAt, updatedAt) VALUES (4, '2026-01-03', 1000, 1000)",
            )
        } catch (_: Exception) {
            missingNotesRejected = true
        }
        assertTrue("NOT NULL notes must be enforced", missingNotesRejected)

        // outfit_daily_decision: full insert + unique dayKey.
        db.execSQL(
            "INSERT INTO outfit_daily_decision (id, dayKey, selectedCombinationId, candidateIds, occasion, explanation, decidedAt, updatedAt) VALUES " +
                "(1, '2026-01-02', 1, '1,2', 'TRAINING', 'Rule-based pick.', 1000, 1000)",
        )
        var duplicateDecisionRejected = false
        try {
            db.execSQL(
                "INSERT INTO outfit_daily_decision (id, dayKey, selectedCombinationId, candidateIds, occasion, explanation, decidedAt, updatedAt) VALUES " +
                    "(2, '2026-01-02', 1, '1', 'TRAINING', 'Again', 1000, 1000)",
            )
        } catch (_: Exception) {
            duplicateDecisionRejected = true
        }
        assertTrue("Unique dayKey must reject a second decision per day", duplicateDecisionRejected)

        db.query(
            "SELECT selectedCombinationId, candidateIds, occasion FROM outfit_daily_decision WHERE id = 1",
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getLong(0))
            assertEquals("1,2", it.getString(1))
            assertEquals("TRAINING", it.getString(2))
        }

        db.close()
    }
}
