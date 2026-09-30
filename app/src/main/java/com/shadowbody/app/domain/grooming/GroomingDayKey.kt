package com.shadowbody.app.domain.grooming

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object GroomingDayKey {
    private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(FORMATTER)

    fun of(timestamp: Long): String =
        LocalDate.ofEpochDay(timestamp / 86400000).format(FORMATTER)
}