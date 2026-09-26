package com.shadowbody.app.data.local

import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.validation.MorningRoutineValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 built-in routine contract.
 *
 * The seed is a *product* surface, not just fixture data: it has to stay within
 * the routine bounds, stay beginner-friendly, make no medical claims, and be
 * deterministic, because the v4->v5 migration and a fresh install both converge
 * on it.
 */
class MorningRoutineSeedsTest {

    @Test
    fun `the default routine has between eight and twelve steps`() {
        val count = MorningRoutineSeeds.STEPS.size
        assertTrue("a morning routine must stay short, was $count", count in 8..12)
    }

    @Test
    fun `every seeded step passes validation`() {
        MorningRoutineSeeds.STEPS.forEach { step ->
            val errors = MorningRoutineValidator.validateStep(
                title = step.title,
                instructions = step.instructions,
                category = step.category,
                durationSec = step.targetDurationSec,
                reps = step.targetReps,
            )
            assertTrue("seed step '${step.title}' is invalid: $errors", errors.isEmpty())
        }
    }

    @Test
    fun `the routine header passes validation`() {
        assertTrue(
            MorningRoutineValidator
                .validateRoutine(MorningRoutineSeeds.DEFAULT_NAME, MorningRoutineSeeds.DEFAULT_DESCRIPTION)
                .isEmpty(),
        )
    }

    @Test
    fun `step titles are unique`() {
        val titles = MorningRoutineSeeds.STEPS.map { it.title }
        assertEquals(titles.size, titles.distinct().size)
    }

    @Test
    fun `at least one step is a plain reminder with no physical target`() {
        val reminders = MorningRoutineSeeds.STEPS.filter {
            it.targetDurationSec == null && it.targetReps == null
        }
        assertTrue(reminders.isNotEmpty())
        assertTrue(reminders.any { it.category == MorningStepCategory.HYDRATION })
    }

    @Test
    fun `the routine mixes timed, counted and reminder steps`() {
        val timed = MorningRoutineSeeds.STEPS.count { it.targetDurationSec != null }
        val counted = MorningRoutineSeeds.STEPS.count { it.targetReps != null }
        val reminders = MorningRoutineSeeds.STEPS.count {
            it.targetDurationSec == null && it.targetReps == null
        }
        assertTrue(timed > 0)
        assertTrue(counted > 0)
        assertTrue(reminders > 0)
    }

    @Test
    fun `the routine fits in a short morning window`() {
        val seconds = MorningRoutineSeeds.STEPS.sumOf { it.targetDurationSec ?: 0 }
        assertTrue("timed steps total ${seconds}s, too long", seconds in 300..1_200)
    }

    @Test
    fun `no seeded text makes a medical claim`() {
        val forbidden = listOf(
            "prevent", "cures", "cure", "treat", "heals", "diagnos", "injury",
            "injuries", "pain relief", "boosts immunity", "detox", "therapy",
            "rehab", "safe for everyone", "you must",
        )
        MorningRoutineSeeds.STEPS.forEach { step ->
            val text = "${step.title} ${step.instructions}".lowercase()
            forbidden.forEach { word ->
                assertFalse(
                    "seed step '${step.title}' must not claim '$word'",
                    text.contains(word),
                )
            }
        }
        val header = "${MorningRoutineSeeds.DEFAULT_NAME} ${MorningRoutineSeeds.DEFAULT_DESCRIPTION}"
            .lowercase()
        forbidden.forEach { word ->
            assertFalse("routine header must not claim '$word'", header.contains(word))
        }
    }

    @Test
    fun `the built-in routine carries a stable seed key and is active`() {
        assertEquals("MORNING_ACTIVATION_V1", MorningRoutineSeeds.DEFAULT_SEED_KEY)
        val routine = MorningRoutineSeeds.routine(now = 1_000L)
        assertNotNull(routine.seedKey)
        assertTrue(routine.isActive)
        assertEquals(1_000L, routine.createdAt)
        assertEquals(1_000L, routine.updatedAt)
    }

    @Test
    fun `seeded steps get normalized positions and the given routine id`() {
        val steps = MorningRoutineSeeds.steps(routineId = 42L)
        assertEquals(steps.indices.toList(), steps.map { it.position })
        assertTrue(steps.all { it.routineId == 42L })
        assertTrue(steps.all { it.isEnabled })
        assertTrue(steps.all { it.isSeeded })
        // Deterministic: same input, same output.
        assertEquals(steps, MorningRoutineSeeds.steps(routineId = 42L))
    }
}
