package com.shadowbody.app.data.repository

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.data.local.SkinCheckRecord
import com.shadowbody.app.data.local.isComplete
import com.shadowbody.app.domain.skincheck.SkinCheckDayKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Feature B repository tests: daily uniqueness, completion rules, file
 * lifecycle (replace deletes previous, delete-day removes both files).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SkinCheckRepositoryTest {

private class Bundle {
    val ops = FakePhotoFileOps(
        java.io.File(System.getProperty("java.io.tmpdir"), "skin-test-${System.nanoTime()}"),
    )
    val repository = SkinCheckRepository(
        dao = FakeSkinCheckDao(),
        fileOps = ops,
        nowMillis = { 1_700_000_000_000L },
    )
}

private fun bundle() = Bundle()

    @Test
    fun `today is lazy - no empty rows created by reads`() = runTest {
        val b = bundle()
        assertNull(b.repository.today())
        assertEquals(0, b.repository.count())
    }

    @Test
    fun `attaching camera photo creates the day record`() = runTest {
        val b = bundle()
        val file = b.ops.newCameraFile("d", "left_cheek")
        b.repository.setCheekFromCamera("2026-01-01", SkinCheckRepository.Cheek.LEFT, file)
        val record = b.repository.getExisting("2026-01-01")
        assertNotNull(record)
        assertEquals(file.absolutePath, record?.leftCheekPhotoPath)
        assertEquals("CAMERA", record?.leftCheekCaptureMethod)
        assertFalse(record?.isComplete == true)
    }

    @Test
    fun `day completes only when both cheeks exist`() = runTest {
        val b = bundle()
        val day = "2026-01-02"
        b.repository.setCheekFromCamera(day, SkinCheckRepository.Cheek.LEFT, b.ops.newCameraFile("d", "left_cheek"))
        assertFalse(b.repository.getExisting(day)!!.isComplete)
        b.repository.setCheekFromCamera(day, SkinCheckRepository.Cheek.RIGHT, b.ops.newCameraFile("d", "right_cheek"))
        assertTrue(b.repository.getExisting(day)!!.isComplete)
    }

    @Test
    fun `replacing a cheek deletes the previous file`() = runTest {
        val b = bundle()
        val day = "2026-01-03"
        val first = b.ops.newCameraFile("d", "left_cheek")
        b.repository.setCheekFromCamera(day, SkinCheckRepository.Cheek.LEFT, first)
        val second = b.ops.newCameraFile("d", "left_cheek")
        b.repository.setCheekFromCamera(day, SkinCheckRepository.Cheek.LEFT, second)
        assertFalse(java.io.File(first.absolutePath).exists())
        assertTrue(java.io.File(second.absolutePath).exists())
        assertEquals(second.absolutePath, b.repository.getExisting(day)?.leftCheekPhotoPath)
    }

    @Test
    fun `gallery copy lands in private storage and is tracked`() = runTest {
        val b = bundle()
        val day = "2026-01-04"
        val path = b.repository.setCheekFromGallery(
            day,
            SkinCheckRepository.Cheek.RIGHT,
            Uri.parse("test://pick/image1"),
        )
        assertNotNull(path)
        assertTrue(path!!.startsWith(b.ops.dir.canonicalPath))
        assertEquals(path, b.repository.getExisting(day)?.rightCheekPhotoPath)
        assertEquals("GALLERY", b.repository.getExisting(day)?.rightCheekCaptureMethod)
    }

    @Test
    fun `gallery rejects unreadable content honestly`() = runTest {
        val b = bundle()
        val path = b.repository.setCheekFromGallery(
            "2026-01-05",
            SkinCheckRepository.Cheek.LEFT,
            Uri.parse("file://not-supported"),
        )
        assertNull(path)
    }

    @Test
    fun `deleteDay removes both photo files then the row`() = runTest {
        val b = bundle()
        val day = "2026-01-06"
        val left = b.ops.newCameraFile("d", "left_cheek")
        val right = b.ops.newCameraFile("d", "right_cheek")
        b.repository.setCheekFromCamera(day, SkinCheckRepository.Cheek.LEFT, left)
        b.repository.setCheekFromCamera(day, SkinCheckRepository.Cheek.RIGHT, right)
        val deleted = b.repository.deleteDay(day)
        assertTrue(deleted)
        assertFalse(java.io.File(left.absolutePath).exists())
        assertFalse(java.io.File(right.absolutePath).exists())
        assertNull(b.repository.getExisting(day))
    }

    @Test
    fun `deleting unknown day reports failure`() = runTest {
        val b = bundle()
        assertFalse(b.repository.deleteDay("1999-12-31"))
    }

    @Test
    fun `removing one cheek clears its path and deletes its file`() = runTest {
        val b = bundle()
        val day = "2026-01-07"
        val left = b.ops.newCameraFile("d", "left_cheek")
        b.repository.setCheekFromCamera(day, SkinCheckRepository.Cheek.LEFT, left)
        b.repository.removeCheek(day, SkinCheckRepository.Cheek.LEFT)
        val record = b.repository.getExisting(day)
        assertNull(record?.leftCheekPhotoPath)
        assertFalse(java.io.File(left.absolutePath).exists())
    }

    @Test
    fun `notes are trimmed and stored`() = runTest {
        val b = bundle()
        val day = "2026-01-08"
        b.repository.updateNotes(day, "  slight redness on left  ")
        assertEquals("slight redness on left", b.repository.getExisting(day)?.notes)
    }

    @Test
    fun `dayKey format validation`() {
        assertTrue(SkinCheckDayKey.isValid("2026-01-01"))
        assertFalse(SkinCheckDayKey.isValid("not-a-date"))
        assertFalse(SkinCheckDayKey.isValid(""))
    }

    @Test
    fun `no analysis fields exist on the record`() {
        // Structural honesty check: the record has no analysis/score columns.
        val record = SkinCheckRecord(dayKey = "2026-01-09", recordedAt = 0, updatedAt = 0)
        val fields = record.javaClass.declaredFields.map { it.name }
        assertTrue(fields.none { it.contains("score", ignoreCase = true) })
        assertTrue(fields.none { it.contains("condition", ignoreCase = true) })
        assertTrue(fields.none { it.contains("analysis", ignoreCase = true) })
        assertTrue(fields.none { it.contains("confidence", ignoreCase = true) })
    }
}
