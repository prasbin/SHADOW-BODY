package com.shadowbody.app.domain.skincheck

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Daily key for the two-cheek skin check (Feature B). Same ISO pattern as other features. */
object SkinCheckDayKey {
    private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(FORMATTER)

    fun ofEpochDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(FORMATTER)

    /** True when the string is a valid ISO local date key (guards DB input). */
    fun isValid(key: String): Boolean = runCatching { LocalDate.parse(key, FORMATTER) }.isSuccess
}
