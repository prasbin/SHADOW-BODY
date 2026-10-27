package com.shadowbody.app.domain.outfit

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Daily key for the persisted outfit decision (Feature C). */
object OutfitDayKey {
    private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(FORMATTER)
}
