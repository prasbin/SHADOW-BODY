package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.SkinCheckDao
import com.shadowbody.app.data.local.SkinCheckMethods
import com.shadowbody.app.data.local.SkinCheckRecord
import com.shadowbody.app.domain.skincheck.SkinCheckDayKey
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Feature B repository. Guarantees:
 *
 * - At most one record per day (unique `dayKey` + upsert flow).
 * - The day is never marked complete until BOTH cheek photos exist.
 * - Replacing a cheek photo deletes the previous private file (no orphans).
 * - Deleting a day deletes both of its photo files before the row.
 * - Photos are only accepted from app-private paths managed by
 *   [PhotoFileOps]; gallery content is copied into private storage first.
 * - No analysis, scoring or "skin condition" values are ever produced.
 */
class SkinCheckRepository(
    private val dao: SkinCheckDao,
    private val fileOps: PhotoFileOps,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    fun observeAll(): Flow<List<SkinCheckRecord>> = dao.observeAll()

    /** Existing record for today, or null when the user has not started one — never auto-creates empty rows. */
    suspend fun today(): SkinCheckRecord? = dao.getByDay(SkinCheckDayKey.today())

    suspend fun getExisting(dayKey: String): SkinCheckRecord? = dao.getByDay(dayKey)

    private suspend fun getOrCreate(dayKey: String): SkinCheckRecord {
        val existing = dao.getByDay(dayKey)
        if (existing != null) return existing
        val created = SkinCheckRecord(
            dayKey = dayKey,
            recordedAt = nowMillis(),
            updatedAt = nowMillis(),
        )
        dao.upsert(created)
        return dao.getByDay(dayKey) ?: created
    }

    /**
     * Attaches a photo copied from a gallery content Uri. Returns the absolute
     * private path, or null when the copy failed.
     */
    suspend fun setCheekFromGallery(
        dayKey: String,
        cheek: Cheek,
        contentUri: android.net.Uri,
    ): String? {
        val copied = fileOps.copyFromUri(contentUri, PrivatePhotoStorage.DIR_SKIN)
            ?: return null
        applyCheek(dayKey, cheek, copied, SkinCheckMethods.GALLERY)
        return copied
    }

    /** Attaches a photo already captured into a private camera file. */
    suspend fun setCheekFromCamera(
        dayKey: String,
        cheek: Cheek,
        cameraFile: File,
    ) {
        applyCheek(dayKey, cheek, cameraFile.absolutePath, SkinCheckMethods.CAMERA)
    }

    private suspend fun applyCheek(
        dayKey: String,
        cheek: Cheek,
        newPath: String,
        method: String,
    ) {
        val record = getOrCreate(dayKey)
        val oldPath = when (cheek) {
            Cheek.LEFT -> record.leftCheekPhotoPath
            Cheek.RIGHT -> record.rightCheekPhotoPath
        }
        val updated = when (cheek) {
            Cheek.LEFT -> record.copy(leftCheekPhotoPath = newPath, leftCheekCaptureMethod = method)
            Cheek.RIGHT -> record.copy(rightCheekPhotoPath = newPath, rightCheekCaptureMethod = method)
        }.copy(updatedAt = nowMillis())
        dao.upsert(updated)
        // Replace = release the previous file immediately (no orphan history).
        if (oldPath != null && oldPath != newPath) {
            fileOps.deleteFileIfInside(oldPath)
        }
    }

    suspend fun updateNotes(dayKey: String, notes: String) {
        val record = getOrCreate(dayKey)
        dao.upsert(record.copy(notes = notes.trim(), updatedAt = nowMillis()))
    }

    suspend fun removeCheek(dayKey: String, cheek: Cheek) {
        val record = getOrCreate(dayKey)
        val updated = when (cheek) {
            Cheek.LEFT -> record.copy(leftCheekPhotoPath = null, leftCheekCaptureMethod = null)
            Cheek.RIGHT -> record.copy(rightCheekPhotoPath = null, rightCheekCaptureMethod = null)
        }.copy(updatedAt = nowMillis())
        dao.upsert(updated)
        val removedPath = when (cheek) {
            Cheek.LEFT -> record.leftCheekPhotoPath
            Cheek.RIGHT -> record.rightCheekPhotoPath
        }
        removedPath?.let { fileOps.deleteFileIfInside(it) }
    }

    /** Deletes the whole day: both photo files first, then the row. */
    suspend fun deleteDay(dayKey: String): Boolean {
        val record = dao.getByDay(dayKey) ?: return false
        record.leftCheekPhotoPath?.let { fileOps.deleteFileIfInside(it) }
        record.rightCheekPhotoPath?.let { fileOps.deleteFileIfInside(it) }
        dao.deleteById(record.id)
        return true
    }

    suspend fun count(): Int = dao.count()

    enum class Cheek { LEFT, RIGHT }
}
