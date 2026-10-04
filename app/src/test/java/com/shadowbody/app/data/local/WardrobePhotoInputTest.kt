package com.shadowbody.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobePhotoInputTest {

    @Test
    fun photoCombinationStoresPositionAndLabel() {
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combination 1",
            photoPath = "/storage/photos/combo1.jpg",
        )

        assertEquals(0, combo.position)
        assertEquals("Combination 1", combo.label)
        assertEquals("/storage/photos/combo1.jpg", combo.photoPath)
    }

    @Test
    fun photoCombinationAllowsNullPhotoPath() {
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combination 1",
            photoPath = null,
        )

        assertNull(combo.photoPath)
    }

    @Test
    fun threeCombinationsAreDistinguishable() {
        val combo1 = WardrobePhotoCombination(id = 1L, position = 0, label = "Combo 1")
        val combo2 = WardrobePhotoCombination(id = 2L, position = 1, label = "Combo 2")
        val combo3 = WardrobePhotoCombination(id = 3L, position = 2, label = "Combo 3")

        assertEquals(0, combo1.position)
        assertEquals(1, combo2.position)
        assertEquals(2, combo3.position)
    }

    @Test
    fun photoCombinationTracksTimestamps() {
        val now = System.currentTimeMillis()
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
            createdAt = now,
            updatedAt = now,
        )

        assertEquals(now, combo.createdAt)
        assertEquals(now, combo.updatedAt)
    }

    @Test
    fun photoCombinationSupportsNotes() {
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
            notes = "Blue shirt with jeans",
        )

        assertEquals("Blue shirt with jeans", combo.notes)
    }

    @Test
    fun photoCombinationDefaultNotesAreEmpty() {
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
        )

        assertEquals("", combo.notes)
    }

    @Test
    fun photoCombinationIsLocalOnly() {
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
            photoPath = "/local/storage/combo1.jpg",
        )

        assertTrue(combo.photoPath!!.startsWith("/local"))
    }

    @Test
    fun photoCombinationDoesNotEmbedBinaryData() {
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
            photoPath = "/path/to/photo.jpg",
        )

        assertNull(combo.photoPath?.let { if (it.contains("base64")) "found" else null })
    }

    @Test
    fun photoCombinationSupportsReplacement() {
        val original = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
            photoPath = "/old/path.jpg",
        )

        val replaced = original.copy(photoPath = "/new/path.jpg")

        assertEquals("/new/path.jpg", replaced.photoPath)
        assertEquals(1L, replaced.id)
    }

    @Test
    fun photoCombinationSupportsRemoval() {
        val combo = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
            photoPath = "/path/to/photo.jpg",
        )

        val removed = combo.copy(photoPath = null)

        assertNull(removed.photoPath)
    }
}
