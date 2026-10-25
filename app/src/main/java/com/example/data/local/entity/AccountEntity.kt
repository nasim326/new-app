package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType(val displayName: String) {
    REGULAR_SAVINGS("Regular Savings"),
    DPS("DPS (Deposit Pension Scheme)"),
    FIXED_DEPOSIT("Fixed Deposit / FDR"),
    RECURRING_DEPOSIT("Recurring Deposit"),
    EMERGENCY_FUND("Emergency Fund"),
    CASH("Cash Savings"),
    OTHER("Custom Savings Account")
}

enum class CompoundingFrequency(val displayName: String, val timesPerYear: Int) {
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    SEMI_ANNUALLY("Semi-Annually", 2),
    ANNUALLY("Annually", 1),
    AT_MATURITY("At Maturity", 1)
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val accountType: String, // from AccountType name
    val institution: String,
    val accountNumberOrNickname: String = "",
    val openingDate: Long = System.currentTimeMillis(),
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val interestRate: Double = 0.0, // Annual % (e.g., 8.5)
    val depositAmount: Double = 0.0, // Monthly installment for DPS/RD, or principal
    val maturityDate: Long? = null,
    val totalInstallments: Int = 0, // e.g., 36 or 60 months
    val compoundingFrequency: String = CompoundingFrequency.ANNUALLY.name,
    val dayOfMonthDue: Int = 1,
    val notes: String = "",
    val isArchived: Boolean = false
)
