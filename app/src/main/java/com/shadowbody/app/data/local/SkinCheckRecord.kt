package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Feature B: daily two-cheek skin photo record.
 *
 * One row per calendar day (dayKey unique - duplicate daily records are
 * impossible). Cheek photo paths are nullable so a day can track progress:
 * the day is complete only when both paths are non-null. Photos live in
 * app-private storage; they are never uploaded. No column ever contains
 * analysis results: SHADOW BODY performs no skin analysis, it only stores
 * and compares photos.
 */
@Entity(
    tableName = "skin_check_record",
    indices = [Index(value = ["dayKey"], unique = true)],
)
data class SkinCheckRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Local date in ISO format, e.g. "2026-10-27". */
    val dayKey: String,
    val leftCheekPhotoPath: String? = null,
    /** How the left photo was supplied: "CAMERA" or "GALLERY". */
    val leftCheekCaptureMethod: String? = null,
    val rightCheekPhotoPath: String? = null,
    /** How the right photo was supplied: "CAMERA" or "GALLERY". */
    val rightCheekCaptureMethod: String? = null,
    /** Free-text observation notes written by the user - never generated. */
    val notes: String = "",
    val recordedAt: Long,
    val updatedAt: Long,
)
