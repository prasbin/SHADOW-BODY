package com.shadowbody.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineValidatorTest {

    @Test
    fun `single measurement is valid`() {
        assertTrue(BaselineValidator.validate(BaselineInput(weightKg = "80")).isEmpty())
    }

    @Test
    fun `empty record is rejected`() {
        val errors = BaselineValidator.validate(BaselineInput())
        assertTrue(errors.containsKey(BaselineValidator.FIELD_GENERAL))
    }

    @Test
    fun `non-numeric measurement is rejected`() {
        val errors = BaselineValidator.validate(BaselineInput(waistCm = "wide"))
        assertTrue(errors.containsKey(BaselineValidator.FIELD_WAIST))
    }

    @Test
    fun `nan is rejected`() {
        val errors = BaselineValidator.validate(BaselineInput(weightKg = "NaN"))
        assertTrue(errors.containsKey(BaselineValidator.FIELD_WEIGHT))
    }

    @Test
    fun `girth bounds are enforced`() {
        assertTrue(
            BaselineValidator.validate(BaselineInput(chestCm = "5"))
                .containsKey(BaselineValidator.FIELD_CHEST),
        )
        assertTrue(
            BaselineValidator.validate(BaselineInput(thighCm = "999"))
                .containsKey(BaselineValidator.FIELD_THIGH),
        )
    }

    @Test
    fun `body fat percentage bounds are enforced`() {
        assertTrue(
            BaselineValidator.validate(BaselineInput(bodyFatPct = "0"))
                .containsKey(BaselineValidator.FIELD_BODY_FAT),
        )
        assertTrue(
            BaselineValidator.validate(BaselineInput(bodyFatPct = "95"))
                .containsKey(BaselineValidator.FIELD_BODY_FAT),
        )
        assertTrue(BaselineValidator.validate(BaselineInput(bodyFatPct = "18.5")).isEmpty())
    }

    @Test
    fun `oversized notes are rejected`() {
        val errors = BaselineValidator.validate(BaselineInput(weightKg = "80", notes = "x".repeat(501)))
        assertTrue(errors.containsKey(BaselineValidator.FIELD_NOTES))
    }

    @Test
    fun `toRecord maps blanks to null`() {
        val record = BaselineValidator.toRecord(BaselineInput(weightKg = "80", notes = "  "))
        assertEquals(80.0, record.weightKg!!, 0.0)
        assertNull(record.waistCm)
        assertNull(record.notes)
    }
}
