package com.shadowbody.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Phase 1 Room foundation.
 *
 * - Version starts at 1, anchored by the [SchemaAnchor] entity.
 * - Each later phase adds its entities + an **explicit** Migration object;
 *   [fallbackToDestructiveMigration] is deliberately NOT used so user data
 *   can never be silently wiped by a schema upgrade.
 * - Schemas are exported to `app/schemas` and committed to Git so
 *   migrations are verifiable and reviewable.
 */
@Database(
    entities = [SchemaAnchor::class],
    version = ShadowBodyDatabase.VERSION,
    exportSchema = true,
)
abstract class ShadowBodyDatabase : RoomDatabase() {

    abstract fun schemaAnchorDao(): SchemaAnchorDao

    companion object {
        const val VERSION = 1
        const val NAME = "shadow_body.db"

        @Volatile
        private var instance: ShadowBodyDatabase? = null

        fun build(context: Context): ShadowBodyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShadowBodyDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }

        /** Test helper: in-memory database, never persisted. */
        fun buildInMemory(context: Context): ShadowBodyDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ShadowBodyDatabase::class.java,
            ).build()
    }
}
