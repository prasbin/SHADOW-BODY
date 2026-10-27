package com.shadowbody.app.domain.outfit

import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.domain.outfit.OutfitRecommenderEngine.Occasions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feature C engine tests. Core assertions:
 * 1. Only photographed options can be selected.
 * 2. Keyword evidence comes only from user-written label/notes.
 * 3. Every explanation explicitly denies image analysis.
 */
class OutfitRecommenderEngineTest {

    private fun option(
        id: Long,
        label: String,
        notes: String = "",
        withPhoto: Boolean = true,
        createdAt: Long = 1000,
    ) = WardrobePhotoCombination(
        id = id,
        position = id.toInt(),
        label = label,
        photoPath = if (withPhoto) "/private/opts/$id.jpg" else null,
        notes = notes,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    fun `no photographed options yields no fabricated recommendation`() {
        val empty = OutfitRecommenderEngine.recommend(emptyList(), Occasions.TRAINING)
        assertNull(empty)
        val noneWithPhoto = OutfitRecommenderEngine.recommend(
            listOf(option(1, "Something", withPhoto = false)),
            Occasions.TRAINING,
        )
        assertNull(noneWithPhoto)
    }

    @Test
    fun `keyword match in user notes selects that option`() {
        val result = OutfitRecommenderEngine.recommend(
            listOf(
                option(1, "Combo 1", notes = "comfy hoodie for chill"),
                option(2, "Combo 2", notes = "gym tshirt and shorts for training"),
            ),
            Occasions.TRAINING,
        )
        assertNotNull(result)
        assertEquals(2L, result?.selected?.id)
        assertTrue(result?.explanation?.contains("keyword") == true)
    }

    @Test
    fun `without keyword matches picks earliest created option` () {
        val result = OutfitRecommenderEngine.recommend(
            listOf(
                option(2, "Neutral B", createdAt = 3000),
                option(1, "Neutral A", createdAt = 1000),
            ),
            Occasions.TRAINING,
        )
        assertEquals(1L, result?.selected?.id)
        assertTrue(result?.explanation?.contains("earliest-added") == true)
    }

    @Test
    fun `explanation always states that no image analysis occurred`() {
        listOf(Occasions.TRAINING, Occasions.WORK_FORMAL, Occasions.SOCIAL_CASUAL, Occasions.REST_HOME)
            .forEach { occasion ->
                val result = OutfitRecommenderEngine.recommend(
                    listOf(option(1, "Any photo set")),
                    occasion,
                )
                assertNotNull(result)
                assertTrue(
                    "Expected disclaimer in explanation for $occasion",
                    result!!.explanation.contains("No image analysis") ||
                        result.explanation.contains("no image analysis", ignoreCase = true),
                )
                assertFalse(result.imageAnalysisUsed)
            }
    }

    @Test
    fun `imageAnalysisUsed is permanently false - documented limitation`() {
        val result = OutfitRecommenderEngine.recommend(
            listOf(option(1, "Blazer, oxford, formal")),
            Occasions.WORK_FORMAL,
        )
        assertFalse(result!!.imageAnalysisUsed)
    }

    @Test
    fun `formal keyword beats generic label`() {
        val result = OutfitRecommenderEngine.recommend(
            listOf(
                option(1, "Monday pick", createdAt = 500),
                option(2, "Oxford shirt and blazer", createdAt = 900),
            ),
            Occasions.WORK_FORMAL,
        )
        assertEquals(2L, result?.selected?.id)
    }

    @Test
    fun `only options with photos are considered`() {
        val result = OutfitRecommenderEngine.recommend(
            listOf(
                option(1, "gym clothes", withPhoto = false),
                option(2, "dinner jeans", withPhoto = true),
            ),
            Occasions.TRAINING,
        )
        assertEquals(listOf(2L), result?.consideredIds)
    }

    @Test
    fun `deterministic - same inputs same output`() {
        val opts = listOf(
            option(1, "gym"),
            option(2, "training gear"),
            option(3, "run shorts"),
        )
        val a = OutfitRecommenderEngine.recommend(opts, Occasions.TRAINING)
        val b = OutfitRecommenderEngine.recommend(opts, Occasions.TRAINING)
        assertEquals(a?.selected?.id, b?.selected?.id)
        assertEquals(a?.explanation, b?.explanation)
    }
}
