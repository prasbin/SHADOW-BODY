package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.Goal
import com.shadowbody.app.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4 selection tests for [WorkoutGenerator].
 *
 * The generator is deterministic, so every assertion is an exact value rather
 * than a tolerance.
 */
class WorkoutGeneratorTest {

    private fun slot(
        id: Long,
        name: String = "Exercise $id",
        muscle: MuscleGroup = MuscleGroup.CHEST,
        equipment: Equipment = Equipment.BODYWEIGHT,
        category: ExerciseCategory = ExerciseCategory.STRENGTH,
        sets: Int = 3,
        reps: Int? = 10,
        durationSec: Int? = null,
        restSec: Int = 60,
    ) = SlotCandidate(
        exerciseId = id,
        exerciseName = name,
        muscleGroup = muscle,
        equipment = equipment,
        category = category,
        sets = sets,
        reps = reps,
        durationSec = durationSec,
        restSec = restSec,
    )

    private fun plan(
        id: Long,
        name: String = "Plan $id",
        completedCount: Int = 0,
        lastCompletedAt: Long = 0L,
        slots: List<SlotCandidate> = listOf(slot(1)),
    ) = PlanCandidate(
        id = id,
        name = name,
        completedCount = completedCount,
        lastCompletedAt = lastCompletedAt,
        slots = slots,
    )

    private fun context(
        equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT),
        goals: Set<Goal> = emptySet(),
        sessionMinutes: Int? = null,
        trainingDays: Set<Int> = emptySet(),
        plans: List<PlanCandidate> = emptyList(),
        library: List<SlotCandidate> = emptyList(),
        adaptations: Map<Long, ExerciseProgress> = emptyMap(),
        readiness: ReadinessSnapshot? = null,
        completedThisWeek: Int = 0,
        missedSessions: Int = 0,
    ) = GenerationContext(
        now = 1_000L,
        equipment = equipment,
        goals = goals,
        sessionMinutes = sessionMinutes,
        trainingDays = trainingDays,
        plans = plans,
        library = library,
        adaptations = adaptations,
        readiness = readiness,
        completedThisWeek = completedThisWeek,
        missedSessions = missedSessions,
    )

    // --- Determinism --------------------------------------------------------

    @Test
    fun `identical input produces an identical workout`() {
        val input = context(plans = listOf(plan(1, slots = listOf(slot(1), slot(2)))))
        val first = WorkoutGenerator.generate(input)
        val second = WorkoutGenerator.generate(input)
        assertEquals(first, second)
    }

    // --- Equipment ----------------------------------------------------------

    @Test
    fun `exercises the user has no equipment for are dropped`() {
        val workout = WorkoutGenerator.generate(
            context(
                equipment = setOf(Equipment.BODYWEIGHT),
                plans = listOf(
                    plan(1, slots = listOf(slot(1), slot(2, equipment = Equipment.DUMBBELLS))),
                ),
            ),
        )
        assertNotNull(workout)
        assertEquals(listOf(1L), workout!!.exercises.map { it.exerciseId })
    }

    @Test
    fun `no usable exercise anywhere yields no recommendation`() {
        val workout = WorkoutGenerator.generate(
            context(
                equipment = setOf(Equipment.BODYWEIGHT),
                plans = listOf(plan(1, slots = listOf(slot(1, equipment = Equipment.BARBELL)))),
                library = listOf(slot(9, equipment = Equipment.DUMBBELLS)),
            ),
        )
        assertNull(workout)
    }

    @Test
    fun `no equipment at all still allows bodyweight work`() {
        val workout = WorkoutGenerator.generate(
            context(
                equipment = setOf(Equipment.NONE),
                plans = listOf(plan(1, slots = listOf(slot(1)))),
            ),
        )
        assertNotNull(workout)
        assertEquals(1, workout!!.exercises.size)
    }

    // --- Plan rotation ------------------------------------------------------

    @Test
    fun `the least recently trained plan is chosen`() {
        val workout = WorkoutGenerator.generate(
            context(
                plans = listOf(
                    plan(1, name = "Recent", lastCompletedAt = 9_000L, slots = listOf(slot(1))),
                    plan(2, name = "Stale", lastCompletedAt = 1_000L, slots = listOf(slot(2))),
                ),
            ),
        )
        assertEquals(2L, workout!!.sourcePlanId)
        assertTrue(workout.name.contains("STALE"))
    }

    @Test
    fun `equal recency is broken by fewer completions then lowest id`() {
        val byCount = WorkoutGenerator.generate(
            context(
                plans = listOf(
                    plan(1, completedCount = 5, lastCompletedAt = 100L, slots = listOf(slot(1))),
                    plan(2, completedCount = 1, lastCompletedAt = 100L, slots = listOf(slot(2))),
                ),
            ),
        )
        assertEquals(2L, byCount!!.sourcePlanId)

        val byId = WorkoutGenerator.generate(
            context(
                plans = listOf(
                    plan(7, completedCount = 2, lastCompletedAt = 100L, slots = listOf(slot(1))),
                    plan(3, completedCount = 2, lastCompletedAt = 100L, slots = listOf(slot(2))),
                ),
            ),
        )
        assertEquals(3L, byId!!.sourcePlanId)
    }

    @Test
    fun `plans with nothing usable are skipped entirely`() {
        val workout = WorkoutGenerator.generate(
            context(
                plans = listOf(
                    plan(
                        1,
                        lastCompletedAt = 0L,
                        slots = listOf(slot(1, equipment = Equipment.PULL_UP_BAR)),
                    ),
                    plan(2, lastCompletedAt = 5_000L, slots = listOf(slot(2))),
                ),
            ),
        )
        assertEquals(2L, workout!!.sourcePlanId)
    }

    @Test
    fun `library is used when no plan exists`() {
        val workout = WorkoutGenerator.generate(
            context(library = listOf(slot(4), slot(5, muscle = MuscleGroup.LEGS))),
        )
        assertNotNull(workout)
        assertNull(workout!!.sourcePlanId)
        assertEquals("ADAPTIVE SESSION", workout.name)
    }

    // --- Duration and size --------------------------------------------------

    @Test
    fun `a long session is trimmed to the planned length`() {
        val workout = WorkoutGenerator.generate(
            context(
                sessionMinutes = 10,
                plans = listOf(plan(1, slots = (1L..4L).map { slot(it) })),
            ),
        )
        // 30 + 3*40 + 2*60 = 270s each; 4 fit 18 min, 2 fit 9 min.
        assertEquals(2, workout!!.exercises.size)
        assertTrue(workout.estimatedMinutes <= 10)
    }

    @Test
    fun `one exercise is always kept even when it overruns`() {
        val workout = WorkoutGenerator.generate(
            context(
                sessionMinutes = 15,
                plans = listOf(plan(1, slots = listOf(slot(1, sets = 6, reps = 30)))),
            ),
        )
        assertEquals(1, workout!!.exercises.size)
    }

    @Test
    fun `workout size is capped`() {
        val workout = WorkoutGenerator.generate(
            context(plans = listOf(plan(1, slots = (1L..9L).map { slot(it) }))),
        )
        assertEquals(AdaptationLimits.MAX_GENERATED_EXERCISES, workout!!.exercises.size)
    }

    @Test
    fun `estimate is at least one minute`() {
        val workout = WorkoutGenerator.generate(
            context(plans = listOf(plan(1, slots = listOf(slot(1, sets = 1, reps = 5))))),
        )
        assertTrue(workout!!.estimatedMinutes >= 1)
    }

    // --- Adaptation state ---------------------------------------------------

    @Test
    fun `stored adaptation targets are used as the baseline`() {
        val workout = WorkoutGenerator.generate(
            context(
                plans = listOf(plan(1, slots = listOf(slot(1, sets = 3)))),
                adaptations = mapOf(
                    1L to ExerciseProgress(
                        exerciseId = 1L,
                        currentSets = 5,
                        currentReps = 12,
                        restSec = 60,
                        sessionsAtTarget = 0,
                        lastResult = PerformanceRecord(
                            completedSets = 3,
                            targetSets = 3,
                            actualReps = 12,
                            targetReps = 12,
                        ),
                    ),
                ),
            ),
        )
        val row = workout!!.exercises.single()
        // One clean session at the stored 5x12 target: held, not yet raised.
        assertEquals(5, row.sets)
        assertEquals(12, row.reps)
        assertEquals(AdaptationReason.SINGLE_SUCCESS, row.reason)
        assertEquals("5 x 12", row.previousTargetText)
    }

    @Test
    fun `a second clean session at the stored target raises it`() {
        val workout = WorkoutGenerator.generate(
            context(
                plans = listOf(plan(1, slots = listOf(slot(1, sets = 3)))),
                adaptations = mapOf(
                    1L to ExerciseProgress(
                        exerciseId = 1L,
                        currentSets = 5,
                        currentReps = 12,
                        restSec = 60,
                        sessionsAtTarget = 1,
                        lastResult = PerformanceRecord(
                            completedSets = 3,
                            targetSets = 3,
                            actualReps = 12,
                            targetReps = 12,
                        ),
                    ),
                ),
            ),
        )
        val row = workout!!.exercises.single()
        assertEquals(6, row.sets)
        assertEquals(AdaptationReason.SETS_PROGRESSED, row.reason)
        assertEquals(1, workout.progressed)
    }

    @Test
    fun `unknown exercises fall back to the slot target and claim no progress`() {
        val workout = WorkoutGenerator.generate(
            context(plans = listOf(plan(1, slots = listOf(slot(1, sets = 4, reps = 8))))),
        )
        val row = workout!!.exercises.single()
        assertEquals(4, row.sets)
        assertEquals(8, row.reps)
        assertEquals(AdaptationReason.NO_DATA, row.reason)
    }

    @Test
    fun `high fatigue lowers generated demand and says so`() {
        val workout = WorkoutGenerator.generate(
            context(
                plans = listOf(plan(1, slots = listOf(slot(1, sets = 3)))),
                readiness = ReadinessSnapshot(fatigue = 5, soreness = 1),
            ),
        )
        val row = workout!!.exercises.single()
        assertEquals(2, row.sets)
        assertEquals(AdaptationReason.FATIGUE_REDUCED, row.reason)
        assertTrue(workout.summary.contains("fatigue"))
    }

    // --- Explanation --------------------------------------------------------

    @Test
    fun `every exercise carries a reason and its previous target`() {
        val workout = WorkoutGenerator.generate(
            context(plans = listOf(plan(1, slots = listOf(slot(1, sets = 3, reps = 10))))),
        )
        val row = workout!!.exercises.single()
        assertTrue(row.reason.message.isNotBlank())
        assertEquals("3 x 10", row.previousTargetText)
        assertEquals("3 x 10", row.targetText())
    }

    @Test
    fun `summary reports the change counts`() {
        val workout = WorkoutGenerator.generate(
            context(plans = listOf(plan(1, slots = listOf(slot(1), slot(2))))),
        )
        assertTrue(workout!!.summary.contains("maintained"))
        assertEquals(0, workout.progressed)
        assertEquals(0, workout.reduced)
        assertEquals(2, workout.maintained)
    }

    @Test
    fun `a completed week is mentioned rather than escalated`() {
        val workout = WorkoutGenerator.generate(
            context(
                trainingDays = setOf(1, 3, 5),
                completedThisWeek = 3,
                plans = listOf(plan(1, slots = listOf(slot(1)))),
            ),
        )
        assertTrue(workout!!.summary.contains("already complete"))
        assertEquals(AdaptationReason.SCHEDULE_MET, workout.exercises.single().reason)
    }

    @Test
    fun `missed sessions are reported as a conservative reduction`() {
        val workout = WorkoutGenerator.generate(
            context(
                missedSessions = 2,
                plans = listOf(plan(1, slots = listOf(slot(1, sets = 3)))),
            ),
        )
        val row = workout!!.exercises.single()
        assertEquals(AdaptationReason.MISSED_REDUCED, row.reason)
        assertTrue(workout.summary.contains("Missed"))
    }

    // --- Ordering -----------------------------------------------------------

    @Test
    fun `library ordering puts goal muscles first`() {
        val ordered = WorkoutGenerator.orderLibrary(
            listOf(
                slot(1, muscle = MuscleGroup.ARMS),
                slot(2, muscle = MuscleGroup.CHEST),
                slot(3, muscle = MuscleGroup.LEGS),
            ),
            setOf(Goal.BUILD_STRENGTH),
        )
        assertEquals(2L, ordered.first().exerciseId)
    }

    @Test
    fun `default target depends on the category`() {
        assertEquals(10 to null, WorkoutGenerator.defaultTarget(ExerciseCategory.STRENGTH))
        assertEquals(null to 45, WorkoutGenerator.defaultTarget(ExerciseCategory.CARDIO))
        assertEquals(null to 45, WorkoutGenerator.defaultTarget(ExerciseCategory.CORE))
    }

    @Test
    fun `positions are contiguous and ordered`() {
        val workout = WorkoutGenerator.generate(
            context(plans = listOf(plan(1, slots = (1L..4L).map { slot(it) }))),
        )
        assertEquals(listOf(0, 1, 2, 3), workout!!.exercises.map { it.position })
    }
}
