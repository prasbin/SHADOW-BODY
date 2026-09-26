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
 * Migration v3 -> v4: Phase 4 is purely additive.
 *
 * Every Phase 1-3 row must survive untouched, the new adaptive tables must
 * appear empty (no invented history), and the schema anchor must not move.
 */
@RunWith(AndroidJUnit4::class)
class Migration3To4Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShadowBodyDatabase::class.java,
    )

    private fun seedVersion3(name: String): Long {
        var db = helper.createDatabase(name, 3)
        db.execSQL(
            "INSERT INTO user_profile (id, age, heightCm, weightKg, fitnessLevel, " +
                "equipment, goals, trainingDays, sessionMinutes, updatedAt) VALUES " +
                "(1, 31, 180.0, 82.5, 'INTERMEDIATE', 'BODYWEIGHT', 'BUILD_STRENGTH', " +
                "'1,3,5', 45, 4242)",
        )
        db.execSQL("INSERT INTO baseline_record (recordedAt, weightKg) VALUES (1000, 82.5)")
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
            "INSERT INTO workout_plan_exercise (planId, exerciseId, position, targetSets, " +
                "targetReps, restSec, notes) VALUES ($planId, $exerciseId, 0, 3, 12, 60, '')",
        )
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
        val sessionExerciseId = db.query("SELECT id FROM session_exercise").use {
            it.moveToFirst()
            it.getLong(0)
        }
        db.execSQL(
            "INSERT INTO session_set (sessionExerciseId, setNumber, targetReps, " +
                "actualReps, isCompleted) VALUES ($sessionExerciseId, 1, 12, 12, 1)",
        )
        db.close()
        return exerciseId
    }

    @Test
    fun migrate3To4_preservesPhase3DataAndAddsEmptyAdaptiveTables() {
        val name = "migration-test-3-4.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion3(name)

        val db = helper.runMigrationsAndValidate(name, 4, true, Migrations.MIGRATION_3_4)

        // --- Phase 1-3 data is byte-for-byte intact ---
        db.query("SELECT sessionMinutes, weightKg FROM user_profile WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(45, it.getInt(0))
            assertEquals(82.5, it.getDouble(1), 0.001)
        }
        db.query("SELECT COUNT(*) FROM baseline_record").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT created_at FROM schema_anchor WHERE id = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(77L, it.getLong(0))
        }
        db.query("SELECT targetReps FROM workout_plan_exercise").use {
            assertTrue(it.moveToFirst())
            assertEquals(12, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM session_set WHERE isCompleted = 1").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }

        // --- Phase 4 tables exist and start empty: nothing is invented ---
        listOf(
            "readiness_report",
            "exercise_adaptation",
            "missed_workout",
            "workout_recommendation",
            "recommended_exercise",
            "adaptation_checkpoint",
        ).forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                assertEquals("table $table must start empty", 0, it.getInt(0))
            }
        }
        db.close()
    }

    @Test
    fun migrate3To4_reopeningIsIdempotent() {
        val name = "migration-test-3-4-idempotent.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        seedVersion3(name)

        var db = helper.runMigrationsAndValidate(name, 4, true, Migrations.MIGRATION_3_4)
        db.close()

        // Reopening at the current version must not re-run anything.
        db = helper.runMigrationsAndValidate(name, 4, true)
        db.query("SELECT COUNT(*) FROM workout_plan").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM exercise_adaptation").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate3To4_newTablesAcceptRowsAndEnforceConstraints() {
        val name = "migration-test-3-4-constraints.db"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        val exerciseId = seedVersion3(name)

        val db = helper.runMigrationsAndValidate(name, 4, true, Migrations.MIGRATION_3_4)
        // Exercise id 1 is the seeded Push-Up row.
        val realId = db.query("SELECT id FROM exercise WHERE name = 'Push-Up'").use {
            it.moveToFirst()
            it.getLong(0)
        }
        assertTrue(realId > 0)

        db.execSQL(
            "INSERT INTO readiness_report (recordedAt, fatigue, soreness, notes) " +
                "VALUES (500, 3, 2, 'ok')",
        )
        db.execSQL(
            "INSERT INTO exercise_adaptation (exerciseId, currentSets, currentReps, " +
                "restSec, state, sessionsAtTarget, lastReasonCode, lastReasonText, " +
                "lastAdjustmentAt, updatedAt) VALUES " +
                "($realId, 3, 12, 60, 'MAINTAINING', 1, 'SINGLE_SUCCESS', 'held', 500, 500)",
        )
        db.execSQL(
            "INSERT INTO workout_recommendation (createdAt, name, estimatedMinutes, summary, " +
                "status) VALUES (600, 'ADAPTIVE · ALPHA', 40, 'held', 'ACTIVE')",
        )
        db.query("SELECT id FROM workout_recommendation").use {
            assertTrue(it.moveToFirst())
        }
        val recommendationId = db.query("SELECT id FROM workout_recommendation").use {
            it.moveToFirst()
            it.getLong(0)
        }
        db.execSQL(
            "INSERT INTO recommended_exercise (recommendationId, exerciseId, position, sets, " +
                "reps, restSec, reasonCode, reasonText) VALUES " +
                "($recommendationId, $realId, 0, 3, 12, 60, 'SINGLE_SUCCESS', 'held')",
        )
        db.execSQL(
            "INSERT INTO adaptation_checkpoint (id, lastAppliedSessionId, updatedAt) " +
                "VALUES (1, 1, 700)",
        )

        // One adaptation row per exercise is unique.
        try {
            db.execSQL(
                "INSERT INTO exercise_adaptation (exerciseId, currentSets, currentReps, " +
                    "restSec, state, sessionsAtTarget, lastReasonCode, lastReasonText, " +
                    "lastAdjustmentAt, updatedAt) VALUES " +
                    "($realId, 4, 12, 60, 'MAINTAINING', 0, 'STABLE', 'held', 800, 800)",
            )
            throw AssertionError("duplicate exercise_adaptation must be rejected")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected: unique index on exerciseId.
        }

        // (recommendationId, position) is unique too.
        try {
            db.execSQL(
                "INSERT INTO recommended_exercise (recommendationId, exerciseId, position, " +
                    "sets, reps, restSec, reasonCode, reasonText) VALUES " +
                    "($recommendationId, $realId, 0, 4, 12, 60, 'STABLE', 'held')",
            )
            throw AssertionError("duplicate recommended_exercise position must be rejected")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected: unique(recommendationId, position).
        }

        // Deleting a recommendation is left to Room's generated code, where
        // foreign keys are actually enforced; see Phase4SchemaTest. Here we
        // only prove the additive schema and its constraints.
        db.query("SELECT COUNT(*) FROM exercise_adaptation WHERE exerciseId = $realId").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        assertTrue(exerciseId > 0)
        db.close()
    }
}
