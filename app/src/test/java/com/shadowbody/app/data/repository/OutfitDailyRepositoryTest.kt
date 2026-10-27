package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.OutfitDailyDecision
import com.shadowbody.app.data.local.OutfitDailyDecisionDao
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.data.local.WardrobePhotoCombinationDao
import com.shadowbody.app.domain.outfit.OutfitDayKey
import com.shadowbody.app.domain.outfit.OutfitRecommenderEngine.Occasions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feature C repository tests: decision persistence via the photographed
 * wardrobe options; honest explanations are stored verbatim.
 */
class OutfitDailyRepositoryTest {

    private class FakeOutfitDao : OutfitDailyDecisionDao {
        val rows = LinkedHashMap<Long, OutfitDailyDecision>()
        private val flow = MutableStateFlow(0L)

        override fun observeAll(): Flow<List<OutfitDailyDecision>> = flow.map {
            rows.values.sortedByDescending { it.dayKey }
        }

        override suspend fun getByDay(dayKey: String): OutfitDailyDecision? =
            rows.values.lastOrNull { it.dayKey == dayKey }

        override suspend fun upsert(decision: OutfitDailyDecision): Long {
            val existing = rows.values.lastOrNull { it.dayKey == decision.dayKey }
            val id = existing?.id ?: (rows.keys.maxOrNull() ?: 0L) + 1
            val stored = if (decision.id == 0L) decision.copy(id = id) else decision
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

    private class FakeCombinationDao : WardrobePhotoCombinationDao {
        val rows = LinkedHashMap<Long, WardrobePhotoCombination>()

        override fun observeAll(): Flow<List<WardrobePhotoCombination>> =
            MutableStateFlow(rows.values.sortedBy { it.position })

        override suspend fun getById(id: Long): WardrobePhotoCombination? = rows[id]

        override suspend fun getByPosition(position: Int): WardrobePhotoCombination? =
            rows.values.firstOrNull { it.position == position }

        override suspend fun insert(combination: WardrobePhotoCombination): Long {
            val id = (rows.keys.maxOrNull() ?: 0L) + 1
            rows[id] = combination.copy(id = id)
            return id
        }

        override suspend fun update(combination: WardrobePhotoCombination) {
            rows[combination.id] = combination
        }

        override suspend fun delete(combination: WardrobePhotoCombination) {
            rows.remove(combination.id)
        }

        override suspend fun deleteById(id: Long) {
            rows.remove(id)
        }

        override suspend fun count(): Int = rows.size

        override suspend fun snapshotOrderedByPosition(): List<WardrobePhotoCombination> =
            rows.values.sortedBy { it.position }
    }

    private class Bundle {
        val outfitDao = FakeOutfitDao()
        val combinationDao = FakeCombinationDao()
        val repository = OutfitDailyRepository(
            decisionDao = outfitDao,
            combinationDao = combinationDao,
            nowMillis = { 1_700_000_000_000L },
        )

        suspend fun seed(vararg combos: WardrobePhotoCombination) {
            combos.forEach { combinationDao.insert(it) }
        }
    }

    private fun combo(id: Long, label: String, notes: String = "", withPhoto: Boolean = true) =
        WardrobePhotoCombination(
            id = 0,
            position = id.toInt(),
            label = label,
            photoPath = if (withPhoto) "/p/$id.jpg" else null,
            notes = notes,
            createdAt = 1000 + id,
            updatedAt = 1000 + id,
        )

    @Test
    fun `no photographed options - recommendation refused and nothing persisted`() = runTest {
        val b = Bundle()
        val decision = b.repository.recommendForToday(Occasions.TRAINING)
        assertNull(decision)
        assertEquals(0, b.outfitDao.count())
    }

    @Test
    fun `recommendation persists a decision for today with honest explanation`() = runTest {
        val b = Bundle()
        b.seed(combo(1, "Dinner set", notes = "jeans and sneakers"), combo(2, "gym training tee"))
        val decision = b.repository.recommendForToday(Occasions.TRAINING)
        assertNotNull(decision)
        assertEquals(OutfitDayKey.today(), decision!!.dayKey)
        assertEquals(2L, decision.selectedCombinationId)
        assertTrue(decision.explanation.contains("No image analysis"))
        assertTrue(decision.candidateIds.split(",").contains("1"))
    }

    @Test
    fun `only one decision per day - repeat calls replace`() = runTest {
        val b = Bundle()
        b.seed(combo(1, "casual"), combo(2, "another casual"))
        val first = b.repository.recommendForToday(Occasions.SOCIAL_CASUAL)
        val second = b.repository.recommendForToday(Occasions.REST_HOME)
        assertNotNull(first)
        assertNotNull(second)
        assertEquals(first!!.id, second!!.id)
        assertEquals(1, b.outfitDao.count())
        assertEquals(Occasions.REST_HOME, b.outfitDao.rows[first.id]!!.occasion)
    }

    @Test
    fun `decisions resolve back to their option`() = runTest {
        val b = Bundle()
        b.seed(combo(5, "The pick"))
        val decision = b.repository.recommendForToday(Occasions.TRAINING)
        val resolved = decision?.let { b.repository.resolveSelected(it) }
        assertEquals("The pick", resolved?.label)
    }

    @Test
    fun `decision history preserves past days`() = runTest {
        val b = Bundle()
        b.seed(combo(1, "x"))
        b.repository.recommendForToday(Occasions.TRAINING)
        // Simulate a past decision row manually.
        b.outfitDao.upsert(
            OutfitDailyDecision(
                id = 0,
                dayKey = "2026-01-01",
                selectedCombinationId = 1,
                candidateIds = "1",
                occasion = Occasions.SOCIAL_CASUAL,
                explanation = "historic",
                decidedAt = 1,
                updatedAt = 1,
            ),
        )
        val history = b.repository.observeDecisions().first()
        assertEquals(2, history.size)
        assertTrue(history.any { it.dayKey == "2026-01-01" })
        assertTrue(history.any { it.dayKey == OutfitDayKey.today() })
    }
}
