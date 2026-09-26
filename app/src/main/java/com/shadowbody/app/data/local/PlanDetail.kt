package com.shadowbody.app.data.local

/** Plan header + ordered slots assembled for detail/edit screens. */
data class PlanDetail(
    val plan: WorkoutPlan,
    val slots: List<PlanSlot>,
)

/** One slot with its exercise resolved. */
data class PlanSlot(
    val slot: WorkoutPlanExercise,
    val exercise: Exercise,
)
