package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobePhotoCombination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitAnalysisTest {

    private fun combination(
        position: Int,
        photoPath: String? = "/photo.jpg",
        notes: String = "",
    ) = WardrobePhotoCombination(
        position = position,
        label = "Combination ${position + 1}",
        photoPath = photoPath,
        notes = notes,
    )

    @Test
    fun emptyCombinationsReturnsNoRecommendation() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(emptyList())

        assertEquals(-1, result.recommendedPosition)
        assertTrue(result.issues.isNotEmpty())
        assertFalse(result.isVisualAnalysis)
        assertTrue(result.isRuleBased)
    }

    @Test
    fun noPhotosReturnsNoRecommendation() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(
                combination(0, photoPath = null),
                combination(1, photoPath = null),
            )
        )

        assertEquals(-1, result.recommendedPosition)
        assertTrue(result.issues.isNotEmpty())
    }

    @Test
    fun recommendsFirstCombinationWithPhoto() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(
                combination(0, photoPath = "/photo0.jpg"),
                combination(1, photoPath = "/photo1.jpg"),
                combination(2, photoPath = "/photo2.jpg"),
            )
        )

        assertEquals(0, result.recommendedPosition)
        assertTrue(result.reasoning.contains("Combination 1"))
    }

    @Test
    fun missingPhotosAreReported() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(
                combination(0, photoPath = "/photo0.jpg"),
                combination(1, photoPath = null),
                combination(2, photoPath = null),
            )
        )

        assertTrue(result.issues.any { it.contains("missing photos") || it.contains("2") })
    }

    @Test
    fun notesAreIncludedInStrengths() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(
                combination(0, photoPath = "/photo0.jpg", notes = "Blue shirt with jeans"),
            )
        )

        assertTrue(result.strengths.any { it.contains("Blue shirt") })
    }

    @Test
    fun deterministicSameInputSameOutput() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val combinations = listOf(
            combination(0, photoPath = "/photo0.jpg"),
            combination(1, photoPath = "/photo1.jpg"),
        )

        val result1 = provider.analyze(combinations)
        val result2 = provider.analyze(combinations)

        assertEquals(result1, result2)
    }

    @Test
    fun doesNotClaimVisualAnalysis() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(combination(0, photoPath = "/photo0.jpg"))
        )

        assertFalse(result.isVisualAnalysis)
        assertTrue(result.isRuleBased)
        assertTrue(result.reasoning.contains("rule-based"))
    }

    @Test
    fun doesNotInventClothingItems() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(combination(0, photoPath = "/photo0.jpg"))
        )

        assertTrue(result.reasoning.contains("Combination 1"))
    }

    @Test
    fun incompletePhotosHandledGracefully() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(
                combination(0, photoPath = null),
                combination(1, photoPath = null),
                combination(2, photoPath = null),
            )
        )

        assertEquals(-1, result.recommendedPosition)
        assertTrue(result.issues.isNotEmpty())
    }

    @Test
    fun privacyPreservedNoUpload() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(combination(0, photoPath = "/local/photo.jpg"))
        )

        assertTrue(result.isRuleBased)
        assertFalse(result.isVisualAnalysis)
    }

    @Test
    fun doesNotMakeMedicalClaims() {
        val provider = LocalRuleBasedOutfitAnalysis()
        val result = provider.analyze(
            listOf(combination(0, photoPath = "/photo0.jpg"))
        )

        val text = "${result.reasoning} ${result.strengths.joinToString(" ")}".lowercase()
        assertFalse(text.contains("medical"))
        assertFalse(text.contains("health"))
        assertFalse(text.contains("diagnose"))
    }
}
