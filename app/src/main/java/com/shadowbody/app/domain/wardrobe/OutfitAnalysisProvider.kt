package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobePhotoCombination

data class OutfitAnalysisResult(
    val recommendedPosition: Int,
    val recommendedLabel: String,
    val reasoning: String,
    val strengths: List<String>,
    val issues: List<String>,
    val isVisualAnalysis: Boolean,
    val isRuleBased: Boolean,
)

interface OutfitAnalysisProvider {
    fun analyze(combinations: List<WardrobePhotoCombination>): OutfitAnalysisResult
}
