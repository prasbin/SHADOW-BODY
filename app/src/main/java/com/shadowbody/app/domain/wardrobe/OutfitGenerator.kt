package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobeItem

data class OutfitSuggestion(
    val top: WardrobeItem?,
    val bottom: WardrobeItem?,
    val footwear: WardrobeItem?,
    val accessory: WardrobeItem?,
    val explanation: String,
    val missingCategories: List<String>,
)

object OutfitGenerator {

    fun generate(
        items: List<WardrobeItem>,
        occasion: String = "CASUAL",
        season: String = "ALL_SEASON",
    ): OutfitSuggestion {
        val enabled = items.filter { it.isEnabled }
        val missing = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        val tops = enabled.filter { it.category == "TOP" }
        val bottoms = enabled.filter { it.category == "BOTTOM" }
        val footwear = enabled.filter { it.category == "FOOTWEAR" }
        val accessories = enabled.filter { it.category == "ACCESSORY" }

        val selectedTop = selectByOccasionAndSeason(tops, occasion, season)
        val selectedBottom = selectByOccasionAndSeason(bottoms, occasion, season)
        val selectedFootwear = selectByOccasionAndSeason(footwear, occasion, season)
        val selectedAccessory = selectByOccasionAndSeason(accessories, occasion, season)

        if (selectedTop == null) missing.add("TOP")
        if (selectedBottom == null) missing.add("BOTTOM")
        if (selectedFootwear == null) missing.add("FOOTWEAR")

        explanations.add("Occasion: $occasion")
        explanations.add("Season: $season")
        explanations.add("Available items: ${enabled.size}")

        if (selectedTop != null) explanations.add("Top: ${selectedTop.name} (${selectedTop.color})")
        if (selectedBottom != null) explanations.add("Bottom: ${selectedBottom.name} (${selectedBottom.color})")
        if (selectedFootwear != null) explanations.add("Footwear: ${selectedFootwear.name} (${selectedFootwear.color})")
        if (selectedAccessory != null) explanations.add("Accessory: ${selectedAccessory.name}")

        if (missing.isNotEmpty()) {
            explanations.add("Missing: ${missing.joinToString(", ")}")
        }

        return OutfitSuggestion(
            top = selectedTop,
            bottom = selectedBottom,
            footwear = selectedFootwear,
            accessory = selectedAccessory,
            explanation = explanations.joinToString("; "),
            missingCategories = missing,
        )
    }

    private fun selectByOccasionAndSeason(
        items: List<WardrobeItem>,
        occasion: String,
        season: String,
    ): WardrobeItem? {
        if (items.isEmpty()) return null

        val seasonMatches = items.filter { it.season == season || it.season == "ALL_SEASON" }
        val pool = seasonMatches.ifEmpty { items }

        val occasionMatches = pool.filter { it.occasion == occasion }
        val finalPool = occasionMatches.ifEmpty { pool }

        return finalPool.firstOrNull()
    }
}
