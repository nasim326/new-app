package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["month", "year", "category"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int, // 1 to 12
    val year: Int,
    val category: String, // "TOTAL" or specific category name (e.g., "Food & Dining")
    val amountLimit: Double,
    val carryForwardUnspent: Boolean = false
)
