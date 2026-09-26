package com.shadowbody.app.data.local

import com.shadowbody.app.domain.model.Difficulty
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.MuscleGroup
import com.shadowbody.app.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDetailTest {

    private fun detailOf(completed: List<Boolean>): SessionDetail {
        val exercise = Exercise(
            id = 1, name = "Push-Up", muscleGroup = MuscleGroup.CHEST,
            category = ExerciseCategory.BODYWEIGHT, equipment = Equipment.BODYWEIGHT,
            description = "d", instructions = "i", difficulty = Difficulty.BEGINNER,
        )
        val rows = completed.mapIndexed { index, done ->
            val seId = index + 1L
            SessionExerciseDetail(
                SessionExercise(id = seId, sessionId = 1L, exerciseId = 1L, position = index, isCompleted = done),
                exercise,
                listOf(
                    SessionSet(id = seId * 10, sessionExerciseId = seId, setNumber = 1, isCompleted = done),
                    SessionSet(id = seId * 10 + 1, sessionExerciseId = seId, setNumber = 2, isCompleted = done),
                ),
            )
        }
        return SessionDetail(
            WorkoutSession(id = 1L, name = "T", startedAt = 1_000L, endedAt = 61_000L, status = SessionStatus.COMPLETED),
            planName = "P",
            exercises = rows,
        )
    }

    @Test
    fun `progress counts completed sets`() {
        val detail = detailOf(listOf(true, false))
        assertEquals(4, detail.totalSets)
        assertEquals(2, detail.completedSets)
        assertEquals(0.5f, detail.progress, 0.001f)
        assertEquals(1, detail.completedExercises)
    }

    @Test
    fun `empty detail has zero progress and minute duration`() {
        val detail = detailOf(emptyList())
        assertEquals(0, detail.totalSets)
        assertEquals(0f, detail.progress, 0.001f)
        assertEquals(1L, detail.durationMin)
    }
}
