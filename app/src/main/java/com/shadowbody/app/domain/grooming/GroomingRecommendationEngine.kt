package com.shadowbody.app.domain.grooming

import java.util.Calendar

data class GroomingRecommendation(
    val title: String,
    val category: String,
    val instruction: String,
    val reason: String,
    val safetyNote: String? = null,
)

object GroomingRecommendationEngine {

    fun generate(dayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)): GroomingRecommendation {
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        return when {
            isWeekend -> GroomingRecommendation(
                title = "Recovery Grooming",
                category = "RECOVERY",
                instruction = "Gentle cleanse, moisturize, and apply sunscreen if going outdoors.",
                reason = "Weekend recovery day. Focus on skin rest and hydration.",
            )
            dayOfWeek == Calendar.MONDAY -> GroomingRecommendation(
                title = "Deep Cleanse",
                category = "CLEANSING",
                instruction = "Use a gentle cleanser. Follow with moisturizer.",
                reason = "Start of week. Remove accumulated oil and impurities.",
            )
            dayOfWeek == Calendar.TUESDAY -> GroomingRecommendation(
                title = "Hydration Focus",
                category = "MOISTURIZING",
                instruction = "Apply moisturizer to damp skin. Use sunscreen if daytime.",
                reason = "Mid-week hydration maintenance.",
            )
            dayOfWeek == Calendar.WEDNESDAY -> GroomingRecommendation(
                title = "Sun Protection",
                category = "SUNSCREEN",
                instruction = "Apply SPF 30+ sunscreen. Reapply every 2 hours if outdoors.",
                reason = "Mid-week sun protection reminder.",
            )
            dayOfWeek == Calendar.THURSDAY -> GroomingRecommendation(
                title = "Basic Grooming",
                category = "GROOMING",
                instruction = "Brush teeth, cleanse face, apply moisturizer.",
                reason = "Standard daily grooming maintenance.",
            )
            dayOfWeek == Calendar.FRIDAY -> GroomingRecommendation(
                title = "Pre-Weekend Prep",
                category = "GROOMING",
                instruction = "Cleanse, moisturize, and prepare skin for the weekend.",
                reason = "End of week grooming preparation.",
            )
            else -> GroomingRecommendation(
                title = "Daily Grooming",
                category = "GROOMING",
                instruction = "Cleanse, moisturize, and apply sunscreen.",
                reason = "Standard daily grooming.",
            )
        }
    }

    fun getSafetyNote(): String {
        return "This is grooming/self-care guidance only. It is not medical advice. " +
            "Consult a qualified professional for skin concerns."
    }
}
