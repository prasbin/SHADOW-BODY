package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobePhotoCombinationDao {
    @Query("SELECT * FROM wardrobe_photo_combination ORDER BY position")
    fun observeAll(): Flow<List<WardrobePhotoCombination>>

    @Query("SELECT * FROM wardrobe_photo_combination WHERE id = :id")
    suspend fun getById(id: Long): WardrobePhotoCombination?

    @Query("SELECT * FROM wardrobe_photo_combination WHERE position = :position")
    suspend fun getByPosition(position: Int): WardrobePhotoCombination?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(combination: WardrobePhotoCombination): Long

    @Update
    suspend fun update(combination: WardrobePhotoCombination)

    @Delete
    suspend fun delete(combination: WardrobePhotoCombination)

    @Query("DELETE FROM wardrobe_photo_combination WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM wardrobe_photo_combination")
    suspend fun count(): Int
}
