package com.shadowbody.app.domain.outfit

import com.shadowbody.app.data.local.WardrobePhotoCombination

/**
 * Feature C: honest, rule-based daily outfit recommendation.
 *
 * IMPORTANT — what this is NOT:
 * No pixel, colour or garment analysis of the photos is performed anywhere
 * in this class. Selection is based ONLY on the occasion the user picked and
 * keywords present in the option labels/notes the user wrote themselves.
 * Every produced explanation says this explicitly.
 */
object OutfitRecommenderEngine {

    const val NOT_ANALYSED_DISCLAIMER =
        "No image analysis was performed — photos are only displayed."

    /** Occasion keys. Kept small and stable; UI maps them to display names. */
    object Occasions {
        const val TRAINING = "TRAINING"
        const val WORK_FORMAL = "WORK_FORMAL"
        const val SOCIAL_CASUAL = "SOCIAL_CASUAL"
        const val REST_HOME = "REST_HOME"

        val ALL = listOf(TRAINING, WORK_FORMAL, SOCIAL_CASUAL, REST_HOME)

        fun label(occasion: String): String = when (occasion) {
            TRAINING -> "Training / Gym"
            WORK_FORMAL -> "Work / Formal"
            SOCIAL_CASUAL -> "Social / Casual"
            REST_HOME -> "Rest / Home"
            else -> occasion
        }
    }

    /** Keywords matched against the user-written label + notes for each option. */
    private val OCCASION_KEYWORDS: Map<String, List<String>> = mapOf(
        Occasions.TRAINING to listOf("gym", "training", "sport", "sweat", "workout", "active", "run"),
        Occasions.WORK_FORMAL to listOf("formal", "office", "work", "suit", "oxford", "blazer",
            "shirt", "chino", "dress"),
        Occasions.SOCIAL_CASUAL to listOf("casual", "social", "weekend", "friend", "dinner",
            "jeans", "street"),
        Occasions.REST_HOME to listOf("home", "rest", "lazy", "comfort", "recover", "sleep"),
    )

    data class Result(
        val selected: WardrobePhotoCombination,
        val consideredIds: List<Long>,
        val explanation: String,
        /** Always false today: documented honest limitation, surfaced in the UI. */
        val imageAnalysisUsed: Boolean = false,
    )

    /**
     * Returns null (no recommendation) when there are no photographed
     * options — an empty state is honest, never a fabricated pick.
     */
    fun recommend(
        options: List<WardrobePhotoCombination>,
        occasion: String,
    ): Result? {
        val candidates = options.filter { it.photoPath != null }
        if (candidates.isEmpty()) return null

        val keywords = OCCASION_KEYWORDS[occasion].orEmpty()
        val scored = candidates.map { option ->
            val text = "${option.label} ${option.notes}".lowercase()
            OptionScore(option, keywords.count { text.contains(it) })
        }
        // Highest keyword score wins; ties resolve deterministically to the
        // earliest-created option. Nothing else influences the choice.
        val best = scored.maxWith(
            compareBy({ it.matchCount }, { -it.option.createdAt }, { -it.option.id }),
        )
        val consideredIds = scored.map { it.option.id }

        val explanation = if (best.matchCount > 0) {
            val matched = best.option.label
            buildString {
                append("Rule-based pick for ")
                append(Occasions.label(occasion))
                append(": '")
                append(matched)
                append("' has ")
                append(best.matchCount)
                append(if (best.matchCount == 1) " keyword" else " keywords")
                append(" in your own label/notes matching this occasion. ")
                append(NOT_ANALYSED_DISCLAIMER)
            }
        } else {
            buildString {
                append("No label or note for any option mentions '")
                append(Occasions.label(occasion))
                append("' — picked the earliest-added photographed option as a neutral default. ")
                append(NOT_ANALYSED_DISCLAIMER)
            }
        }

        return Result(
            selected = best.option,
            consideredIds = consideredIds,
            explanation = explanation,
        )
    }

    /** True when label/notes mention any keyword for the occasion (used for the preview chips). */
    fun keywordMatches(option: WardrobePhotoCombination, occasion: String): Int {
        val keywords = OCCASION_KEYWORDS[occasion].orEmpty()
        val text = "${option.label} ${option.notes}".lowercase()
        return keywords.count { text.contains(it) }
    }

    private data class OptionScore(
        val option: WardrobePhotoCombination,
        val matchCount: Int,
    )
}
