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
 * Migration v4 -> v5: Phase 5 is purely additive.
 *
 * Every Phase 1-4 row must survive untouched, the four new morning tables must
 * appear with no invented history, and the built-in routine must be seeded
 * exactly once — the same routine a fresh install gets.
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion4(name: String) {
        var db = helper.createDatabase(name, 4)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 29, 175.0, 79.0, 'BEGINNER', 'BODYWEIGHT', 'LOSE_FAT', '1,2,3', 40, 4242)",
        )
        db.execSQL("INSERT INTO baseline_record (recordedAt, weightKg) VALUES (1000, 79.0)")
        db.execSQL("INSERT INTO schema_anchor (id, created_at) VALUES (1, 77)")
        db.execSQL(
            "INSERT INTO exercise (name, muscleGroup, category, equipment, description, " +
                "instructions, difficulty, isSeeded, isActive) VALUES " +
                "('Push-Up', 'CHEST', 'BODYWEIGHT', 'BODYWEIGHT', 'd', 'i', " +
                "'BEGINNER', 1, 1)",
        )
        val exerciseId = db.query("SELECT id FROM exercise WHERE name = 'Push-Up'").use {
            it.moveToFirst()
            it.getLong(0)
        }
        db.execSQL(
            "INSERT INTO workout_plan (name, description, targetDurationMin, isActive, " +
                "createdAt, updatedAt) VALUES ('Alpha', 'plan', 45, 1, 10, 10)",
        )
        val planId = db.query("SELECT id FROM workout_plan WHERE name = 'Alpha'").use {
            it.moveToFirst()
            it.getLong(0)
        }
        db.execSQL(
            "INSERT INTO workout_session (planId, name, status, startedAt, endedAt) " +
                "VALUES ($planId, 'Alpha', 'COMPLETED', 100, 200)",
        )
        val sessionId = db.query("SELECT id FROM workout_session").use {
            it.moveToFirst()
            it.getLong(0)
        }
        db.execSQL(
            "INSERT INTO session_exercise (sessionId, exerciseId, position, isCompleted) " +
                "VALUES ($sessionId, $exerciseId, 0, 1)",
        )
        db.execSQL(
            "INSERT INTO readiness_report (recordedAt, fatigue, soreness, notes) " +
                "VALUES (500, 2, 1, 'ok')",
        )
        db.execSQL(
            "INSERT INTO adaptation_checkpoint (id, lastAppliedSessionId, updatedAt) VALUES (1, $sessionId, 700)",
        )
        db.close()
    }

    @Test
    fun migrate4To5_preservesPhase4DataAndSeedsTheBuiltInRoutine() {
        val name = "migration-test-4-5.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion4(name)

        val db = helper.runMigrationsAndValidate(name, 5, true, Migrations.MIGRATION_4_5)

        // --- Phase 1-4 data is untouched ---
        db.query("SELECT sessionMinutes, weightKg FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(40, it.getInt(0))
            assertEquals(79.0, it.getDouble(1), 0.001)
        }
        db.query("SELECT created_at FROM schema_anchor WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(77L, it.getLong(0))
        }
        db.query("SELECT COUNT(*) FROM session_exercise WHERE isCompleted = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT lastAppliedSessionId FROM adaptation_checkpoint WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(1L, it.getLong(0))
        }

        // --- New history tables start empty: nothing is invented ---
        listOf("morning_routine_log", "morning_routine_step_log").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                assertEquals("table $table must start empty", 0, it.getInt(0))
            }
        }

        // --- The built-in routine is seeded exactly once ---
        db.query("SELECT COUNT(*) FROM morning_routine").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT seedKey, isActive FROM morning_routine").use {
            assertTrue(it.moveToFirst())
            assertEquals(MorningRoutineSeeds.DEFAULT_SEED_KEY, it.getString(0))
            assertEquals(1, it.getInt(1))
        }
        db.query("SELECT COUNT(*) FROM morning_routine_step").use {
            assertTrue(it.moveToFirst())
            assertEquals(MorningRoutineSeeds.STEPS.size, it.getInt(0))
        }
        db.query(
            "SELECT COUNT(*) FROM morning_routine_step WHERE isEnabled = 0 OR isSeeded = 0",
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
        // Positions are 0..n-1 with no gaps and no duplicates.
        db.query("SELECT MIN(position), MAX(position), COUNT(DISTINCT position) FROM morning_routine_step")
            .use {
                assertTrue(it.moveToFirst())
                val min = it.getInt(0)
                val max = it.getInt(1)
                val distinct = it.getInt(2)
                val expected = MorningRoutineSeeds.STEPS.size
                assertEquals(0, min)
                assertEquals(expected - 1, max)
                assertEquals(expected, distinct)
            }
        db.close()
    }

    @Test
    fun migrate4To5_seedingIsIdempotentAcrossRepeatedRuns() {
        val name = "migration-test-4-5-idempotent.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion4(name)

        var db = helper.runMigrationsAndValidate(name, 5, true, Migrations.MIGRATION_4_5)
        db.close()

        // Reopening at the current version must not re-run the migration.
        db = helper.runMigrationsAndValidate(name, 5, true)
        db.query("SELECT COUNT(*) FROM morning_routine").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM morning_routine_step").use {
            assertTrue(it.moveToFirst())
            assertEquals(MorningRoutineSeeds.STEPS.size, it.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate4To5_newTablesEnforceTheirConstraints() {
        val name = "migration-test-4-5-constraints.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion4(name)

        val db = helper.runMigrationsAndValidate(name, 5, true, Migrations.MIGRATION_4_5)
        val routineId = db.query("SELECT id FROM morning_routine").use {
            it.moveToFirst()
            it.getLong(0)
        }
        db.execSQL(
            "INSERT INTO morning_routine_log (routineId, routineName, dayKey, attempt, " +
                "startedAt, status, completedSteps, skippedSteps, totalSteps, notes) " +
                "VALUES ($routineId, 'MORNING', '2026-09-26', 1, 100, 'IN_PROGRESS', 0, 0, 3, '')",
        )
        val logId = db.query("SELECT id FROM morning_routine_log").use {
            it.moveToFirst()
            it.getLong(0)
        }
        db.execSQL(
            "INSERT INTO morning_routine_step_log (logId, position, title, category, " +
                "outcome, recordedAt) VALUES ($logId, 0, 'Water', 'HYDRATION', " +
                "'COMPLETED', 200)",
        )
        db.execSQL(
            "INSERT INTO morning_routine_step_log (logId, position, title, category, " +
                "outcome, recordedAt) VALUES ($logId, 1, 'Breathe', 'BREATHING', " +
                "'SKIPPED', 210)",
        )

        // One attempt per routine per day.
        try {
            db.execSQL(
                "INSERT INTO morning_routine_log (routineId, routineName, dayKey, attempt, " +
                    "startedAt, status, completedSteps, skippedSteps, totalSteps, notes) " +
                    "VALUES ($routineId, 'MORNING', '2026-09-26', 1, 300, 'ABANDONED', 0, 0, 3, '')",
            )
            throw AssertionError("a second attempt 1 for one day must be rejected")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected: unique(routineId, dayKey, attempt).
        }

        // One outcome per position inside a run.
        try {
            db.execSQL(
                "INSERT INTO morning_routine_step_log (logId, position, title, category, " +
                    "outcome, recordedAt) VALUES ($logId, 0, 'Water', 'HYDRATION', " +
                    "'SKIPPED', 400)",
            )
            throw AssertionError("a duplicate (logId, position) must be rejected")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected: unique(logId, position).
        }

        // The built-in routine cannot be seeded a second time.
        try {
            db.execSQL(
                "INSERT INTO morning_routine (seedKey, name, description, isActive, " +
                    "sortOrder, createdAt, updatedAt) VALUES " +
                    "('${MorningRoutineSeeds.DEFAULT_SEED_KEY}', 'DUPLICATE', '', 1, 0, 1, 1)",
            )
            throw AssertionError("a duplicate seedKey must be rejected")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected: unique index on seedKey.
        }
        db.close()
    }
}
