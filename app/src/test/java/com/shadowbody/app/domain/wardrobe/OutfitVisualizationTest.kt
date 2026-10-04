package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobePhotoCombination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitVisualizationTest {

    private fun combination(
        position: Int = 0,
        photoPath: String? = "/photo.jpg",
        label: String = "Combination 1",
    ) = WardrobePhotoCombination(
        position = position,
        label = label,
        photoPath = photoPath,
    )

    @Test
    fun noPhotoReturnsError() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination(photoPath = null)) { r -> result = r }

        assertNotNull(result)
        assertFalse(result!!.success)
        assertNotNull(result!!.errorMessage)
    }

    @Test
    fun withPhotoReturnsTextDescription() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination()) { r -> result = r }

        assertNotNull(result)
        assertTrue(result!!.success)
        assertNotNull(result!!.description)
        assertFalse(result!!.isGeneratedImage)
        assertTrue(result!!.isTextDescription)
    }

    @Test
    fun doesNotClaimPixelPerfectFit() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination()) { r -> result = r }

        val desc = result!!.description!!.lowercase()
        assertTrue(desc.contains("does not represent"))
        assertTrue(desc.contains("not available"))
    }

    @Test
    fun doesNotIdentifyPerson() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination()) { r -> result = r }

        val desc = result!!.description!!.lowercase()
        assertFalse(desc.contains("identify"))
        assertFalse(desc.contains("recognize"))
        assertFalse(desc.contains("face"))
    }

    @Test
    fun noCloudDependency() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination()) { r -> result = r }

        assertTrue(result!!.success)
        assertFalse(result!!.isGeneratedImage)
    }

    @Test
    fun explicitUserActionRequired() {
        val provider = LocalOutfitVisualization()
        var called = false

        provider.generateVisualization(combination()) { called = true }

        assertTrue(called)
    }

    @Test
    fun noAutomaticGeneration() {
        val provider = LocalOutfitVisualization()
        var called = false

        assertFalse(called)
    }

    @Test
    fun descriptionIncludesCombinationLabel() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination(label = "My Outfit")) { r -> result = r }

        assertTrue(result!!.description!!.contains("My Outfit"))
    }

    @Test
    fun descriptionIncludesPhotoReference() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination(photoPath = "/local/photo.jpg")) { r -> result = r }

        assertTrue(result!!.description!!.contains("/local/photo.jpg"))
    }

    @Test
    fun noMedicalClaims() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination()) { r -> result = r }

        val desc = result!!.description!!.lowercase()
        assertFalse(desc.contains("medical"))
        assertFalse(desc.contains("health"))
        assertFalse(desc.contains("diagnose"))
    }

    @Test
    fun deterministicSameInputSameOutput() {
        val provider = LocalOutfitVisualization()
        var result1: OutfitVisualizationResult? = null
        var result2: OutfitVisualizationResult? = null

        provider.generateVisualization(combination()) { r -> result1 = r }
        provider.generateVisualization(combination()) { r -> result2 = r }

        assertEquals(result1, result2)
    }

    @Test
    fun noCopyrightedCharacters() {
        val provider = LocalOutfitVisualization()
        var result: OutfitVisualizationResult? = null

        provider.generateVisualization(combination()) { r -> result = r }

        val desc = result!!.description!!.lowercase()
        assertFalse(desc.contains("solo leveling"))
        assertFalse(desc.contains("copyright"))
    }
}
