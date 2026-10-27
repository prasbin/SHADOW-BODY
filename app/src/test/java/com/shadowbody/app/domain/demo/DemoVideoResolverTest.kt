package com.shadowbody.app.domain.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feature A: deterministic external-video resolution (no invented video ids). */
class DemoVideoResolverTest {

    @Test
    fun `resolver builds search url for a normal exercise name`() {
        val demo = DemoVideoResolver.forExercise("Bench Press")
        assertNotNull(demo)
        assertEquals("Bench Press", demo?.exerciseName)
        assertTrue(demo?.url?.startsWith("https://www.youtube.com/results?search_query=") == true)
        assertTrue(demo?.url?.contains("Bench%20Press") == true)
        assertTrue(demo?.url?.contains("proper%20form") == true)
    }

    @Test
    fun `resolver encodes special characters safely`() {
        val demo = DemoVideoResolver.forExercise("Dips (weighted)")!!
        val url = demo.url
        assertFalse(url.contains("("))
        assertFalse(url.contains(")"))
        // Encoded space of some form: %20 (or + handled by the resolver normally).
        assertTrue(url.contains("%20") || url.contains("+"))
        assertTrue(url.startsWith("https://"))
    }

    @Test
    fun `blank or whitespace name has no demo - honest unavailable`() {
        assertNull(DemoVideoResolver.forExercise(""))
        assertNull(DemoVideoResolver.forExercise("   "))
    }

    @Test
    fun `every demo is labelled external and never claims a verified video`() {
        val names = listOf("Squat", "Pull-Up", "Hip Thrust")
        names.forEach { name ->
            val demo = DemoVideoResolver.forExercise(name)!!
            assertFalse(demo.verifiedVideo)
            assertTrue(demo.requiresInternet)
            assertTrue(demo.provider.contains("external") || demo.provider.contains("External"))
            assertTrue(demo.attribution.isNotBlank())
        }
    }

    @Test
    fun `same input yields same url - deterministic`() {
        val a = DemoVideoResolver.forExercise("Romanian Deadlift")!!
        val b = DemoVideoResolver.forExercise("Romanian Deadlift")!!
        assertEquals(a.url, b.url)
        assertEquals(a.title, b.title)
    }
}
