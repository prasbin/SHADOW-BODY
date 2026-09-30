package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitRecordDao {
    @Query("SELECT * FROM outfit_record ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OutfitRecord>>

    @Query("SELECT * FROM outfit_record WHERE id = :id")
    suspend fun getById(id: Long): OutfitRecord?

    @Query("SELECT * FROM outfit_record ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<OutfitRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: OutfitRecord): Long

    @Update
    suspend fun update(record: OutfitRecord)

    @Delete
    suspend fun delete(record: OutfitRecord)

    @Query("DELETE FROM outfit_record WHERE id = :id")
    suspend fun deleteById(id: Long)
}
