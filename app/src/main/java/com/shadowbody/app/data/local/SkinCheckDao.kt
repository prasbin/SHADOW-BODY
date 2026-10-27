package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SkinCheckDao {
    @Query("SELECT * FROM skin_check_record ORDER BY dayKey DESC")
    fun observeAll(): Flow<List<SkinCheckRecord>>

    @Query("SELECT * FROM skin_check_record WHERE dayKey = :dayKey")
    suspend fun getByDay(dayKey: String): SkinCheckRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: SkinCheckRecord): Long

    @Query("DELETE FROM skin_check_record WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM skin_check_record")
    suspend fun count(): Int
}
