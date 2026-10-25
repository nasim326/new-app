package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "EXPENSE" or "INCOME"
    val iconName: String = "Category",
    val colorHex: String = "#3B82F6"
)
