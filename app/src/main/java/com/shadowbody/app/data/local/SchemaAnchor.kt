package com.shadowbody.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 1 foundation entity: anchors schema v1.
 *
 * Room's annotation processor requires at least one entity, and an anchored
 * v1 schema is what future explicit migrations (Phase 2+) build on. This
 * table stores no user data — a single row recording when the database was
 * first created. All feature tables arrive with their own phases.
 */
@Entity(tableName = "schema_anchor")
data class SchemaAnchor(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
