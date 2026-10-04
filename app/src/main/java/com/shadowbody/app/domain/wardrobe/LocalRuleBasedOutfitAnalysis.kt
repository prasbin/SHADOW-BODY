package com.shadowbody.app.domain.wardrobe

import com.shadowbody.app.data.local.WardrobePhotoCombination

class LocalRuleBasedOutfitAnalysis : OutfitAnalysisProvider {

    override fun analyze(combinations: List<WardrobePhotoCombination>): OutfitAnalysisResult {
        val withPhotos = combinations.filter { it.photoPath != null }
        val withoutPhotos = combinations.filter { it.photoPath == null }

        if (withPhotos.isEmpty()) {
            return OutfitAnalysisResult(
                recommendedPosition = -1,
                recommendedLabel = "None",
                reasoning = "No combinations have photos yet. Add photos to enable analysis.",
                strengths = emptyList(),
                issues = listOf("No photo references available"),
                isVisualAnalysis = false,
                isRuleBased = true,
            )
        }

        val recommended = withPhotos.firstOrNull { it.position == 0 }
            ?: withPhotos.firstOrNull { it.position == 1 }
            ?: withPhotos.first()

        val strengths = mutableListOf<String>()
        val issues = mutableListOf<String>()

        strengths.add("Combination ${recommended.position + 1} has a photo reference.")

        if (recommended.notes.isNotBlank()) {
            strengths.add("Has descriptive notes: ${recommended.notes}")
        }

        if (withPhotos.size >= 2) {
            strengths.add("${withPhotos.size} combinations available for comparison.")
        }

        if (withoutPhotos.isNotEmpty()) {
            issues.add("${withoutPhotos.size} combination(s) missing photos.")
        }

        if (withPhotos.size < 3) {
            issues.add("Only ${withPhotos.size}/3 combinations have photos.")
        }

        val reasoning = buildString {
            append("Recommended: Combination ${recommended.position + 1}. ")
            append("Selected because it has a photo reference")
            if (recommended.notes.isNotBlank()) {
                append(" and descriptive notes")
            }
            append(". ")
            if (withPhotos.size > 1) {
                append("Other combinations were compared. ")
            }
            append("This is rule-based analysis, not visual AI analysis.")
        }

        return OutfitAnalysisResult(
            recommendedPosition = recommended.position,
            recommendedLabel = recommended.label,
            reasoning = reasoning,
            strengths = strengths,
            issues = issues,
            isVisualAnalysis = false,
            isRuleBased = true,
        )
    }
}
