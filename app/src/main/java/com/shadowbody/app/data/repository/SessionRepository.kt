package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.SessionDao
import com.shadowbody.app.data.local.SessionDetail
import com.shadowbody.app.data.local.SessionSet
import com.shadowbody.app.data.local.WorkoutSession
import com.shadowbody.app.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

/** Session store. All multi-row mutations go through [SessionDao]. */
class SessionRepository(
    private val sessions: SessionDao,
    private val plans: PlanRepository,
    private val progression: ProgressionRepository? = null,
) {

    fun recent(limit: Int = 20): Flow<List<WorkoutSession>> = sessions.observeRecent(limit)

    fun completedCount(): Flow<Int> = sessions.observeCompletedCount()

    suspend fun detail(sessionId: Long): SessionDetail? = sessions.getDetail(sessionId)

    /** Starts a session from the current plan snapshot. Returns session id. */
    suspend fun start(planId: Long): Long {
        val detail = plans.getDetailOnce(planId)
            ?: error("Cannot start missing plan $planId")
        require(detail.slots.isNotEmpty()) { "Cannot start an empty plan" }
        return sessions.startSession(detail.plan, detail.slots.map { it.slot })
    }

    suspend fun recordSet(
        set: SessionSet,
        actualReps: Int?,
        actualDurationSec: Int?,
        weightKg: Double?,
        completed: Boolean,
    ) {
        sessions.updateSet(
            set.copy(
                actualReps = actualReps,
                actualDurationSec = actualDurationSec,
                weightKg = weightKg,
                isCompleted = completed,
            ),
        )
    }

    suspend fun setExerciseCompleted(sessionExerciseId: Long, completed: Boolean) {
        val current = sessions.getDetailExercise(sessionExerciseId) ?: return
        sessions.updateSessionExercise(current.copy(isCompleted = completed))
    }

    suspend fun finish(sessionId: Long) {
        val current = sessions.getSession(sessionId) ?: return
        sessions.updateSession(
            current.copy(status = SessionStatus.COMPLETED, endedAt = System.currentTimeMillis()),
        )
        // Award XP for completed workout
        progression?.awardXp(
            source = com.shadowbody.app.domain.progression.XpSource.WORKOUT,
            sourceRef = "workout:$sessionId",
        )?.let { result ->
            if (result is ProgressionRepository.AwardResult.Awarded) {
                progression.processProgression()
            }
        }
    }

    suspend fun abandon(sessionId: Long) {
        val current = sessions.getSession(sessionId) ?: return
        sessions.updateSession(
            current.copy(status = SessionStatus.ABANDONED, endedAt = System.currentTimeMillis()),
        )
    }

    suspend fun delete(sessionId: Long) {
        sessions.deleteSession(sessionId) // Exercises + sets cascade.
    }

    /** Exposes plan slots for the editor without leaking DAOs. */
    suspend fun planSlots(planId: Long) =
        plans.getDetailOnce(planId)?.slots ?: emptyList()
}
