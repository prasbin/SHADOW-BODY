package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroomingPreferencesDao {
    @Query("SELECT * FROM grooming_preferences WHERE id = 1")
    fun observe(): Flow<GroomingPreferences?>

    @Query("SELECT * FROM grooming_preferences WHERE id = 1")
    suspend fun getSync(): GroomingPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(prefs: GroomingPreferences)
}