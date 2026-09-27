package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak WHERE id = 1")
    abstract fun observe(): Flow<Streak?>

    @Query("SELECT * FROM streak WHERE id = 1")
    abstract suspend fun getSync(): Streak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(streak: Streak)

    @Update
    abstract suspend fun update(streak: Streak)
}