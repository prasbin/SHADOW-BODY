package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitGeneratorTest {

    private fun item(
        id: Long,
        name: String,
        category: String,
        color: String = "Black",
        occasion: String = "CASUAL",
        season: String = "ALL_SEASON",
        enabled: Boolean = true,
    ) = WardrobeItem(
        id = id,
        name = name,
        category = category,
        clothingType = category.lowercase(),
        color = color,
        occasion = occasion,
        season = season,
        isEnabled = enabled,
    )

    @Test
    fun generatesCompleteOutfitWhenAllCategoriesAvailable() {
        val items = listOf(
            item(1, "T-Shirt", "TOP"),
            item(2, "Jeans", "BOTTOM"),
            item(3, "Sneakers", "FOOTWEAR"),
            item(4, "Watch", "ACCESSORY"),
        )

        val result = OutfitGenerator.generate(items)

        assertNotNull(result.top)
        assertNotNull(result.bottom)
        assertNotNull(result.footwear)
        assertNotNull(result.accessory)
        assertEquals("T-Shirt", result.top?.name)
        assertEquals("Jeans", result.bottom?.name)
        assertTrue(result.missingCategories.isEmpty())
    }

    @Test
    fun reportsMissingCategoriesWhenInventoryIncomplete() {
        val items = listOf(
            item(1, "T-Shirt", "TOP"),
        )

        val result = OutfitGenerator.generate(items)

        assertNotNull(result.top)
        assertNull(result.bottom)
        assertNull(result.footwear)
        assertTrue(result.missingCategories.contains("BOTTOM"))
        assertTrue(result.missingCategories.contains("FOOTWEAR"))
    }

    @Test
    fun excludesDisabledItems() {
        val items = listOf(
            item(1, "T-Shirt", "TOP", enabled = false),
            item(2, "Jeans", "BOTTOM"),
            item(3, "Sneakers", "FOOTWEAR"),
        )

        val result = OutfitGenerator.generate(items)

        assertNull(result.top)
        assertNotNull(result.bottom)
        assertTrue(result.missingCategories.contains("TOP"))
    }

    @Test
    fun filtersBySeason() {
        val items = listOf(
            item(1, "Summer Shirt", "TOP", season = "SUMMER"),
            item(2, "Winter Jacket", "TOP", season = "WINTER"),
            item(3, "Jeans", "BOTTOM"),
            item(4, "Sneakers", "FOOTWEAR"),
        )

        val result = OutfitGenerator.generate(items, season = "WINTER")

        assertEquals("Winter Jacket", result.top?.name)
    }

    @Test
    fun filtersByOccasion() {
        val items = listOf(
            item(1, "Casual Shirt", "TOP", occasion = "CASUAL"),
            item(2, "Work Shirt", "TOP", occasion = "WORK"),
            item(3, "Jeans", "BOTTOM"),
            item(4, "Sneakers", "FOOTWEAR"),
        )

        val result = OutfitGenerator.generate(items, occasion = "WORK")

        assertEquals("Work Shirt", result.top?.name)
    }

    @Test
    fun allSeasonItemsMatchAnySeason() {
        val items = listOf(
            item(1, "Basic Tee", "TOP", season = "ALL_SEASON"),
            item(2, "Jeans", "BOTTOM"),
            item(3, "Sneakers", "FOOTWEAR"),
        )

        val result = OutfitGenerator.generate(items, season = "SUMMER")

        assertEquals("Basic Tee", result.top?.name)
    }

    @Test
    fun emptyInventoryProducesEmptySuggestion() {
        val result = OutfitGenerator.generate(emptyList())

        assertNull(result.top)
        assertNull(result.bottom)
        assertNull(result.footwear)
        assertNull(result.accessory)
        assertEquals(3, result.missingCategories.size)
    }

    @Test
    fun explanationContainsSelectionDetails() {
        val items = listOf(
            item(1, "T-Shirt", "TOP", color = "Blue"),
            item(2, "Jeans", "BOTTOM", color = "Black"),
            item(3, "Sneakers", "FOOTWEAR", color = "White"),
        )

        val result = OutfitGenerator.generate(items)

        assertTrue(result.explanation.contains("T-Shirt"))
        assertTrue(result.explanation.contains("Jeans"))
        assertTrue(result.explanation.contains("Sneakers"))
        assertTrue(result.explanation.contains("CASUAL"))
    }
}
