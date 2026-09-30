package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeItemDao {
    @Query("SELECT * FROM wardrobe_item ORDER BY category, name")
    fun observeAll(): Flow<List<WardrobeItem>>

    @Query("SELECT * FROM wardrobe_item WHERE isEnabled = 1 ORDER BY category, name")
    fun observeEnabled(): Flow<List<WardrobeItem>>

    @Query("SELECT * FROM wardrobe_item WHERE id = :id")
    suspend fun getById(id: Long): WardrobeItem?

    @Query("SELECT * FROM wardrobe_item WHERE category = :category AND isEnabled = 1 ORDER BY name")
    fun observeByCategory(category: String): Flow<List<WardrobeItem>>

    @Query("SELECT * FROM wardrobe_item WHERE name LIKE '%' || :query || '%' OR clothingType LIKE '%' || :query || '%' OR color LIKE '%' || :query || '%' ORDER BY category, name")
    fun search(query: String): Flow<List<WardrobeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WardrobeItem): Long

    @Update
    suspend fun update(item: WardrobeItem)

    @Delete
    suspend fun delete(item: WardrobeItem)

    @Query("DELETE FROM wardrobe_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE wardrobe_item SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM wardrobe_item")
    suspend fun count(): Int
}
