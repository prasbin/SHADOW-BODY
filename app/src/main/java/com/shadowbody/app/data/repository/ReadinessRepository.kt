package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.ReadinessDao
import com.shadowbody.app.data.local.ReadinessReport
import com.shadowbody.app.domain.adaptive.ReadinessSnapshot
import com.shadowbody.app.domain.validation.AdaptiveValidator
import kotlinx.coroutines.flow.Flow

/**
 * Phase 4: readiness check-ins. Append-only and local; the engine only ever
 * reads the most recent one. Validation happens before anything is stored, so
 * an out-of-range rating can never reach the engine.
 */
class ReadinessRepository(private val reports: ReadinessDao) {

    fun latest(): Flow<ReadinessReport?> = reports.observeLatest()

    fun recent(limit: Int = 10): Flow<List<ReadinessReport>> = reports.observeRecent(limit)

    suspend fun latestOnce(): ReadinessReport? = reports.latest()

    suspend fun count(): Int = reports.count()

    /** Returns the new report id, or null when the input is out of range. */
    suspend fun record(
        fatigue: Int?,
        soreness: Int?,
        notes: String = "",
        now: Long = System.currentTimeMillis(),
    ): Long? {
        val clean = notes.trim()
        if (AdaptiveValidator.validateReadiness(fatigue, soreness, clean).isNotEmpty()) return null
        return reports.insert(
            ReadinessReport(
                recordedAt = now,
                fatigue = fatigue!!,
                soreness = soreness!!,
                notes = clean,
            ),
        )
    }

    companion object {
        fun toSnapshot(report: ReadinessReport?): ReadinessSnapshot? = report?.let {
            ReadinessSnapshot(fatigue = it.fatigue, soreness = it.soreness, notes = it.notes)
        }
    }
}
