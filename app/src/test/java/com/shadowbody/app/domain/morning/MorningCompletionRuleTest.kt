package com.shadowbody.app.domain.morning

import com.shadowbody.app.domain.model.MorningLogStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 5 completion criteria.
 *
 * The product rule is strict on purpose: a skipped step is recorded honestly
 * but never counted as work done, and a run the user walked away from is stored
 * as ABANDONED rather than promoted to a success.
 */
class MorningCompletionRuleTest {

    @Test
    fun `untouched run is not ready to finish`() {
        assertEquals(
            MorningFinishDecision.NOT_READY,
            MorningCompletionRule.evaluate(totalSteps = 12, completedSteps = 0, skippedSteps = 0),
        )
    }

    @Test
    fun `partly done run is not ready to finish`() {
        assertEquals(
            MorningFinishDecision.NOT_READY,
            MorningCompletionRule.evaluate(totalSteps = 12, completedSteps = 5, skippedSteps = 2),
        )
    }

    @Test
    fun `every step dealt with with at least one completion is complete`() {
        assertEquals(
            MorningFinishDecision.COMPLETED,
            MorningCompletionRule.evaluate(totalSteps = 12, completedSteps = 11, skippedSteps = 1),
        )
    }

    @Test
    fun `a fully completed run is complete`() {
        assertEquals(
            MorningFinishDecision.COMPLETED,
            MorningCompletionRule.evaluate(totalSteps = 4, completedSteps = 4, skippedSteps = 0),
        )
    }

    @Test
    fun `skipping everything is only partial, never a completion`() {
        assertEquals(
            MorningFinishDecision.PARTIAL,
            MorningCompletionRule.evaluate(totalSteps = 6, completedSteps = 0, skippedSteps = 6),
        )
    }

    @Test
    fun `an empty routine is never ready`() {
        assertEquals(
            MorningFinishDecision.NOT_READY,
            MorningCompletionRule.evaluate(totalSteps = 0, completedSteps = 0, skippedSteps = 0),
        )
    }

    @Test
    fun `one completed step out of many is enough for a completion`() {
        assertEquals(
            MorningCompletionRule.MIN_COMPLETED_STEPS,
            1,
        )
        assertEquals(
            MorningFinishDecision.COMPLETED,
            MorningCompletionRule.evaluate(totalSteps = 12, completedSteps = 1, skippedSteps = 11),
        )
    }

    @Test
    fun `decisions map to honest stored statuses`() {
        assertEquals(
            MorningLogStatus.COMPLETED,
            MorningCompletionRule.statusFor(MorningFinishDecision.COMPLETED),
        )
        assertEquals(
            MorningLogStatus.ABANDONED,
            MorningCompletionRule.statusFor(MorningFinishDecision.PARTIAL),
        )
        assertEquals(
            MorningLogStatus.ABANDONED,
            MorningCompletionRule.statusFor(MorningFinishDecision.NOT_READY),
        )
    }
}
