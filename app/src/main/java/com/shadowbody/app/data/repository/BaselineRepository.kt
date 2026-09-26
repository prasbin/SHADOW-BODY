package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.BaselineRecord
import com.shadowbody.app.data.local.BaselineRecordDao
import kotlinx.coroutines.flow.Flow

/** Baseline store. Validation lives in BaselineValidator; this only persists. */
class BaselineRepository(private val dao: BaselineRecordDao) {

    fun history(): Flow<List<BaselineRecord>> = dao.observeAll()

    fun latest(): Flow<BaselineRecord?> = dao.observeLatest()

    suspend fun add(record: BaselineRecord): Long = dao.insert(record)

    suspend fun delete(record: BaselineRecord) {
        dao.delete(record)
    }
}
