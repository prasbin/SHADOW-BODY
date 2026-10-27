package com.shadowbody.app.domain.haircut

import com.shadowbody.app.domain.haircut.HaircutCatalog.Haircut

/**
 * Deterministic advisor over the fixed catalog. Filters by length and
 * maintenance, then ranks by look fit, styling-time proximity and catalog
 * order. No face or hair analysis happens — only the user's stated
 * preferences are used, and the UI says so.
 */
object HaircutAdvisor {

    sealed interface Preferences {
        val length: String?
        val maintenance: String?
        val look: String?
        val maxStylingMinutes: Int?
    }

    data class Input(
        override val length: String? = null,
        override val maintenance: String? = null,
        override val look: String? = null,
        override val maxStylingMinutes: Int? = null,
    ) : Preferences

    data class Advice(
        val rank: Int,
        val haircut: Haircut,
        /** Which of the user's preferences this entry matched, for honest display. */
        val matchedPreferences: List<String>,
        /** True when no preference constrained the result — the full catalog follows. */
        val unfiltered: Boolean,
    )

    fun recommend(input: Preferences): List<Advice> {
        var pool = HaircutCatalog.ALL
        var unfiltered = true

        input.length?.let { length ->
            pool = pool.filter { it.length == length }
            unfiltered = false
        }
        input.maintenance?.let { maintenance ->
            val cap = when (maintenance) {
                "LOW" -> 1 // LOW only
                "MEDIUM" -> 2 // LOW or MEDIUM
                else -> 3
            }
            pool = pool.filter { maintenanceLevel(it.maintenance) <= cap }
            unfiltered = false
        }
        input.maxStylingMinutes?.let { max ->
            val capped = pool.filter { it.stylingMinutes <= max }
            if (capped.isNotEmpty()) {
                pool = capped
                unfiltered = false
            }
        }
        input.look?.let { look ->
            val matching = pool.filter { it.looks.contains(look) }
            if (matching.isNotEmpty()) {
                // Look is a preference, not a constraint: rank matches first
                // but keep practical styles visible when the pool is small.
                pool = matching + pool.filter { !it.looks.contains(look) }
            }
        }
        if (pool.isEmpty()) {
            // Never hand back an empty wall of nothing when a filter is too
            // tight — relax to the full catalog and say so honestly.
            pool = HaircutCatalog.ALL
            unfiltered = true
        }

        return pool
            .mapIndexed { index, haircut ->
                val matched = mutableListOf<String>()
                input.look?.let { look ->
                    if (haircut.looks.contains(look)) matched.add("look")
                }
                input.maxStylingMinutes?.let { max ->
                    if (haircut.stylingMinutes <= max) matched.add("styling time")
                }
                if (input.length == haircut.length) matched.add("length")
                Triple(index, haircut, matched)
            }
            .sortedWith(
                compareBy(
                    { entry -> -entry.third.size },          // most matched preferences first
                    { entry -> entry.second.stylingMinutes }, // then quickest to style
                    { entry -> entry.first },                 // then stable catalog order
                ),
            )
            .mapIndexed { idx, (_, haircut, matched) ->
                Advice(
                    rank = idx + 1,
                    haircut = haircut,
                    matchedPreferences = matched,
                    unfiltered = unfiltered,
                )
            }
    }

    private fun maintenanceLevel(maintenance: String): Int = when (maintenance) {
        "LOW" -> 1
        "MEDIUM" -> 2
        else -> 3
    }
}
