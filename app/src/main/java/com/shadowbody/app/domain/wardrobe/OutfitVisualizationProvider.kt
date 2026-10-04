package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobePhotoCombination

data class OutfitVisualizationResult(
    val success: Boolean,
    val imagePath: String? = null,
    val description: String? = null,
    val isGeneratedImage: Boolean = false,
    val isTextDescription: Boolean = false,
    val errorMessage: String? = null,
)

interface OutfitVisualizationProvider {
    fun generateVisualization(
        combination: WardrobePhotoCombination,
        onResult: (OutfitVisualizationResult) -> Unit,
    )
}
