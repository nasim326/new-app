package com.example.util

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CompoundingFrequency
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import java.util.Calendar
import kotlin.math.pow

data class FdMaturityEstimate(
    val principal: Double,
    val maturityAmount: Double,
    val estimatedInterest: Double,
    val durationDays: Long,
    val durationYears: Double
)

data class DpsProgressSummary(
    val totalInstallments: Int,
    val paidInstallments: Int,
    val missedInstallments: Int,
    val remainingInstallments: Int,
    val totalPaidAmount: Double,
    val estimatedMaturityAmount: Double,
    val estimatedTotalInterest: Double,
    val nextDueDate: Long?
)

object InterestCalculator {

    /**
     * Calculates Compound Interest for Fixed Deposit (FDR)
     * A = P * (1 + r/n)^(n*t)
     */
    fun calculateFdMaturity(
        principal: Double,
        annualRatePercent: Double,
        openingDate: Long,
        maturityDate: Long?,
        compoundingFrequency: String
    ): FdMaturityEstimate {
        if (principal <= 0.0) {
            return FdMaturityEstimate(0.0, 0.0, 0.0, 0, 0.0)
        }

        val effectiveMaturity = maturityDate ?: (openingDate + (365L * 24 * 60 * 60 * 1000))
        val durationMillis = (effectiveMaturity - openingDate).coerceAtLeast(0)
        val durationDays = durationMillis / (1000 * 60 * 60 * 24)
        val t = durationDays / 365.25

        val r = annualRatePercent / 100.0

        val n = when (compoundingFrequency) {
            CompoundingFrequency.MONTHLY.name -> 12
            CompoundingFrequency.QUARTERLY.name -> 4
            CompoundingFrequency.SEMI_ANNUALLY.name -> 2
            CompoundingFrequency.ANNUALLY.name -> 1
            CompoundingFrequency.AT_MATURITY.name -> 1
            else -> 1
        }

        val maturityValue = if (compoundingFrequency == CompoundingFrequency.AT_MATURITY.name) {
            // Simple interest if paid once at maturity
            principal * (1.0 + r * t)
        } else {
            // Compound interest
            principal * (1.0 + (r / n)).pow(n * t)
        }

        val estimatedInterest = (maturityValue - principal).coerceAtLeast(0.0)

        return FdMaturityEstimate(
            principal = principal,
            maturityAmount = (maturityValue * 100).toLong() / 100.0,
            estimatedInterest = (estimatedInterest * 100).toLong() / 100.0,
            durationDays = durationDays,
            durationYears = (t * 10).toLong() / 10.0
        )
    }

    /**
     * Analyzes DPS / Recurring Deposit Progress, Installments and Maturity
     */
    fun analyzeDps(
        account: AccountEntity,
        transactions: List<TransactionEntity>
    ): DpsProgressSummary {
        val totalInst = if (account.totalInstallments > 0) account.totalInstallments else 36
        val installmentAmount = account.depositAmount

        // Count successful deposit transactions to this account
        val depositTxs = transactions.filter {
            (it.targetAccountId == account.id && (it.type == TransactionType.DEPOSIT.name || it.type == TransactionType.TRANSFER.name))
        }

        val paidCount = depositTxs.size
        val totalPaid = depositTxs.sumOf { it.amount }

        // Determine expected installments from opening date to current date
        val nowCal = Calendar.getInstance()
        val openCal = Calendar.getInstance().apply { timeInMillis = account.openingDate }

        val yearDiff = nowCal.get(Calendar.YEAR) - openCal.get(Calendar.YEAR)
        val monthDiff = nowCal.get(Calendar.MONTH) - openCal.get(Calendar.MONTH)
        val totalMonthsElapsed = (yearDiff * 12 + monthDiff + 1).coerceAtLeast(1)

        val expectedInst = totalMonthsElapsed.coerceAtMost(totalInst)
        val missedCount = (expectedInst - paidCount).coerceAtLeast(0)
        val remainingCount = (totalInst - paidCount).coerceAtLeast(0)

        // Calculate next due date
        val nextDueCal = Calendar.getInstance()
        val dueDay = if (account.dayOfMonthDue in 1..31) account.dayOfMonthDue else 5
        val maxDayCurrentMonth = nextDueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        nextDueCal.set(Calendar.DAY_OF_MONTH, dueDay.coerceAtMost(maxDayCurrentMonth))
        nextDueCal.set(Calendar.HOUR_OF_DAY, 10)
        nextDueCal.set(Calendar.MINUTE, 0)
        nextDueCal.set(Calendar.SECOND, 0)

        if (nextDueCal.timeInMillis < System.currentTimeMillis()) {
            // Already passed this month's due date, move to next month
            nextDueCal.add(Calendar.MONTH, 1)
            val maxDayNextMonth = nextDueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            nextDueCal.set(Calendar.DAY_OF_MONTH, dueDay.coerceAtMost(maxDayNextMonth))
        }

        val nextDueDate = if (remainingCount > 0) nextDueCal.timeInMillis else null

        // Estimated maturity value calculation for recurring deposit
        // Monthly compounding: Sum of D * (1 + r/12)^(N - k + 1) for k = 1 to N
        val r = (account.interestRate / 100.0) / 12.0
        var estimatedMaturity = 0.0
        if (r > 0) {
            for (k in 1..totalInst) {
                estimatedMaturity += installmentAmount * (1.0 + r).pow(totalInst - k + 1)
            }
        } else {
            estimatedMaturity = installmentAmount * totalInst
        }

        val estimatedInterest = (estimatedMaturity - (installmentAmount * totalInst)).coerceAtLeast(0.0)

        return DpsProgressSummary(
            totalInstallments = totalInst,
            paidInstallments = paidCount,
            missedInstallments = missedCount,
            remainingInstallments = remainingCount,
            totalPaidAmount = totalPaid,
            estimatedMaturityAmount = (estimatedMaturity * 100).toLong() / 100.0,
            estimatedTotalInterest = (estimatedInterest * 100).toLong() / 100.0,
            nextDueDate = nextDueDate
        )
    }
}
