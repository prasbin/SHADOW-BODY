package com.shadowbody.app.ui.profile

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

fun formatRecordedAt(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DATE_FMT)

fun formatKg(value: Double?): String = if (value == null) "—" else "${trimNum(value)} kg"
fun formatCm(value: Double?): String = if (value == null) "—" else "${trimNum(value)} cm"

private fun trimNum(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
