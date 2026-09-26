package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.Exercise
import com.shadowbody.app.data.local.ExerciseDao
import com.shadowbody.app.data.local.ExerciseSeeds
import kotlinx.coroutines.flow.Flow

/**
 * Exercise library store.
 *
 * Seeding is duplicate-safe at two levels: the unique name index with
 * IGNORE inserts (repository + migration SQL), and [ensureSeeded], which
 * the workout list calls on every visit so fresh installs are seeded even
 * though `onCreate` alone is never trusted for this.
 */
class ExerciseRepository(private val dao: ExerciseDao) {

    fun library(): Flow<List<Exercise>> = dao.observeActive()

    fun search(query: String, muscle: String?): Flow<List<Exercise>> =
        dao.search(query.trim(), muscle?.ifBlank { null })

    suspend fun getById(id: Long): Exercise? = dao.getById(id)

    suspend fun ensureSeeded(): Int {
        dao.insertIgnore(ExerciseSeeds.ALL)
        return dao.seededCount()
    }

    suspend fun addCustom(exercise: Exercise): Long =
        dao.insertStrict(exercise.copy(id = 0, isSeeded = false))

    suspend fun setActive(exercise: Exercise, active: Boolean) {
        dao.update(exercise.copy(isActive = active))
    }

    /**
     * Seeded exercises are never deleted: history references them via
     * RESTRICT foreign keys. Returns false when deletion is refused.
     */
    suspend fun delete(id: Long): Boolean = dao.deleteCustom(id) > 0
}
