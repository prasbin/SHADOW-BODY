package com.shadowbody.app.domain.morning

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Phase 5 calendar day key ("today") for a timestamp.
 *
 * The routine is a *local* morning habit, so the day boundary is the user's
 * own timezone rather than UTC. The key is plain ISO-8601 (`2026-09-26`) so it
 * sorts chronologically as text and is trivially comparable in SQL.
 */
object MorningDayKey {

    fun of(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()

    fun today(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
        of(now, zone)

    /** Parses a key produced by [of]. Returns null for anything else. */
    fun parse(key: String): LocalDate? = runCatching { LocalDate.parse(key) }.getOrNull()
}
