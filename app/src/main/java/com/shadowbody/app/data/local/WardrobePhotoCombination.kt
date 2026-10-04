package com.shadowbody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wardrobe_photo_combination")
data class WardrobePhotoCombination(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val position: Int,
    val label: String,
    val photoPath: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
