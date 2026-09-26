package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.PlanDao
import com.shadowbody.app.data.local.PlanDetail
import com.shadowbody.app.data.local.PlanExerciseDao
import com.shadowbody.app.data.local.PlanSlot
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.local.WorkoutPlanExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/** Plan store. Slots are always rewritten with normalized positions. */
class PlanRepository(
    private val plans: PlanDao,
    private val slots: PlanExerciseDao,
    private val exerciseLookup: com.shadowbody.app.data.local.ExerciseDao,
) {

    fun plans(): Flow<List<WorkoutPlan>> = plans.observePlans()

    fun detail(planId: Long): Flow<PlanDetail?> = combine(
        plans.observeById(planId),
        slots.observeByPlan(planId),
    ) { plan, rows -> plan to rows }
        .flatMapLatest { (plan, rows) ->
            flow {
                if (plan == null) {
                    emit(null)
                } else {
                    emit(PlanDetail(plan, rows.map { row -> assemble(plan.id, row) }))
                }
            }
        }

    private suspend fun assemble(
        planId: Long,
        row: WorkoutPlanExercise,
    ): PlanSlot {
        // A missing exercise fails loudly — RESTRICT should make this
        // unreachable, never silent.
        val exercise = exerciseLookup.getById(row.exerciseId)
            ?: error("Plan $planId references missing exercise ${row.exerciseId}")
        return PlanSlot(row, exercise)
    }

    suspend fun getDetailOnce(planId: Long): PlanDetail? {
        val plan = plans.getById(planId) ?: return null
        val rows = slots.getByPlan(planId)
        return PlanDetail(plan, rows.map { row ->
            val exercise = exerciseLookup.getById(row.exerciseId)
                ?: error("Plan $planId references missing exercise ${row.exerciseId}")
            PlanSlot(row, exercise)
        })
    }

    /**
     * Creates or updates a plan with its slots atomically (from the caller's
     * perspective): header write, old slots cleared, new slots inserted in
     * order. Positions are normalized to 0..n.
     */
    suspend fun savePlan(
        plan: WorkoutPlan,
        entries: List<WorkoutPlanExercise>,
    ): Long {
        val now = System.currentTimeMillis()
        val planId = if (plan.id == 0L) {
            plans.insert(plan.copy(createdAt = now, updatedAt = now))
        } else {
            plans.update(plan.copy(updatedAt = now))
            slots.deleteByPlan(plan.id)
            plan.id
        }
        entries.forEachIndexed { index, entry ->
            slots.insert(
                entry.copy(id = 0, planId = planId, position = index),
            )
        }
        return planId
    }

    suspend fun deletePlan(planId: Long) {
        plans.delete(planId) // Slots cascade; sessions keep SET_NULL planId.
    }
}
