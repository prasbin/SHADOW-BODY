package com.shadowbody.app.domain.nutrition

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NutritionDayKey {
    private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(FORMATTER)
}
