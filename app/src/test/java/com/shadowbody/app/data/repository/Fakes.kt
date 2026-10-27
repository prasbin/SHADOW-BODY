package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.SkinCheckDao
import com.shadowbody.app.data.local.SkinCheckRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory SkinCheckDao fake honouring the unique-dayKey semantics. */
class FakeSkinCheckDao : SkinCheckDao {
    private val rows = LinkedHashMap<Long, SkinCheckRecord>()
    private val flow = MutableStateFlow(0L)

    override fun observeAll(): Flow<List<SkinCheckRecord>> = flow.map {
        rows.values.sortedByDescending { it.dayKey }
    }

    override suspend fun getByDay(dayKey: String): SkinCheckRecord? =
        rows.values.firstOrNull { it.dayKey == dayKey }

    override suspend fun upsert(record: SkinCheckRecord): Long {
        val existing = rows.values.firstOrNull { it.dayKey == record.dayKey }
        val id = if (existing != null) {
            rows.remove(existing.id)
            existing.id
        } else {
            (rows.keys.maxOrNull() ?: 0L) + 1
        }
        val stored = if (record.id == 0L) record.copy(id = id) else record
        rows[stored.id] = stored
        flow.value += 1
        return stored.id
    }

    override suspend fun deleteById(id: Long) {
        rows.remove(id)
        flow.value += 1
    }

    override suspend fun count(): Int = rows.size
}

/** Minimal real-file fake — behaves like PrivatePhotoStorage but under a JVM temp dir. */
class FakePhotoFileOps(root: java.io.File) : PhotoFileOps {
    val dir = java.io.File(root, "skin_cheek_photos").apply { mkdirs() }
    val created = mutableSetOf<String>()

    override fun copyFromUri(uri: android.net.Uri, subDir: String): String? =
        if (uri.scheme == "test") {
            java.io.File(dir, "gallery-${System.nanoTime()}.jpg").apply {
                createNewFile()
                writeText("copied bytes")
            }.absolutePath.also { created.add(it) }
        } else {
            null
        }

    override fun newCameraFile(subDir: String, fileNameStem: String): java.io.File =
        java.io.File(dir, "$fileNameStem-${System.nanoTime()}.jpg").apply {
            createNewFile()
            writeText(" pretend jpeg bytes ")
            created.add(absolutePath)
        }

    override fun deleteFileIfInside(path: String): Boolean = runCatching {
        val file = java.io.File(path)
        if (!file.canonicalPath.startsWith(dir.canonicalPath)) return false
        if (!file.exists()) return true
        file.delete()
    }.getOrDefault(false)

    override fun exists(path: String): Boolean =
        java.io.File(path).exists() && path.startsWith(dir.canonicalPath)

    fun seedGalleryFile(name: String): String =
        java.io.File(dir, name).apply {
            createNewFile()
            writeText("gallery bytes")
        }.absolutePath
}
