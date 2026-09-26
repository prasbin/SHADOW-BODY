package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Minimal DAO for the schema anchor row. Feature DAOs arrive per phase. */
@Dao
interface SchemaAnchorDao {
    @Upsert
    suspend fun upsert(anchor: SchemaAnchor)

    @Query("SELECT * FROM schema_anchor WHERE id = 1")
    suspend fun get(): SchemaAnchor?
}
