package com.shadowbody.app.domain.grooming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GroomingRecommendationTest {

    @Test
    fun mondayReturnsDeepCleanse() {
        val result = GroomingRecommendationEngine.generate(Calendar.MONDAY)
        assertEquals("Deep Cleanse", result.title)
        assertEquals("CLEANSING", result.category)
    }

    @Test
    fun tuesdayReturnsHydrationFocus() {
        val result = GroomingRecommendationEngine.generate(Calendar.TUESDAY)
        assertEquals("Hydration Focus", result.title)
        assertEquals("MOISTURIZING", result.category)
    }

    @Test
    fun wednesdayReturnsSunProtection() {
        val result = GroomingRecommendationEngine.generate(Calendar.WEDNESDAY)
        assertEquals("Sun Protection", result.title)
        assertEquals("SUNSCREEN", result.category)
    }

    @Test
    fun thursdayReturnsBasicGrooming() {
        val result = GroomingRecommendationEngine.generate(Calendar.THURSDAY)
        assertEquals("Basic Grooming", result.title)
        assertEquals("GROOMING", result.category)
    }

    @Test
    fun fridayReturnsPreWeekendPrep() {
        val result = GroomingRecommendationEngine.generate(Calendar.FRIDAY)
        assertEquals("Pre-Weekend Prep", result.title)
        assertEquals("GROOMING", result.category)
    }

    @Test
    fun saturdayReturnsRecoveryGrooming() {
        val result = GroomingRecommendationEngine.generate(Calendar.SATURDAY)
        assertEquals("Recovery Grooming", result.title)
        assertEquals("RECOVERY", result.category)
    }

    @Test
    fun sundayReturnsRecoveryGrooming() {
        val result = GroomingRecommendationEngine.generate(Calendar.SUNDAY)
        assertEquals("Recovery Grooming", result.title)
        assertEquals("RECOVERY", result.category)
    }

    @Test
    fun allRecommendationsHaveInstructions() {
        listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY,
        ).forEach { day ->
            val result = GroomingRecommendationEngine.generate(day)
            assertTrue("Day $day should have instruction", result.instruction.isNotBlank())
            assertTrue("Day $day should have reason", result.reason.isNotBlank())
        }
    }

    @Test
    fun safetyNoteIsNotMedicalAdvice() {
        val note = GroomingRecommendationEngine.getSafetyNote()
        assertTrue(note.contains("not medical advice"))
    }

    @Test
    fun deterministicSameDaySameRecommendation() {
        val result1 = GroomingRecommendationEngine.generate(Calendar.MONDAY)
        val result2 = GroomingRecommendationEngine.generate(Calendar.MONDAY)
        assertEquals(result1, result2)
    }

    @Test
    fun noMedicalDiagnosisInRecommendations() {
        listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY,
        ).forEach { day ->
            val result = GroomingRecommendationEngine.generate(day)
            val text = "${result.title} ${result.instruction} ${result.reason}".lowercase()
            assertTrue("Should not diagnose: $text", !text.contains("diagnose"))
            assertTrue("Should not prescribe: $text", !text.contains("prescribe"))
            assertTrue("Should not claim cure: $text", !text.contains("cure"))
        }
    }
}
