package com.shadowbody.app.data.repository

import androidx.test.core.app.ApplicationProvider
import com.shadowbody.app.data.local.BaselineRecord
import com.shadowbody.app.data.local.ShadowBodyDatabase
import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {

    private var db: ShadowBodyDatabase? = null

    @After
    fun tearDown() {
        db?.close()
        db = null
    }

    private fun profile() = UserProfile(
        age = 28,
        heightCm = 178.0,
        weightKg = 75.0,
        fitnessLevel = FitnessLevel.BEGINNER,
        equipment = setOf(Equipment.BODYWEIGHT),
        goals = setOf(Goal.GENERAL_FITNESS),
        trainingDays = setOf(1, 3, 5),
        sessionMinutes = 30,
    )

    @Test
    fun `profile starts absent then persists after save`() = runTest {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        val repo = ProfileRepository(checkNotNull(db).userProfileDao())
        assertNull(repo.profile.first())
        repo.save(profile())
        assertEquals(28, repo.get()?.age)
    }

    @Test
    fun `profile update overwrites the single row`() = runTest {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        val repo = ProfileRepository(checkNotNull(db).userProfileDao())
        repo.save(profile())
        repo.save(profile().copy(age = 29, fitnessLevel = FitnessLevel.ADVANCED))
        val loaded = repo.get()
        assertEquals(29, loaded?.age)
        assertEquals(FitnessLevel.ADVANCED, loaded?.fitnessLevel)
    }

    @Test
    fun `baseline history accumulates newest first`() = runTest {
        db = ShadowBodyDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        val repo = BaselineRepository(checkNotNull(db).baselineRecordDao())
        assertTrue(repo.history().first().isEmpty())
        repo.add(BaselineRecord(recordedAt = 1000, weightKg = 80.0))
        repo.add(BaselineRecord(recordedAt = 2000, weightKg = 79.0))
        val history = repo.history().first()
        assertEquals(2, history.size)
        assertEquals(79.0, history[0].weightKg!!, 0.0)
        repo.delete(history[0])
        assertEquals(1, repo.history().first().size)
    }

    @Test
    fun `data survives database reopen`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "reopen-${System.nanoTime()}.db")
        val path = file.absolutePath
        val first = androidx.room.Room.databaseBuilder(context, ShadowBodyDatabase::class.java, path)
            .addMigrations(com.shadowbody.app.data.local.Migrations.MIGRATION_1_2)
            .build()
        ProfileRepository(first.userProfileDao()).save(profile())
        BaselineRepository(first.baselineRecordDao()).add(BaselineRecord(weightKg = 80.0))
        first.close()

        val second = androidx.room.Room.databaseBuilder(context, ShadowBodyDatabase::class.java, path)
            .addMigrations(com.shadowbody.app.data.local.Migrations.MIGRATION_1_2)
            .build()
        assertEquals(28, ProfileRepository(second.userProfileDao()).get()?.age)
        assertEquals(1, BaselineRepository(second.baselineRecordDao()).history().first().size)
        second.close()
        file.delete()
    }
}
