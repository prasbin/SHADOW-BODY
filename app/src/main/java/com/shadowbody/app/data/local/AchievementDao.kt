package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievement ORDER BY id")
    abstract fun observeAll(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievement ORDER BY id")
    abstract suspend fun getAllSync(): List<Achievement>

    @Query("SELECT * FROM achievement WHERE id = :id")
    abstract suspend fun getSync(id: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(achievement: Achievement)

    @Update
    abstract suspend fun update(achievement: Achievement)

    @Query("SELECT * FROM achievement WHERE unlocked = 1 ORDER BY unlockedAt DESC")
    abstract fun observeUnlocked(): Flow<List<Achievement>>
}