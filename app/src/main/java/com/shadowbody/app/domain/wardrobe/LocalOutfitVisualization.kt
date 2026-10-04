package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobePhotoCombination

class LocalOutfitVisualization : OutfitVisualizationProvider {

    override fun generateVisualization(
        combination: WardrobePhotoCombination,
        onResult: (OutfitVisualizationResult) -> Unit,
    ) {
        if (combination.photoPath == null) {
            onResult(
                OutfitVisualizationResult(
                    success = false,
                    errorMessage = "No photo reference available for this combination.",
                )
            )
            return
        }

        val description = buildString {
            append("OUTFIT VISUALIZATION\n\n")
            append("Combination: ${combination.label}\n")
            append("Photo reference: ${combination.photoPath}\n\n")
            append("This is a text-based visualization description.\n")
            append("Actual image generation is not available in this build.\n")
            append("The visualization shows a generic model wearing the selected outfit.\n")
            append("It does NOT represent the user's exact body, fit, or appearance.\n")
        }

        onResult(
            OutfitVisualizationResult(
                success = true,
                description = description,
                isGeneratedImage = false,
                isTextDescription = true,
            )
        )
    }
}
