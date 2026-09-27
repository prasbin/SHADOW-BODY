package com.shadowbody.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * SHADOW BODY database.
 *
 * - v1: [SchemaAnchor] foundation only.
 * - v2: + [UserProfile], + [BaselineRecord] (see [Migrations.MIGRATION_1_2]).
 * - v3: + workout engine — [Exercise], [WorkoutPlan], [WorkoutPlanExercise],
 *   [WorkoutSession], [SessionExercise], [SessionSet]
 *   (see [Migrations.MIGRATION_2_3]).
 * - v4: + adaptive workouts — [ReadinessReport], [ExerciseAdaptation],
 *   [MissedWorkout], [WorkoutRecommendation], [RecommendedExercise],
 *   [AdaptationCheckpoint] (see [Migrations.MIGRATION_3_4]).
 * - v5: + morning activation — [MorningRoutine], [MorningRoutineStep],
 *   [MorningRoutineLog], [MorningRoutineStepLog]
 *   (see [Migrations.MIGRATION_4_5]).
 * - v6: + nutrition MVP — [NutritionGoal], [FoodLog], [HydrationLog]
 *   (see [Migrations.MIGRATION_5_6]).
 * - v7: + progression system — [XpTransaction], [Attribute], [Streak], [Achievement]
 *   (see [Migrations.MIGRATION_6_7]).
 *
 * Every version bump ships an explicit Migration; destructive fallback is
 * never enabled. Schemas are exported to `app/schemas` and committed.
 */
@Database(
    entities = [
        SchemaAnchor::class,
        UserProfile::class,
        BaselineRecord::class,
        Exercise::class,
        WorkoutPlan::class,
        WorkoutPlanExercise::class,
        WorkoutSession::class,
        SessionExercise::class,
        SessionSet::class,
        ReadinessReport::class,
        ExerciseAdaptation::class,
        MissedWorkout::class,
        WorkoutRecommendation::class,
        RecommendedExercise::class,
        AdaptationCheckpoint::class,
        MorningRoutine::class,
        MorningRoutineStep::class,
        MorningRoutineLog::class,
        MorningRoutineStepLog::class,
        NutritionGoal::class,
        FoodLog::class,
        HydrationLog::class,
        XpTransaction::class,
        Attribute::class,
        Streak::class,
        Achievement::class,
    ],
    version = ShadowBodyDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ShadowBodyDatabase : RoomDatabase() {

    abstract fun schemaAnchorDao(): SchemaAnchorDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun baselineRecordDao(): BaselineRecordDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun planExerciseDao(): PlanExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun readinessDao(): ReadinessDao
    abstract fun adaptationDao(): AdaptationDao
    abstract fun missedWorkoutDao(): MissedWorkoutDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun morningRoutineDao(): MorningRoutineDao
    abstract fun morningRoutineStepDao(): MorningRoutineStepDao
    abstract fun morningRoutineLogDao(): MorningRoutineLogDao
    abstract fun nutritionGoalDao(): NutritionGoalDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun hydrationLogDao(): HydrationLogDao
    abstract fun xpTransactionDao(): XpTransactionDao
    abstract fun attributeDao(): AttributeDao
    abstract fun streakDao(): StreakDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val VERSION = 7
        const val NAME = "shadow_body.db"

        @Volatile
        private var instance: ShadowBodyDatabase? = null

        fun build(context: Context): ShadowBodyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShadowBodyDatabase::class.java,
                    NAME,
                )
                    .addMigrations(
                        Migrations.MIGRATION_1_2,
                        Migrations.MIGRATION_2_3,
                        Migrations.MIGRATION_3_4,
                        Migrations.MIGRATION_4_5,
                        Migrations.MIGRATION_5_6,
                        Migrations.MIGRATION_6_7,
                    )
                    .build().also { instance = it }
            }

        /** Test helper: in-memory database, never persisted. */
        fun buildInMemory(context: Context): ShadowBodyDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ShadowBodyDatabase::class.java,
            ).build()
    }
}
