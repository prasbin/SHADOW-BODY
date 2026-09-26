package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BaselineRecordDao {
    @Query("SELECT * FROM baseline_record ORDER BY recordedAt DESC, id DESC")
    fun observeAll(): Flow<List<BaselineRecord>>

    @Query("SELECT * FROM baseline_record ORDER BY recordedAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<BaselineRecord?>

    @Query("SELECT * FROM baseline_record WHERE id = :id")
    suspend fun getById(id: Long): BaselineRecord?

    @Insert
    suspend fun insert(record: BaselineRecord): Long

    @Delete
    suspend fun delete(record: BaselineRecord)
}
