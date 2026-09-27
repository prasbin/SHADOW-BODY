package com.shadowbody.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit non-destructive migrations. Phase 1 data (schema_anchor) and
 * Phase 2 data (profile, baselines) are preserved by every migration —
 * destructive fallback is never enabled.
 */
object Migrations {

    /** v1 -> v2: adds the Phase 2 profile + baseline tables. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`age` INTEGER NOT NULL, " +
                    "`heightCm` REAL NOT NULL, " +
                    "`weightKg` REAL NOT NULL, " +
                    "`fitnessLevel` TEXT NOT NULL, " +
                    "`equipment` TEXT NOT NULL, " +
                    "`goals` TEXT NOT NULL, " +
                    "`trainingDays` TEXT NOT NULL, " +
                    "`sessionMinutes` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `baseline_record` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recordedAt` INTEGER NOT NULL, " +
                    "`weightKg` REAL, " +
                    "`chestCm` REAL, " +
                    "`waistCm` REAL, " +
                    "`hipsCm` REAL, " +
                    "`bicepsCm` REAL, " +
                    "`thighCm` REAL, " +
                    "`bodyFatPct` REAL, " +
                    "`notes` TEXT)",
            )
        }
    }

    /**
     * v2 -> v3: creates the Phase 3 workout tables and seeds the exercise
     * library for upgrading databases (fresh installs seed via
     * [ExerciseSeeds] in the onCreate callback instead).
     *
     * All seed inserts are OR IGNORE on the unique name index, so repeated
     * migration runs never duplicate rows.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `exercise` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`muscleGroup` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`equipment` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`instructions` TEXT NOT NULL, " +
                    "`difficulty` TEXT NOT NULL, " +
                    "`isActive` INTEGER NOT NULL, " +
                    "`isSeeded` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_name` " +
                    "ON `exercise` (`name`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `workout_plan` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`targetDurationMin` INTEGER, " +
                    "`isActive` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `workout_plan_exercise` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`planId` INTEGER NOT NULL, " +
                    "`exerciseId` INTEGER NOT NULL, " +
                    "`position` INTEGER NOT NULL, " +
                    "`targetSets` INTEGER NOT NULL, " +
                    "`targetReps` INTEGER, " +
                    "`targetDurationSec` INTEGER, " +
                    "`restSec` INTEGER NOT NULL, " +
                    "`notes` TEXT NOT NULL, " +
                    "FOREIGN KEY(`planId`) REFERENCES `workout_plan`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_plan_exercise_planId_position` " +
                    "ON `workout_plan_exercise` (`planId`, `position`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_workout_plan_exercise_planId_exerciseId` " +
                    "ON `workout_plan_exercise` (`planId`, `exerciseId`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `workout_session` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`planId` INTEGER, " +
                    "`name` TEXT NOT NULL, " +
                    "`startedAt` INTEGER NOT NULL, " +
                    "`endedAt` INTEGER, " +
                    "`status` TEXT NOT NULL, " +
                    "FOREIGN KEY(`planId`) REFERENCES `workout_plan`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_session_startedAt` ON `workout_session` (`startedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_session_status` ON `workout_session` (`status`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `session_exercise` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sessionId` INTEGER NOT NULL, " +
                    "`exerciseId` INTEGER NOT NULL, " +
                    "`position` INTEGER NOT NULL, " +
                    "`isCompleted` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`sessionId`) REFERENCES `workout_session`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_session_exercise_sessionId_position` " +
                    "ON `session_exercise` (`sessionId`, `position`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `session_set` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sessionExerciseId` INTEGER NOT NULL, " +
                    "`setNumber` INTEGER NOT NULL, " +
                    "`targetReps` INTEGER, " +
                    "`targetDurationSec` INTEGER, " +
                    "`actualReps` INTEGER, " +
                    "`actualDurationSec` INTEGER, " +
                    "`weightKg` REAL, " +
                    "`isCompleted` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercise`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_session_set_sessionExerciseId_setNumber` " +
                    "ON `session_set` (`sessionExerciseId`, `setNumber`)",
            )
            seedExercises(db)
        }

        private fun seed(db: SupportSQLiteDatabase, values: String) {
            db.execSQL(
                "INSERT OR IGNORE INTO `exercise` (`name`, `muscleGroup`, " +
                    "`category`, `equipment`, `description`, `instructions`, " +
                    "`difficulty`, `isActive`, `isSeeded`) VALUES $values",
            )
        }

        /** Mirrors [ExerciseSeeds.ALL]; keep both lists in sync. */
        private fun seedExercises(db: SupportSQLiteDatabase) {
            seed(db, "('Push-Up', 'CHEST', 'BODYWEIGHT', 'BODYWEIGHT', 'Classic chest and triceps press from the floor.', 'Hands under shoulders, body straight. Lower chest near floor, press up.', 'BEGINNER', 1, 1)")
            seed(db, "('Incline Push-Up', 'CHEST', 'BODYWEIGHT', 'BODYWEIGHT', 'Easier push-up variation using an elevated surface.', 'Hands on a bench or step. Keep body rigid, lower and press.', 'BEGINNER', 1, 1)")
            seed(db, "('Bodyweight Squat', 'LEGS', 'BODYWEIGHT', 'BODYWEIGHT', 'Foundational lower-body movement.', 'Feet shoulder width, sit hips back and down, knees track over toes, stand.', 'BEGINNER', 1, 1)")
            seed(db, "('Forward Lunge', 'LEGS', 'BODYWEIGHT', 'BODYWEIGHT', 'Single-leg strength and balance builder.', 'Step forward, lower until both knees near 90 degrees, push back.', 'BEGINNER', 1, 1)")
            seed(db, "('Glute Bridge', 'GLUTES', 'BODYWEIGHT', 'BODYWEIGHT', 'Glute and hip activation from the floor.', 'Back on floor, feet flat. Drive hips up, squeeze glutes, lower slowly.', 'BEGINNER', 1, 1)")
            seed(db, "('Calf Raise', 'LEGS', 'BODYWEIGHT', 'BODYWEIGHT', 'Standing calf strengthener.', 'Rise onto the balls of your feet, pause, lower with control.', 'BEGINNER', 1, 1)")
            seed(db, "('Plank', 'CORE', 'CORE', 'BODYWEIGHT', 'Timed core stability hold.', 'Forearms and toes support, body straight, brace core, breathe.', 'BEGINNER', 1, 1)")
            seed(db, "('Side Plank', 'CORE', 'CORE', 'BODYWEIGHT', 'Oblique and lateral core hold.', 'One forearm and feet stacked, hips lifted, hold each side.', 'INTERMEDIATE', 1, 1)")
            seed(db, "('Crunch', 'CORE', 'CORE', 'BODYWEIGHT', 'Basic upper-abdominal curl.', 'Back on floor, knees bent. Curl shoulders up slightly, lower slowly.', 'BEGINNER', 1, 1)")
            seed(db, "('Mountain Climber', 'FULL_BODY', 'CARDIO', 'BODYWEIGHT', 'Dynamic plank with alternating knee drives.', 'Plank position, drive knees toward chest alternately at a steady pace.', 'INTERMEDIATE', 1, 1)")
            seed(db, "('Burpee', 'FULL_BODY', 'CARDIO', 'BODYWEIGHT', 'Full-body conditioning movement.', 'Squat, jump feet back to plank, optional push-up, jump up with arms overhead.', 'ADVANCED', 1, 1)")
            seed(db, "('Jumping Jack', 'FULL_BODY', 'CARDIO', 'BODYWEIGHT', 'Simple warm-up and conditioning move.', 'Jump feet out while raising arms, return to start, keep rhythm.', 'BEGINNER', 1, 1)")
            seed(db, "('Superman', 'BACK', 'BODYWEIGHT', 'BODYWEIGHT', 'Lower-back and posterior chain lift.', 'Lie face down, lift chest and legs together, hold briefly, lower.', 'BEGINNER', 1, 1)")
            seed(db, "('Bird Dog', 'CORE', 'CORE', 'BODYWEIGHT', 'Contralateral core stability drill.', 'On all fours, extend opposite arm and leg, hold, switch sides.', 'BEGINNER', 1, 1)")
            seed(db, "('Wall Sit', 'LEGS', 'BODYWEIGHT', 'BODYWEIGHT', 'Timed isometric leg hold.', 'Back flat to wall, thighs parallel to floor, hold the position.', 'BEGINNER', 1, 1)")
            seed(db, "('High Knees', 'FULL_BODY', 'CARDIO', 'BODYWEIGHT', 'Running-in-place cardio drill.', 'Drive knees up toward hips alternately, pump arms, stay tall.', 'BEGINNER', 1, 1)")
            seed(db, "('Bench Dip', 'ARMS', 'BODYWEIGHT', 'BENCH', 'Triceps dip using a bench or sturdy chair.', 'Hands behind on edge, lower body by bending elbows, press up.', 'INTERMEDIATE', 1, 1)")
            seed(db, "('Dumbbell Row', 'BACK', 'STRENGTH', 'DUMBBELLS', 'Single-arm back pull.', 'Hinge at hips, pull dumbbell to ribs, squeeze back, lower slowly.', 'BEGINNER', 1, 1)")
            seed(db, "('Dumbbell Shoulder Press', 'SHOULDERS', 'STRENGTH', 'DUMBBELLS', 'Overhead press for shoulders and triceps.', 'Press dumbbells overhead from shoulders, avoid arching back, lower slowly.', 'INTERMEDIATE', 1, 1)")
            seed(db, "('Dumbbell Curl', 'ARMS', 'STRENGTH', 'DUMBBELLS', 'Biceps isolation curl.', 'Elbows pinned at sides, curl weights up, squeeze, lower fully.', 'BEGINNER', 1, 1)")
            seed(db, "('Dumbbell Triceps Extension', 'ARMS', 'STRENGTH', 'DUMBBELLS', 'Overhead triceps builder.', 'Weight behind head, extend arms overhead, keep elbows still, lower slowly.', 'INTERMEDIATE', 1, 1)")
            seed(db, "('Goblet Squat', 'LEGS', 'STRENGTH', 'DUMBBELLS', 'Weighted squat holding one dumbbell at the chest.', 'Hold weight at chest, squat deep with upright torso, drive up.', 'INTERMEDIATE', 1, 1)")
            seed(db, "('Dumbbell Romanian Deadlift', 'GLUTES', 'STRENGTH', 'DUMBBELLS', 'Hip-hinge for glutes and hamstrings.', 'Soft knees, push hips back lowering weights along thighs, stand tall.', 'INTERMEDIATE', 1, 1)")
            seed(db, "('Dumbbell Chest Press', 'CHEST', 'STRENGTH', 'DUMBBELLS', 'Lying chest press, floor or bench.', 'On back, press dumbbells from chest to full extension, lower slowly.', 'BEGINNER', 1, 1)")
            seed(db, "('Dumbbell Lateral Raise', 'SHOULDERS', 'STRENGTH', 'DUMBBELLS', 'Side-shoulder isolation.', 'Light weights, raise arms to shoulder height with soft elbows, lower slowly.', 'BEGINNER', 1, 1)")
            seed(db, "('Barbell Overhead Press', 'SHOULDERS', 'STRENGTH', 'BARBELL', 'Standing barbell press for full upper body.', 'Brace core, press bar overhead to lockout, lower to chin level.', 'ADVANCED', 1, 1)")
            seed(db, "('Pull-Up', 'BACK', 'BODYWEIGHT', 'PULL_UP_BAR', 'Classic upper-body pull from a bar.', 'Hang from bar, pull chest toward bar, lower fully with control.', 'ADVANCED', 1, 1)")
            seed(db, "('Resistance Band Pull-Apart', 'SHOULDERS', 'STRENGTH', 'RESISTANCE_BANDS', 'Upper-back and rear-shoulder band drill.', 'Hold band at shoulder height, pull apart until chest, return slowly.', 'BEGINNER', 1, 1)")
        }
    }

    /**
     * v3 -> v4: adds the Phase 4 adaptive tables.
     *
     * Purely additive: no existing table, column or row is touched, so
     * profiles, baselines, the exercise library, plans, plan slots and every
     * completed session survive untouched. Adaptation state starts empty on
     * purpose — an upgrade must never invent progress the user did not earn.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `readiness_report` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recordedAt` INTEGER NOT NULL, " +
                    "`fatigue` INTEGER NOT NULL, " +
                    "`soreness` INTEGER NOT NULL, " +
                    "`notes` TEXT NOT NULL)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_readiness_report_recordedAt` " +
                    "ON `readiness_report` (`recordedAt`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `exercise_adaptation` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`exerciseId` INTEGER NOT NULL, " +
                    "`currentSets` INTEGER NOT NULL, " +
                    "`currentReps` INTEGER, " +
                    "`currentDurationSec` INTEGER, " +
                    "`restSec` INTEGER NOT NULL, " +
                    "`state` TEXT NOT NULL, " +
                    "`sessionsAtTarget` INTEGER NOT NULL, " +
                    "`lastCompletedSets` INTEGER, " +
                    "`lastTargetSets` INTEGER, " +
                    "`lastActualReps` INTEGER, " +
                    "`lastTargetReps` INTEGER, " +
                    "`lastActualDurationSec` INTEGER, " +
                    "`lastTargetDurationSec` INTEGER, " +
                    "`lastReasonCode` TEXT NOT NULL, " +
                    "`lastReasonText` TEXT NOT NULL, " +
                    "`lastAdjustmentAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_adaptation_exerciseId` " +
                    "ON `exercise_adaptation` (`exerciseId`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `missed_workout` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`planId` INTEGER, " +
                    "`recordedAt` INTEGER NOT NULL, " +
                    "`reason` TEXT NOT NULL, " +
                    "FOREIGN KEY(`planId`) REFERENCES `workout_plan`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_missed_workout_recordedAt` " +
                    "ON `missed_workout` (`recordedAt`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `workout_recommendation` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`planId` INTEGER, " +
                    "`adoptedPlanId` INTEGER, " +
                    "`readinessReportId` INTEGER, " +
                    "`missedWorkoutId` INTEGER, " +
                    "`name` TEXT NOT NULL, " +
                    "`estimatedMinutes` INTEGER NOT NULL, " +
                    "`summary` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, " +
                    "FOREIGN KEY(`planId`) REFERENCES `workout_plan`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                    "FOREIGN KEY(`adoptedPlanId`) REFERENCES `workout_plan`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                    "FOREIGN KEY(`readinessReportId`) REFERENCES `readiness_report`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                    "FOREIGN KEY(`missedWorkoutId`) REFERENCES `missed_workout`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_recommendation_createdAt` " +
                    "ON `workout_recommendation` (`createdAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_recommendation_status` " +
                    "ON `workout_recommendation` (`status`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recommended_exercise` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recommendationId` INTEGER NOT NULL, " +
                    "`exerciseId` INTEGER NOT NULL, " +
                    "`position` INTEGER NOT NULL, " +
                    "`sets` INTEGER NOT NULL, " +
                    "`reps` INTEGER, " +
                    "`durationSec` INTEGER, " +
                    "`restSec` INTEGER NOT NULL, " +
                    "`reasonCode` TEXT NOT NULL, " +
                    "`reasonText` TEXT NOT NULL, " +
                    "FOREIGN KEY(`recommendationId`) " +
                    "REFERENCES `workout_recommendation`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_recommended_exercise_recommendationId_position` " +
                    "ON `recommended_exercise` (`recommendationId`, `position`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `adaptation_checkpoint` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`lastAppliedSessionId` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
        }
    }

    /**
     * v4 -> v5: creates the Phase 5 morning activation tables and seeds the
     * built-in routine.
     *
     * Purely additive: no Phase 1-4 table is touched, so profile, baselines,
     * workout history and adaptive history all survive an upgrade untouched.
     *
     * The seed is OR IGNORE on the unique `seedKey` index and the unique
     * `(routineId, position)` step index, so this migration is safe to run
     * repeatedly and converges on the same single routine a fresh install gets
     * from [MorningRoutineSeeds].
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `morning_routine` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`seedKey` TEXT, " +
                    "`name` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`isActive` INTEGER NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_morning_routine_seedKey` " +
                    "ON `morning_routine` (`seedKey`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_morning_routine_sortOrder` " +
                    "ON `morning_routine` (`sortOrder`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `morning_routine_step` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`routineId` INTEGER NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`instructions` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`targetDurationSec` INTEGER, " +
                    "`targetReps` INTEGER, " +
                    "`position` INTEGER NOT NULL, " +
                    "`isEnabled` INTEGER NOT NULL, " +
                    "`isSeeded` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`routineId`) REFERENCES `morning_routine`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_morning_routine_step_routineId_position` " +
                    "ON `morning_routine_step` (`routineId`, `position`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_morning_routine_step_routineId` " +
                    "ON `morning_routine_step` (`routineId`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `morning_routine_log` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`routineId` INTEGER, " +
                    "`routineName` TEXT NOT NULL, " +
                    "`dayKey` TEXT NOT NULL, " +
                    "`attempt` INTEGER NOT NULL, " +
                    "`startedAt` INTEGER NOT NULL, " +
                    "`completedAt` INTEGER, " +
                    "`status` TEXT NOT NULL, " +
                    "`completedSteps` INTEGER NOT NULL, " +
                    "`skippedSteps` INTEGER NOT NULL, " +
                    "`totalSteps` INTEGER NOT NULL, " +
                    "`notes` TEXT NOT NULL, " +
                    "FOREIGN KEY(`routineId`) REFERENCES `morning_routine`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_morning_routine_log_routineId_dayKey_attempt` " +
                    "ON `morning_routine_log` (`routineId`, `dayKey`, `attempt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_morning_routine_log_dayKey` " +
                    "ON `morning_routine_log` (`dayKey`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_morning_routine_log_startedAt` " +
                    "ON `morning_routine_log` (`startedAt`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `morning_routine_step_log` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`logId` INTEGER NOT NULL, " +
                    "`stepId` INTEGER, " +
                    "`position` INTEGER NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`targetDurationSec` INTEGER, " +
                    "`targetReps` INTEGER, " +
                    "`outcome` TEXT NOT NULL, " +
                    "`elapsedSec` INTEGER, " +
                    "`recordedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`logId`) REFERENCES `morning_routine_log`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`stepId`) REFERENCES `morning_routine_step`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_morning_routine_step_log_logId_position` " +
                    "ON `morning_routine_step_log` (`logId`, `position`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_morning_routine_step_log_stepId` " +
                    "ON `morning_routine_step_log` (`stepId`)",
            )
            seedDefaultMorningRoutine(db)
        }
    }

    /**
     * v5 -> v6: creates the Phase 6 nutrition tables (`nutrition_goal`, `food_log`, `hydration_log`).
     * Purely additive: preserves all Phase 1-5 data.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `nutrition_goal` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`calorieTarget` INTEGER NOT NULL, " +
                    "`proteinGrams` INTEGER NOT NULL, " +
                    "`carbGrams` INTEGER NOT NULL, " +
                    "`fatGrams` INTEGER NOT NULL, " +
                    "`hydrationMlTarget` INTEGER NOT NULL, " +
                    "`goalType` TEXT NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `food_log` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`dayKey` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`calories` INTEGER NOT NULL, " +
                    "`proteinGrams` REAL NOT NULL, " +
                    "`carbGrams` REAL NOT NULL, " +
                    "`fatGrams` REAL NOT NULL, " +
                    "`servingText` TEXT NOT NULL, " +
                    "`notes` TEXT NOT NULL, " +
                    "`loggedAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_food_log_dayKey` ON `food_log` (`dayKey`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_food_log_loggedAt` ON `food_log` (`loggedAt`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `hydration_log` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`dayKey` TEXT NOT NULL, " +
                    "`amountMl` INTEGER NOT NULL, " +
                    "`loggedAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hydration_log_dayKey` ON `hydration_log` (`dayKey`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hydration_log_loggedAt` ON `hydration_log` (`loggedAt`)",
            )
        }
    }

    /**
     * v6 -> v7: creates the Phase 7 progression tables (`xp_transaction`, `attribute`, `streak`, `achievement`).
     * Purely additive: preserves all Phase 1-6 data.
     * Achievements are initialized with default definitions; user progress starts at zero.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `xp_transaction` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`xpAmount` INTEGER NOT NULL, " +
                    "`source` TEXT NOT NULL, " +
                    "`sourceRef` TEXT NOT NULL, " +
                    "`dayKey` TEXT NOT NULL, " +
                    "`reason` TEXT NOT NULL, " +
                    "`loggedAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_xp_transaction_dayKey` ON `xp_transaction` (`dayKey`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_xp_transaction_loggedAt` ON `xp_transaction` (`loggedAt`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_xp_transaction_source_sourceRef` " +
                    "ON `xp_transaction` (`source`, `sourceRef`)",
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `attribute` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`strength` INTEGER NOT NULL DEFAULT 0, " +
                    "`endurance` INTEGER NOT NULL DEFAULT 0, " +
                    "`discipline` INTEGER NOT NULL DEFAULT 0, " +
                    "`recovery` INTEGER NOT NULL DEFAULT 0, " +
                    "`nutrition` INTEGER NOT NULL DEFAULT 0, " +
                    "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))",
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `streak` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`currentStreak` INTEGER NOT NULL DEFAULT 0, " +
                    "`longestStreak` INTEGER NOT NULL DEFAULT 0, " +
                    "`lastActiveDayKey` TEXT, " +
                    "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))",
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `achievement` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`unlocked` INTEGER NOT NULL DEFAULT 0, " +
                    "`unlockedAt` INTEGER, " +
                    "`progressCurrent` INTEGER NOT NULL DEFAULT 0, " +
                    "`progressTarget` INTEGER NOT NULL DEFAULT 1, " +
                    "PRIMARY KEY(`id`))",
            )

            // Seed default achievement definitions
            seedDefaultAchievements(db)
        }
    }

    private fun seedDefaultAchievements(db: SupportSQLiteDatabase) {
        val achievements = listOf(
            listOf("first_step", "First Step", "Complete your first qualifying activity", 1),
            listOf("workout_initiate", "Workout Initiate", "Complete your first workout", 1),
            listOf("morning_awakened", "Morning Awakened", "Complete your first Morning Activation", 1),
            listOf("nutrition_logged", "Nutrition Logged", "Log your first qualifying meal", 1),
            listOf("hydration_habit", "Hydration Habit", "Log your first qualifying hydration", 1),
            listOf("week_warrior", "Week Warrior", "7 consecutive qualifying days", 7),
            listOf("xp_100", "XP 100", "Reach 100 total XP", 100),
        )
        achievements.forEach { ach ->
            db.execSQL(
                "INSERT OR IGNORE INTO `achievement` (`id`, `name`, `description`, `progressTarget`) " +
                    "VALUES (?, ?, ?, ?)",
                arrayOf(ach[0], ach[1], ach[2], ach[3]),
            )
        }
    }

    /**
     * Inserts the built-in routine and its steps into an upgrading database.
     * Shared with nothing else on purpose: fresh installs seed through
     * [MorningRoutineSeeds] in the onCreate callback.
     */
    internal fun seedDefaultMorningRoutine(db: SupportSQLiteDatabase, now: Long = 0L) {
        db.execSQL(
            "INSERT OR IGNORE INTO `morning_routine` " +
                "(`seedKey`, `name`, `description`, `isActive`, `sortOrder`, `createdAt`, " +
                "`updatedAt`) VALUES (?, ?, ?, 1, 0, ?, ?)",
            arrayOf(
                MorningRoutineSeeds.DEFAULT_SEED_KEY,
                MorningRoutineSeeds.DEFAULT_NAME,
                MorningRoutineSeeds.DEFAULT_DESCRIPTION,
                now,
                now,
            ),
        )
        db.query(
            "SELECT `id` FROM `morning_routine` WHERE `seedKey` = ? LIMIT 1",
            arrayOf<Any>(MorningRoutineSeeds.DEFAULT_SEED_KEY),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return
            val routineId = cursor.getLong(0)
            MorningRoutineSeeds.STEPS.forEachIndexed { index, step ->
                db.execSQL(
                    "INSERT OR IGNORE INTO `morning_routine_step` " +
                        "(`routineId`, `title`, `instructions`, `category`, " +
                        "`targetDurationSec`, `targetReps`, `position`, `isEnabled`, " +
                        "`isSeeded`) VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1)",
                    arrayOf<Any?>(
                        routineId,
                        step.title,
                        step.instructions,
                        step.category.name,
                        step.targetDurationSec,
                        step.targetReps,
                        index,
                    ),
                )
            }
        }
    }
}
