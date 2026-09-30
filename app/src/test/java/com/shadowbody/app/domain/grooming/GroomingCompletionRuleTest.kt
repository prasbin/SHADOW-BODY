package com.shadowbody.app.domain.grooming

import org.junit.Assert.assertEquals
import org.junit.Test

class GroomingCompletionRuleTest {

    @Test
    fun notReadyWhenStepsUntouched() {
        val result = GroomingCompletionRule.evaluate(totalSteps = 5, completedSteps = 2, skippedSteps = 0)
        assertEquals(GroomingCompletionRule.FinishDecision.NOT_READY, result)
    }

    @Test
    fun completedWhenAllDealtWith() {
        val result = GroomingCompletionRule.evaluate(totalSteps = 5, completedSteps = 3, skippedSteps = 2)
        assertEquals(GroomingCompletionRule.FinishDecision.COMPLETED, result)
    }

    @Test
    fun completedWhenAllCompleted() {
        val result = GroomingCompletionRule.evaluate(totalSteps = 3, completedSteps = 3, skippedSteps = 0)
        assertEquals(GroomingCompletionRule.FinishDecision.COMPLETED, result)
    }

    @Test
    fun completedWhenAllSkipped() {
        val result = GroomingCompletionRule.evaluate(totalSteps = 2, completedSteps = 0, skippedSteps = 2)
        assertEquals(GroomingCompletionRule.FinishDecision.COMPLETED, result)
    }

    @Test
    fun statusForCompleted() {
        assertEquals("COMPLETED", GroomingCompletionRule.statusFor(GroomingCompletionRule.FinishDecision.COMPLETED))
    }

    @Test
    fun statusForNotReady() {
        assertEquals("ABANDONED", GroomingCompletionRule.statusFor(GroomingCompletionRule.FinishDecision.NOT_READY))
    }
}