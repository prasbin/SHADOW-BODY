package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shadowbody.app.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SessionDao {

    // --- Writes ---

    @Insert
    protected abstract suspend fun insertSession(session: WorkoutSession): Long

    @Insert
    protected abstract suspend fun insertSessionExercise(row: SessionExercise): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertSet(set: SessionSet): Long

    @Update
    abstract suspend fun updateSession(session: WorkoutSession)

    @Update
    abstract suspend fun updateSessionExercise(row: SessionExercise)

    @Update
    abstract suspend fun updateSet(set: SessionSet)

    @Query("DELETE FROM workout_session WHERE id = :id")
    abstract suspend fun deleteSession(id: Long)

    /**
     * Starts a session from a plan snapshot: copies slots + targets into
     * session rows so later plan edits never rewrite recorded history.
     */
    @Transaction
    open suspend fun startSession(
        plan: WorkoutPlan,
        slots: List<WorkoutPlanExercise>,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val sessionId = insertSession(
            WorkoutSession(planId = plan.id, name = plan.name, startedAt = now),
        )
        slots.sortedBy { it.position }.forEachIndexed { index, slot ->
            val seId = insertSessionExercise(
                SessionExercise(
                    sessionId = sessionId,
                    exerciseId = slot.exerciseId,
                    position = index,
                ),
            )
            repeat(slot.targetSets) { n ->
                insertSet(
                    SessionSet(
                        sessionExerciseId = seId,
                        setNumber = n + 1,
                        targetReps = slot.targetReps,
                        targetDurationSec = slot.targetDurationSec,
                    ),
                )
            }
        }
        return sessionId
    }

    // --- Reads ---

    @Query("SELECT * FROM workout_session WHERE id = :id")
    protected abstract suspend fun sessionById(id: Long): WorkoutSession?

    // --- Phase 4: reads for the adaptive engine ---

    /** Completed sessions after the adaptation checkpoint, oldest first. */
    @Query("SELECT * FROM workout_session WHERE status = 'COMPLETED' AND id > :afterId ORDER BY id")
    abstract suspend fun completedSessionsAfter(afterId: Long): List<WorkoutSession>

    @Query("SELECT COUNT(*) FROM workout_session WHERE status = 'COMPLETED' AND endedAt >= :since")
    abstract suspend fun completedCountSince(since: Long): Int

    @Query(
        "SELECT COUNT(*) FROM workout_session " +
            "WHERE status = 'COMPLETED' AND planId = :planId",
    )
    abstract suspend fun completedCountForPlan(planId: Long): Int

    @Query(
        "SELECT MAX(endedAt) FROM workout_session " +
            "WHERE status = 'COMPLETED' AND planId = :planId",
    )
    abstract suspend fun lastCompletedAtForPlan(planId: Long): Long?

    /** Single-session read for finish/abandon flows. */
    @Query("SELECT * FROM workout_session WHERE id = :id")
    abstract suspend fun getSession(id: Long): WorkoutSession?

    /** Single exercise-row read for completion toggles. */
    @Query("SELECT * FROM session_exercise WHERE id = :id")
    abstract suspend fun getDetailExercise(id: Long): SessionExercise?

    @Query("SELECT * FROM session_exercise WHERE sessionId = :sessionId ORDER BY position")
    protected abstract suspend fun exercisesOf(sessionId: Long): List<SessionExercise>

    @Query("SELECT * FROM session_set WHERE sessionExerciseId = :seId ORDER BY setNumber")
    protected abstract suspend fun setsOf(seId: Long): List<SessionSet>

    @Query("SELECT * FROM exercise WHERE id = :id")
    protected abstract suspend fun exerciseById(id: Long): Exercise?

    @Query("SELECT name FROM workout_plan WHERE id = :planId")
    protected abstract suspend fun planName(planId: Long): String?

    @Transaction
    open suspend fun getDetail(sessionId: Long): SessionDetail? {
        val session = sessionById(sessionId) ?: return null
        val details = exercisesOf(sessionId).map { se ->
            val exercise = exerciseById(se.exerciseId)
                ?: error("Session $sessionId references missing exercise ${se.exerciseId}")
            SessionExerciseDetail(se, exercise, setsOf(se.id))
        }
        val planName = session.planId?.let { planName(it) }
        return SessionDetail(session, planName, details)
    }

    @Query(
        "SELECT * FROM workout_session ORDER BY " +
            "CASE WHEN status = 'IN_PROGRESS' THEN 0 ELSE 1 END, startedAt DESC LIMIT :limit",
    )
    abstract fun observeRecent(limit: Int = 20): Flow<List<WorkoutSession>>

    @Query("SELECT COUNT(*) FROM workout_session WHERE status = 'COMPLETED'")
    abstract fun observeCompletedCount(): Flow<Int>
}
