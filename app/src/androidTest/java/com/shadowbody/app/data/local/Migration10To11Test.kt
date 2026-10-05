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
class Migration10To11Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion10(name: String) {
        val db = helper.createDatabase(name, 10)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, aggressionLevel, updatedAt) VALUES " +
                "(1, 30, 180.0, 82.0, 'INTERMEDIATE', 'DUMBBELLS', 'BUILD_MUSCLE', '1,2,3,4', 45, 3, 5555)",
        )
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 999)")
        db.execSQL(
            "INSERT INTO wardrobe_item (id, name, category, clothingType, color, secondaryColor, style, season, occasion, fit, isEnabled, notes, photoPath, createdAt, updatedAt) VALUES " +
                "(1, 'Black T-Shirt', 'TOP', 'T_SHIRT', '#000000', '#FFFFFF', 'CASUAL', 'ALL_SEASON', 'GYM', 'REGULAR', 1, '', '/path.jpg', 1000, 1000)",
        )
        db.execSQL(
            "INSERT INTO outfit_record (id, name, topItemId, bottomItemId, footwearItemId, accessoryItemId, occasion, season, explanation, createdAt) VALUES " +
                "(1, 'Gym Outfit', 1, NULL, NULL, NULL, 'WORKOUT', 'ALL_SEASON', '', 2000)",
        )
        db.close()
    }

    @Test
    fun migrate10To11_preservesDataAndCreatesWardrobePhotoCombinationTable() {
        val name = "migration-test-10-11.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion10(name)

        val db = helper.runMigrationsAndValidate(name, 11, true, Migrations.MIGRATION_10_11)

        // Verify Phase 1-10 data preserved
        db.query("SELECT sessionMinutes, aggressionLevel FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(45, it.getInt(0))
            assertEquals(3, it.getInt(1))
        }

        db.query("SELECT name, category FROM wardrobe_item WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("Black T-Shirt", it.getString(0))
            assertEquals("TOP", it.getString(1))
        }

        db.query("SELECT name FROM outfit_record WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("Gym Outfit", it.getString(0))
        }

        // Verify wardrobe_photo_combination table exists with correct schema
        db.query("SELECT COUNT(*) FROM wardrobe_photo_combination").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // Verify column types and constraints by inserting test data
        db.execSQL(
            "INSERT INTO wardrobe_photo_combination (position, label, photoPath, notes, createdAt, updatedAt) VALUES " +
                "(0, 'Test Combo', '/path/to/photo.jpg', 'Test notes', 1234567890, 1234567890)",
        )

        db.query("SELECT position, label, photoPath, notes, createdAt, updatedAt FROM wardrobe_photo_combination WHERE position = 0").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
            assertEquals("Test Combo", it.getString(1))
            assertEquals("/path/to/photo.jpg", it.getString(2))
            assertEquals("Test notes", it.getString(3))
            assertEquals(1234567890L, it.getLong(4))
            assertEquals(1234567890L, it.getLong(5))
        }

        // Verify nullable photoPath accepts NULL
        db.execSQL(
            "INSERT INTO wardrobe_photo_combination (position, label, photoPath, notes, createdAt, updatedAt) VALUES " +
                "(1, 'No Photo', NULL, '', 1234567891, 1234567891)",
        )
        db.query("SELECT photoPath FROM wardrobe_photo_combination WHERE position = 1").use {
            assertTrue(it.moveToFirst())
            assertTrue(it.isNull(0))
        }

        // Verify NOT NULL constraints enforced
        var constraintFailed = false
        try {
            db.execSQL("INSERT INTO wardrobe_photo_combination (position, label) VALUES (2, 'Incomplete')")
        } catch (e: Exception) {
            constraintFailed = true
        }
        assertTrue("NOT NULL constraint should prevent insert without required columns", constraintFailed)

        db.close()
    }
}