package com.shadowbody.app.ui.dashboard

import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.data.repository.ProfileRepository
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Dashboard reflects profile state: missing vs configured. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardProfileStateTest {

    private val dispatcher = StandardTestDispatcher()
    private var db: ShadowBodyDatabase? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        db?.close()
        db = null
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboard reports profile missing then configured`() = runTest(dispatcher) {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        val repo = ProfileRepository(checkNotNull(db).userProfileDao())
        val vm = DashboardViewModel(repo)

        advanceUntilIdle()
        assertFalse(vm.uiState.value.profileConfigured)

        repo.save(
            UserProfile(
                age = 28, heightCm = 178.0, weightKg = 75.0,
                fitnessLevel = FitnessLevel.BEGINNER,
                equipment = setOf(Equipment.BODYWEIGHT),
                goals = setOf(Goal.GENERAL_FITNESS),
                trainingDays = setOf(1, 3, 5),
                sessionMinutes = 30,
            ),
        )
        // Room re-queries on its own executor: await the emission instead of
        // assuming virtual-time scheduling covers real background threads.
        val configured = withTimeout(10_000) {
            vm.uiState.first { it.profileConfigured }
        }
        assertTrue(configured.profileConfigured)
    }
}
