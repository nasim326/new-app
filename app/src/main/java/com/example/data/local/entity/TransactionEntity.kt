package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType(val displayName: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Account Transfer"),
    DEPOSIT("Savings Deposit"),
    WITHDRAWAL("Savings Withdrawal"),
    INTEREST("Interest Earned")
}

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("type"),
        Index("category"),
        Index("sourceAccountId"),
        Index("targetAccountId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // TransactionType name
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String,
    val paymentMethod: String = "Cash", // Cash, Bank Transfer, Card, Mobile Banking, etc.
    val description: String = "",
    val sourceAccountId: Long? = null,
    val targetAccountId: Long? = null,
    val isEstimatedInterest: Boolean = false, // Distinguish estimated vs actual interest
    val installmentMonthIndex: Int? = null // e.g. installment #3 for DPS
)
