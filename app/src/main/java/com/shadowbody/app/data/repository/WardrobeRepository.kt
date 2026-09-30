package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.WardrobeItem
import com.shadowbody.app.data.local.WardrobeItemDao
import kotlinx.coroutines.flow.Flow

class WardrobeRepository(
    private val itemDao: WardrobeItemDao,
) {
    val allItems: Flow<List<WardrobeItem>> = itemDao.observeAll()
    val enabledItems: Flow<List<WardrobeItem>> = itemDao.observeEnabled()

    fun observeByCategory(category: String): Flow<List<WardrobeItem>> =
        itemDao.observeByCategory(category)

    fun search(query: String): Flow<List<WardrobeItem>> =
        itemDao.search(query)

    suspend fun getById(id: Long): WardrobeItem? = itemDao.getById(id)

    suspend fun addItem(item: WardrobeItem): Long = itemDao.insert(item)

    suspend fun updateItem(item: WardrobeItem) = itemDao.update(item)

    suspend fun deleteItem(item: WardrobeItem) = itemDao.delete(item)

    suspend fun deleteById(id: Long) = itemDao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) =
        itemDao.setEnabled(id, enabled, System.currentTimeMillis())

    suspend fun count(): Int = itemDao.count()
}
