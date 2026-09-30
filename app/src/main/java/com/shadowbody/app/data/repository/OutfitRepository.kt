package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.OutfitRecord
import com.shadowbody.app.data.local.OutfitRecordDao
import kotlinx.coroutines.flow.Flow

class OutfitRepository(
    private val recordDao: OutfitRecordDao,
) {
    val allRecords: Flow<List<OutfitRecord>> = recordDao.observeAll()

    fun observeRecent(limit: Int = 20): Flow<List<OutfitRecord>> =
        recordDao.observeRecent(limit)

    suspend fun getById(id: Long): OutfitRecord? = recordDao.getById(id)

    suspend fun saveOutfit(record: OutfitRecord): Long = recordDao.insert(record)

    suspend fun updateOutfit(record: OutfitRecord) = recordDao.update(record)

    suspend fun deleteOutfit(record: OutfitRecord) = recordDao.delete(record)

    suspend fun deleteById(id: Long) = recordDao.deleteById(id)
}
