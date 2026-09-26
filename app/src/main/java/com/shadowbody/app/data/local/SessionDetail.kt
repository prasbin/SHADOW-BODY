package com.shadowbody.app.data.local

/** Assembled read model for one session exercise + its sets + exercise info. */
data class SessionExerciseDetail(
    val sessionExercise: SessionExercise,
    val exercise: Exercise,
    val sets: List<SessionSet>,
)

/** Assembled read model for the active-workout and result screens. */
data class SessionDetail(
    val session: WorkoutSession,
    val planName: String?,
    val exercises: List<SessionExerciseDetail>,
) {
    val totalSets: Int = exercises.sumOf { it.sets.size }
    val completedSets: Int = exercises.sumOf { d -> d.sets.count { it.isCompleted } }
    val completedExercises: Int = exercises.count { it.sessionExercise.isCompleted }
    val progress: Float =
        if (totalSets == 0) 0f else completedSets.toFloat() / totalSets.toFloat()

    val durationMin: Long? = session.endedAt?.let { (it - session.startedAt) / 60_000L }
}
