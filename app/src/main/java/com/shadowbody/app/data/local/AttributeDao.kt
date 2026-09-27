package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttributeDao {
    @Query("SELECT * FROM attribute WHERE id = 1")
    abstract fun observe(): Flow<Attribute?>

    @Query("SELECT * FROM attribute WHERE id = 1")
    abstract suspend fun getSync(): Attribute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(attr: Attribute)

    @Update
    abstract suspend fun update(attr: Attribute)
}