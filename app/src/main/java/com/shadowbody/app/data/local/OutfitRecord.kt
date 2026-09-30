package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfit_record")
data class OutfitRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val topItemId: Long? = null,
    val bottomItemId: Long? = null,
    val footwearItemId: Long? = null,
    val accessoryItemId: Long? = null,
    val occasion: String = "CASUAL",
    val season: String = "ALL_SEASON",
    val explanation: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
