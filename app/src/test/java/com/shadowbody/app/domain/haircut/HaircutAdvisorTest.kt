package com.shadowbody.app.domain.haircut

import com.shadowbody.app.domain.haircut.HaircutAdvisor.Input
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feature D advisor/catalog tests. */
class HaircutAdvisorTest {

    private val input = Input()

    @Test
    fun `unfiltered returns the full catalog deterministically`() {
        val result = HaircutAdvisor.recommend(input)
        assertEquals(HaircutCatalog.ALL.size, result.size)
        assertTrue(result.all { it.unfiltered })
        assertEquals((1..result.size).toList(), result.map { it.rank })
    }

    @Test
    fun `length filter restricts to matching entries`() {
        val result = HaircutAdvisor.recommend(input.copy(length = "SHORT"))
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.haircut.length == "SHORT" })
    }

    @Test
    fun `low maintenance never suggests high-maintenance styles`() {
        val result = HaircutAdvisor.recommend(input.copy(maintenance = "LOW"))
        assertTrue(result.all { it.haircut.maintenance == "LOW" })
    }

    @Test
    fun `styling time cap filters entries over budget`() {
        val result = HaircutAdvisor.recommend(input.copy(maxStylingMinutes = 3))
        assertTrue(result.all { it.haircut.stylingMinutes <= 3 })
    }

    @Test
    fun `unmatched preferences score zero - unfiltered catalog still shown`() {
        val result = HaircutAdvisor.recommend(input.copy(look = "PROFESSIONAL"))
        // CASUAL-only catalog entries must still appear, but ranked behind matches.
        val matched = result.filter { it.matchedPreferences.contains("look") }
        val unmatched = result.filter { !it.matchedPreferences.contains("look") }
        assertTrue(matched.isNotEmpty())
        assertTrue(unmatched.isNotEmpty())
        // No PROFESSIONAL-matching entry appears after any unmatched entry.
        val firstUnmatchedRank = unmatched.minOf { it.rank }
        assertTrue(matched.all { it.rank < firstUnmatchedRank })
    }

    @Test
    fun `tight filters relax to full catalog instead of empty result`() {
        val result = HaircutAdvisor.recommend(
            Input(length = "SHORT", maxStylingMinutes = 3),
        )
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `every catalog entry has complete barber guidance and honest visual state`() {
        HaircutCatalog.ALL.forEach { haircut ->
            assertTrue(haircut.name.isNotBlank())
            assertTrue(haircut.barberInstructions.isNotBlank())
            assertTrue(haircut.maintenanceGuidance.isNotBlank())
            assertTrue(haircut.description.isNotBlank())
            assertFalse("No entry may claim a bundled visual example", haircut.hasVisualExample)
            assertTrue(haircut.visualExampleNote.isNotBlank())
        }
    }

    @Test
    fun `catalog ids are unique`() {
        val ids = HaircutCatalog.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `byId resolves and unknown id is null`() {
        assertNotNull(HaircutCatalog.byId("crew_cut"))
        assertEquals(null, HaircutCatalog.byId("nonexistent"))
    }

    @Test
    fun `advisor never references analysis claims`() {
        HaircutCatalog.ALL.forEach {
            val text = "${it.description} ${it.maintenanceGuidance} ${it.visualExampleNote}".lowercase()
            assertFalse(text.contains("analysed your face"))
            assertFalse(text.contains("analyzed your face"))
        }
    }
}
