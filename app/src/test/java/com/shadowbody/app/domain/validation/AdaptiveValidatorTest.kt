package com.shadowbody.app.domain.validation

import com.shadowbody.app.domain.adaptive.AdaptationLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4 readiness validation. Deliberately narrow: the check-in only carries
 * two 1..5 ratings and a short optional note.
 */
class AdaptiveValidatorTest {

    @Test
    fun `a valid check-in has no errors`() {
        assertTrue(
            AdaptiveValidator.validateReadiness(1, 1, "").isEmpty(),
        )
        assertTrue(
            AdaptiveValidator.validateReadiness(5, 5, "sore shoulder").isEmpty(),
        )
        assertTrue(
            AdaptiveValidator.validateReadiness(3, 4, "").isEmpty(),
        )
    }

    @Test
    fun `ratings outside the scale are rejected`() {
        assertTrue(
            AdaptiveValidator.validateReadiness(0, 3, "").containsKey(AdaptiveValidator.FIELD_FATIGUE),
        )
        assertTrue(
            AdaptiveValidator.validateReadiness(6, 3, "").containsKey(AdaptiveValidator.FIELD_FATIGUE),
        )
        assertTrue(
            AdaptiveValidator.validateReadiness(3, 0, "").containsKey(AdaptiveValidator.FIELD_SORENESS),
        )
        assertTrue(
            AdaptiveValidator.validateReadiness(3, 6, "").containsKey(AdaptiveValidator.FIELD_SORENESS),
        )
    }

    @Test
    fun `missing ratings are rejected rather than assumed`() {
        val errors = AdaptiveValidator.validateReadiness(null, null, "")
        assertTrue(errors.containsKey(AdaptiveValidator.FIELD_FATIGUE))
        assertTrue(errors.containsKey(AdaptiveValidator.FIELD_SORENESS))
    }

    @Test
    fun `an over long note is rejected`() {
        val errors = AdaptiveValidator.validateReadiness(
            3,
            3,
            "x".repeat(AdaptiveValidator.MAX_NOTES + 1),
        )
        assertTrue(errors.containsKey(AdaptiveValidator.FIELD_NOTES))
    }

    @Test
    fun `a note of exactly the limit is accepted`() {
        assertTrue(
            AdaptiveValidator.validateReadiness(
                3,
                3,
                "x".repeat(AdaptiveValidator.MAX_NOTES),
            ).isEmpty(),
        )
    }

    @Test
    fun `the fatigue threshold matches the engine rule`() {
        assertFalse(AdaptiveValidator.isHighFatigue(AdaptationLimits.HIGH_FATIGUE_THRESHOLD - 1))
        assertTrue(AdaptiveValidator.isHighFatigue(AdaptationLimits.HIGH_FATIGUE_THRESHOLD))
        assertTrue(AdaptiveValidator.isHighFatigue(AdaptationLimits.READINESS_MAX))
    }

    @Test
    fun `both errors are reported at once`() {
        val errors = AdaptiveValidator.validateReadiness(9, -1, "")
        assertEquals(2, errors.size)
    }
}
