package com.shadowbody.app.domain.model

/** Available workout equipment. NONE is exclusive (see ProfileValidator). */
enum class Equipment(val label: String) {
    NONE("None"),
    BODYWEIGHT("Bodyweight"),
    DUMBBELLS("Dumbbells"),
    RESISTANCE_BANDS("Resistance bands"),
    PULL_UP_BAR("Pull-up bar"),
    BENCH("Bench"),
    BARBELL("Barbell"),
    OTHER("Other"),
}
