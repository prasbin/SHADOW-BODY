package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.data.local.UserProfileDao
import kotlinx.coroutines.flow.Flow

/** Profile store. Validation lives in ProfileValidator; this only persists. */
class ProfileRepository(private val dao: UserProfileDao) {

    val profile: Flow<UserProfile?> = dao.observe()

    suspend fun get(): UserProfile? = dao.get()

    suspend fun save(profile: UserProfile) {
        dao.upsert(profile.copy(id = 1, updatedAt = System.currentTimeMillis()))
    }
}
