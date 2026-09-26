package com.shadowbody.app.data.local

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room foundation test: the database (schema v1) builds, opens, and
 * round-trips the anchor row. Catches annotation/KSP wiring mistakes early,
 * before Phase 2 adds real tables.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShadowBodyDatabaseTest {

    private var db: ShadowBodyDatabase? = null

    @After
    fun tearDown() {
        db?.close()
        db = null
    }

    @Test
    fun `in-memory database opens`() = runTest {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        // Room opens lazily: touch the database before asserting.
        checkNotNull(db).schemaAnchorDao().get()
        assertTrue(checkNotNull(db).isOpen)
    }

    @Test
    fun `anchor row round-trips`() = runTest {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        val dao = checkNotNull(db).schemaAnchorDao()
        dao.upsert(SchemaAnchor())
        val loaded = dao.get()
        assertNotNull(loaded)
        assertEquals(1, checkNotNull(loaded).id)
    }

    @Test
    fun `database version is pinned to the current schema`() {
        // Phase 1 shipped v1, Phase 2 bumped to v2, Phase 3 to v3, Phase 4
        // (adaptive) to v4, Phase 5 (morning activation) to v5. Pin the current
        // value so accidental version changes break loudly.
        assertEquals(5, ShadowBodyDatabase.VERSION)
    }
}
