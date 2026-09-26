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
class Migration5To6Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion5(name: String) {
        val db = helper.createDatabase(name, 5)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 29, 175.0, 79.0, 'BEGINNER', 'BODYWEIGHT', 'LOSE_FAT', '1,2,3', 40, 4242)",
        )
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 77)")
        db.close()
    }

    @Test
    fun migrate5To6_preservesPhase5DataAndCreatesNutritionTables() {
        val name = "migration-test-5-6.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion5(name)

        val db = helper.runMigrationsAndValidate(name, 6, true, Migrations.MIGRATION_5_6)

        // Verify Phase 1-5 data survives
        db.query("SELECT sessionMinutes FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(40, it.getInt(0))
        }

        // Verify nutrition tables exist and start empty
        listOf("nutrition_goal", "food_log", "hydration_log").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                assertEquals("table $table must start empty", 0, it.getInt(0))
            }
        }
        db.close()
    }
}
