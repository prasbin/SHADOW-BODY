package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wardrobe_item")
data class WardrobeItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val clothingType: String,
    val color: String,
    val secondaryColor: String? = null,
    val style: String = "",
    val season: String = "ALL_SEASON",
    val occasion: String = "CASUAL",
    val fit: String = "",
    val isEnabled: Boolean = true,
    val notes: String = "",
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
