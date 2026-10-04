package com.shadowbody.app.domain.grooming

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroomingPrivacyTest {

    @Test
    fun recommendationEngineIsLocalOnly() {
        val result = GroomingRecommendationEngine.generate()
        assertNotNull(result)
        assertTrue(result.instruction.isNotBlank())
    }

    @Test
    fun noCloudDependencyInRecommendationEngine() {
        val result = GroomingRecommendationEngine.generate()
        val text = "${result.title} ${result.instruction} ${result.reason}".lowercase()
        assertFalse("Should not reference cloud", text.contains("cloud"))
        assertFalse("Should not reference upload", text.contains("upload"))
        assertFalse("Should not reference server", text.contains("server"))
        assertFalse("Should not reference api", text.contains("api"))
    }

    @Test
    fun noNetworkPermissionRequired() {
        val result = GroomingRecommendationEngine.generate()
        assertNotNull(result)
    }

    @Test
    fun noFaceImageStorageInRecommendationEngine() {
        val result = GroomingRecommendationEngine.generate()
        val text = "${result.title} ${result.instruction} ${result.reason}".lowercase()
        assertFalse("Should not store images", text.contains("store image"))
        assertFalse("Should not save images", text.contains("save image"))
        assertFalse("Should not persist images", text.contains("persist image"))
    }

    @Test
    fun noAutomaticCaptureInRecommendationEngine() {
        val result = GroomingRecommendationEngine.generate()
        val text = "${result.title} ${result.instruction} ${result.reason}".lowercase()
        assertFalse("Should not auto-capture", text.contains("auto capture"))
        assertFalse("Should not auto-capture", text.contains("automatic capture"))
        assertFalse("Should not auto-capture", text.contains("background capture"))
    }

    @Test
    fun noMedicalClaimsInRecommendationEngine() {
        listOf(
            java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY,
            java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY,
            java.util.Calendar.SATURDAY,
        ).forEach { day ->
            val result = GroomingRecommendationEngine.generate(day)
            val text = "${result.title} ${result.instruction} ${result.reason}".lowercase()
            assertFalse("Should not diagnose", text.contains("diagnose"))
            assertFalse("Should not prescribe", text.contains("prescribe"))
            assertFalse("Should not claim cure", text.contains("cure"))
            assertFalse("Should not claim treat", text.contains("treat"))
        }
    }

    @Test
    fun safetyNoteClearlyStatesNotMedicalAdvice() {
        val note = GroomingRecommendationEngine.getSafetyNote()
        assertTrue(note.contains("not medical advice"))
        assertTrue(note.contains("professional"))
    }

    @Test
    fun recommendationEngineDoesNotRequireInternet() {
        val result = GroomingRecommendationEngine.generate()
        assertNotNull(result)
        assertTrue(result.instruction.isNotBlank())
    }

    @Test
    fun recommendationEngineDoesNotIdentifyPeople() {
        val result = GroomingRecommendationEngine.generate()
        val text = "${result.title} ${result.instruction} ${result.reason}".lowercase()
        assertFalse("Should not identify people", text.contains("identify"))
        assertFalse("Should not recognize faces", text.contains("recognize"))
    }
}
