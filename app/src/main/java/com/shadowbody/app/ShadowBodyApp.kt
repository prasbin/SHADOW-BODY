package com.shadowbody.app

import android.app.Application
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.data.preferences.AppPreferences
import com.shadowbody.app.data.repository.BaselineRepository
import com.shadowbody.app.data.repository.ExerciseRepository
import com.shadowbody.app.data.repository.PlanRepository
import com.shadowbody.app.data.repository.ProfileRepository
import com.shadowbody.app.data.repository.SessionRepository

/**
 * Phase 1 application entry point.
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
        SessionRepository(database.sessionDao(), planRepository)
    }
}
