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
    fun `database version starts at 1`() {
        assertEquals(1, ShadowBodyDatabase.VERSION)
    }
}
