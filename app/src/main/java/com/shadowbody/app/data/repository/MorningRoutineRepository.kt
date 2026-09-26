package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.MorningRoutine
import com.shadowbody.app.data.local.MorningRoutineDao
import com.shadowbody.app.data.local.MorningRoutineSeeds
import com.shadowbody.app.data.local.MorningRoutineStep
import com.shadowbody.app.data.local.MorningRoutineStepDao
import kotlinx.coroutines.flow.Flow

/**
 * Phase 5 routine management: the built-in morning routine plus any routine the
 * user creates.
 *
 * Seeding is duplicate-safe at two levels — the unique `seedKey` with IGNORE
 * inserts, and [ensureSeeded], which every entry point calls so a fresh install
 * is seeded even though `onCreate` alone is never trusted for this. Steps are
 * IGNORE-inserted against the unique `(routineId, position)` index, so a
 * repeated seed cannot duplicate or reorder an existing routine.
 */
class MorningRoutineRepository(
    private val routines: MorningRoutineDao,
    private val steps: MorningRoutineStepDao,
) {

    fun observeRoutines(): Flow<List<MorningRoutine>> = routines.observeAll()

    fun observeActive(): Flow<List<MorningRoutine>> = routines.observeActive()

    fun observeRoutine(routineId: Long): Flow<MorningRoutine?> = routines.observeById(routineId)

    fun observeSteps(routineId: Long): Flow<List<MorningRoutineStep>> =
        steps.observeByRoutine(routineId)

    suspend fun getAll(): List<MorningRoutine> = routines.getAll()

    suspend fun getRoutine(routineId: Long): MorningRoutine? = routines.getById(routineId)

    suspend fun getSteps(routineId: Long): List<MorningRoutineStep> = steps.getByRoutine(routineId)

    suspend fun getEnabledSteps(routineId: Long): List<MorningRoutineStep> =
        steps.getEnabledByRoutine(routineId)

    /** Seeds the default routine if it is missing. Returns its id either way. */
    suspend fun ensureSeeded(now: Long = System.currentTimeMillis()): Long {
        routines.insertIgnore(MorningRoutineSeeds.routine(now))
        val routine = routines.getBySeedKey(MorningRoutineSeeds.DEFAULT_SEED_KEY)
            ?: error("Default morning routine could not be seeded")
        if (steps.countByRoutine(routine.id) == 0) {
            MorningRoutineSeeds.steps(routine.id, now).forEach { steps.insertIgnore(it) }
        }
        return routine.id
    }

    /** Creates a routine with normalized step positions. */
    suspend fun createRoutine(
        name: String,
        description: String,
        entries: List<MorningRoutineStep>,
    ): Long {
        val now = System.currentTimeMillis()
        val routineId = routines.insert(
            MorningRoutine(
                name = name,
                description = description,
                createdAt = now,
                updatedAt = now,
            ),
        )
        entries.forEachIndexed { index, entry ->
            steps.insert(entry.copy(id = 0, routineId = routineId, position = index))
        }
        return routineId
    }

    /**
     * Updates a routine and rewrites its steps in order. Positions are always
     * normalized to 0..n so a reorder can never leave a gap or a duplicate.
     */
    suspend fun saveRoutine(
        routine: MorningRoutine,
        entries: List<MorningRoutineStep>,
    ) {
        val now = System.currentTimeMillis()
        routines.update(routine.copy(updatedAt = now))
        steps.deleteByRoutine(routine.id)
        entries.forEachIndexed { index, entry ->
            steps.insert(
                entry.copy(
                    id = 0,
                    routineId = routine.id,
                    position = index,
                    isSeeded = false,
                ),
            )
        }
    }

    suspend fun setActive(routine: MorningRoutine, active: Boolean) {
        routines.update(routine.copy(isActive = active, updatedAt = System.currentTimeMillis()))
    }

    /** Deletes a routine and its steps. Recorded runs survive with SET_NULL. */
    suspend fun delete(routineId: Long) {
        routines.delete(routineId)
    }

    suspend fun count(): Int = routines.count()
}
