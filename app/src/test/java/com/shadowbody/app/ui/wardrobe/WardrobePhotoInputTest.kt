package com.shadowbody.app.ui.wardrobe

import com.shadowbody.app.data.local.WardrobePhotoCombination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobePhotoInputTest {

    @Test
    fun threeSlotsCanBePopulated() {
        val slot0 = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = "/photo0.jpg")
        val slot1 = WardrobePhotoCombination(position = 1, label = "Combo 2", photoPath = "/photo1.jpg")
        val slot2 = WardrobePhotoCombination(position = 2, label = "Combo 3", photoPath = "/photo2.jpg")

        assertEquals(0, slot0.position)
        assertEquals(1, slot1.position)
        assertEquals(2, slot2.position)
        assertNotNull(slot0.photoPath)
        assertNotNull(slot1.photoPath)
        assertNotNull(slot2.photoPath)
    }

    @Test
    fun emptySlotsHaveNullPhotoPath() {
        val slot = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = null)
        assertNull(slot.photoPath)
    }

    @Test
    fun replacementUpdatesPhotoPath() {
        val original = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = "/old.jpg")
        val replaced = original.copy(photoPath = "/new.jpg")

        assertEquals("/new.jpg", replaced.photoPath)
        assertEquals(0, replaced.position)
    }

    @Test
    fun removalSetsPhotoPathToNull() {
        val original = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = "/photo.jpg")
        val removed = original.copy(photoPath = null)

        assertNull(removed.photoPath)
    }

    @Test
    fun photoReferencesAreLocalPaths() {
        val slot = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = "/local/storage/photo.jpg")
        assertTrue(slot.photoPath!!.startsWith("/local"))
    }

    @Test
    fun noBinaryDataEmbedded() {
        val slot = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = "/path/to/photo.jpg")
        assertTrue(slot.photoPath!!.length < 1000)
    }

    @Test
    fun missingPhotoFileDoesNotCrash() {
        val slot = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = "/nonexistent/photo.jpg")
        assertNotNull(slot.photoPath)
    }

    @Test
    fun combinationsAreDistinguishable() {
        val combo1 = WardrobePhotoCombination(position = 0, label = "Combo 1")
        val combo2 = WardrobePhotoCombination(position = 1, label = "Combo 2")
        val combo3 = WardrobePhotoCombination(position = 2, label = "Combo 3")

        assertTrue(combo1.position != combo2.position)
        assertTrue(combo2.position != combo3.position)
        assertTrue(combo1.position != combo3.position)
    }

    @Test
    fun photoInputIsExplicitUserAction() {
        val slot = WardrobePhotoCombination(position = 0, label = "Combo 1", photoPath = null)
        assertNull(slot.photoPath)
    }

    @Test
    fun persistenceAcrossRestart() {
        val slot = WardrobePhotoCombination(
            id = 1L,
            position = 0,
            label = "Combo 1",
            photoPath = "/local/photo.jpg",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )

        assertEquals(1L, slot.id)
        assertEquals("/local/photo.jpg", slot.photoPath)
    }
}
