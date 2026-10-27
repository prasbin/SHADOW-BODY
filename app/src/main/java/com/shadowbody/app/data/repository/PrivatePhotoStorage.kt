package com.shadowbody.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * File lifecycle operations for app-private photo storage. Abstracted so
 * repositories stay unit-testable with an in-memory fake.
 */
interface PhotoFileOps {
    /** Copies a gallery/content Uri into private storage. Returns the absolute path or null on failure. */
    fun copyFromUri(uri: Uri, subDir: String): String?

    /** Creates an empty target file for a camera capture in private storage. */
    fun newCameraFile(subDir: String, fileNameStem: String): File

    /** Deletes a private photo file by absolute path. Returns true when deleted (or absent). */
    fun deleteFileIfInside(path: String): Boolean

    /** True when the path exists inside private storage (guards history display). */
    fun exists(path: String): Boolean
}

/**
 * Owns all feature photos under the app-private `filesDir/<subDir>`:
 *
 * - `skin_cheek_photos`  — Feature B daily two-cheek photos
 * - `outfit_photos`      — Feature C photographed outfit options
 *
 * Files never leave app-private storage: no MediaStore, no public
 * directories, no synchronisation, no uploads (the app has no INTERNET
 * permission). Only paths inside `filesDir` are ever deleted by this class.
 */
class PrivatePhotoStorage(
    private val filesDir: File,
    private val contentResolver: ContentResolver,
) : PhotoFileOps {

    constructor(context: Context) : this(
        context.applicationContext.filesDir,
        context.applicationContext.contentResolver,
    )

    private fun dir(subDir: String): File {
        val safe = subDir.replace(File.separatorChar, '_').replace("..", "_")
        return File(filesDir, safe).apply { if (!exists()) mkdirs() }
    }

    private fun isInsidePrivateDir(file: File): Boolean = runCatching {
        file.canonicalPath.startsWith(filesDir.canonicalPath + File.separator)
    }.getOrDefault(false)

    private fun fileNameFor(stem: String): String {
        val safeStem = stem.filter { it.isLetterOrDigit() || it == '-' }.ifEmpty { "photo" }
        return "${safeStem}_${System.currentTimeMillis()}.jpg"
    }

    override fun copyFromUri(uri: Uri, subDir: String): String? = runCatching {
        resolverStream(uri)?.use { input -> copyToPrivateFile(input, subDir) }
    }.getOrNull()

    private fun resolverStream(uri: Uri): InputStream? = try {
        contentResolver.openInputStream(uri)
    } catch (_: SecurityException) {
        null
    }

    private fun copyToPrivateFile(input: InputStream, subDir: String): String? {
        val target = File(dir(subDir), fileNameFor("gallery"))
        target.outputStream().use { output -> input.copyTo(output) }
        if (target.length() == 0L) {
            target.delete()
            return null
        }
        return target.absolutePath
    }

    override fun newCameraFile(subDir: String, fileNameStem: String): File =
        File(dir(subDir), fileNameFor(fileNameStem)).apply { createNewFile() }

    override fun deleteFileIfInside(path: String): Boolean = runCatching {
        val file = File(path)
        if (!isInsidePrivateDir(file)) return false
        if (!file.exists()) return true
        file.delete()
    }.getOrDefault(false)

    override fun exists(path: String): Boolean = runCatching {
        val file = File(path)
        isInsidePrivateDir(file) && file.exists()
    }.getOrDefault(false)

    companion object {
        const val DIR_SKIN = "skin_cheek_photos"
        const val DIR_OUTFIT = "outfit_photos"
    }
}
