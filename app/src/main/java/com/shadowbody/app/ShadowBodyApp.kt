package com.shadowbody.app

import android.app.Application
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.data.preferences.AppPreferences
import com.shadowbody.app.data.repository.AdaptiveWorkoutPlanner
import com.shadowbody.app.data.repository.AdaptationRepository
import com.shadowbody.app.data.repository.BaselineRepository
import com.shadowbody.app.data.repository.ExerciseRepository
import com.shadowbody.app.data.repository.GroomingRepository
import com.shadowbody.app.data.repository.WardrobeRepository
import com.shadowbody.app.data.repository.OutfitRepository
import com.shadowbody.app.data.repository.MorningActivationRepository
import com.shadowbody.app.data.repository.MorningRoutineRepository
import com.shadowbody.app.data.repository.NutritionRepository
import com.shadowbody.app.data.repository.PlanRepository
import com.shadowbody.app.data.repository.ProfileRepository
import com.shadowbody.app.data.repository.ProgressionRepository
import com.shadowbody.app.data.repository.ReadinessRepository
import com.shadowbody.app.data.repository.RecommendationRepository
import com.shadowbody.app.data.repository.SessionRepository

/**
 * SHADOW BODY application entry point.
 *
 * Owns the single [ShadowBodyDatabase] instance and the [AppPreferences]
 * DataStore wrapper. Both are local-first: the app launches and works
 * fully offline.
 */
class ShadowBodyApp : Application() {

    val database: ShadowBodyDatabase by lazy {
        ShadowBodyDatabase.build(this)
    }

    val preferences: AppPreferences by lazy {
        AppPreferences(this)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(database.userProfileDao())
    }

    val baselineRepository: BaselineRepository by lazy {
        BaselineRepository(database.baselineRecordDao())
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao())
    }

    val planRepository: PlanRepository by lazy {
        PlanRepository(database.planDao(), database.planExerciseDao(), database.exerciseDao())
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(database.sessionDao(), planRepository, progressionRepository)
    }

    // --- Phase 4: adaptive layer ---

    val readinessRepository: ReadinessRepository by lazy {
        ReadinessRepository(database.readinessDao())
    }

    val adaptationRepository: AdaptationRepository by lazy {
        AdaptationRepository(
            database.adaptationDao(),
            database.sessionDao(),
            database.readinessDao(),
            database.missedWorkoutDao(),
        )
    }

    val recommendationRepository: RecommendationRepository by lazy {
        RecommendationRepository(database.recommendationDao())
    }

    val adaptivePlanner: AdaptiveWorkoutPlanner by lazy {
        AdaptiveWorkoutPlanner(
            profiles = profileRepository,
            plans = planRepository,
            planDao = database.planDao(),
            planExercises = database.planExerciseDao(),
            exerciseDao = database.exerciseDao(),
            sessions = database.sessionDao(),
            adaptation = adaptationRepository,
            readiness = readinessRepository,
            recommendations = recommendationRepository,
        )
    }

    // --- Phase 5: morning activation ---

    val morningRoutineRepository: MorningRoutineRepository by lazy {
        MorningRoutineRepository(database.morningRoutineDao(), database.morningRoutineStepDao())
    }

    val morningActivationRepository: MorningActivationRepository by lazy {
        MorningActivationRepository(
            database.morningRoutineLogDao(),
            database.morningRoutineDao(),
            database.morningRoutineStepDao(),
            progressionRepository,
        )
    }

    // --- Phase 6: nutrition ---
    val nutritionRepository: NutritionRepository by lazy {
        NutritionRepository(
            database.nutritionGoalDao(),
            database.foodLogDao(),
            database.hydrationLogDao(),
            progressionRepository,
        )
    }

    // --- Phase 7: progression ---
    val progressionRepository: ProgressionRepository by lazy {
        ProgressionRepository(
            database.xpTransactionDao(),
            database.attributeDao(),
            database.streakDao(),
            database.achievementDao(),
        )
    }

    // --- Phase 8: grooming ---
    val groomingRepository: GroomingRepository by lazy {
        GroomingRepository(
            database.groomingPreferencesDao(),
            database.groomingRoutineDao(),
            database.groomingRoutineStepDao(),
            database.groomingLogDao(),
        )
    }

    // --- Phase 9: wardrobe ---
    val wardrobeRepository: WardrobeRepository by lazy {
        WardrobeRepository(database.wardrobeItemDao())
    }

    val outfitRepository: OutfitRepository by lazy {
        OutfitRepository(database.outfitRecordDao())
    }
}
