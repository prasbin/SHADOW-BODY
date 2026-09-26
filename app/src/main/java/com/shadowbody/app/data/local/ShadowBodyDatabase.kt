package com.shadowbody.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * SHADOW BODY database.
 *
 * - v1: [SchemaAnchor] foundation only.
 * - v2: + [UserProfile], + [BaselineRecord] (see [Migrations.MIGRATION_1_2]).
 *
 * Every version bump ships an explicit Migration; destructive fallback is
 * never enabled. Schemas are exported to `app/schemas` and committed.
 */
@Database(
    entities = [SchemaAnchor::class, UserProfile::class, BaselineRecord::class],
    version = ShadowBodyDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ShadowBodyDatabase : RoomDatabase() {

    abstract fun schemaAnchorDao(): SchemaAnchorDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun baselineRecordDao(): BaselineRecordDao

    companion object {
        const val VERSION = 2
        const val NAME = "shadow_body.db"

        @Volatile
        private var instance: ShadowBodyDatabase? = null

        fun build(context: Context): ShadowBodyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShadowBodyDatabase::class.java,
                    NAME,
                )
                    .addMigrations(Migrations.MIGRATION_1_2)
                    .build().also { instance = it }
            }

        /** Test helper: in-memory database, never persisted. */
        fun buildInMemory(context: Context): ShadowBodyDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ShadowBodyDatabase::class.java,
            ).build()
    }
}
