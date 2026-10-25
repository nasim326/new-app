package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.IndigoTransfer
import com.example.ui.theme.RoseExpense
import com.example.util.CurrencyUtils
import com.example.util.DateUtils

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    accountsMap: Map<Long, AccountEntity>,
    dateFormat: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE.name
    val isIncome = transaction.type == TransactionType.INCOME.name
    val isTransfer = transaction.type == TransactionType.TRANSFER.name
    val isDeposit = transaction.type == TransactionType.DEPOSIT.name
    val isWithdrawal = transaction.type == TransactionType.WITHDRAWAL.name
    val isInterest = transaction.type == TransactionType.INTEREST.name

    val amountColor = when {
        isExpense || isWithdrawal -> RoseExpense
        isIncome || isInterest || isDeposit -> EmeraldIncome
        else -> IndigoTransfer
    }

    val amountPrefix = when {
        isExpense || isWithdrawal -> "-"
        isIncome || isInterest || isDeposit -> "+"
        else -> "⇆ "
    }

    val categoryIcon = getCategoryIcon(transaction.category, transaction.type)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(amountColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = amountColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Description, Category, Account info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.description.ifBlank { transaction.category },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = DateUtils.formatDate(transaction.date, dateFormat),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                // Account tag if available
                val accountName = when {
                    isTransfer -> {
                        val src = transaction.sourceAccountId?.let { accountsMap[it]?.name } ?: ""
                        val tgt = transaction.targetAccountId?.let { accountsMap[it]?.name } ?: ""
                        if (src.isNotEmpty() && tgt.isNotEmpty()) "$src → $tgt" else ""
                    }
                    transaction.sourceAccountId != null -> accountsMap[transaction.sourceAccountId]?.name ?: ""
                    transaction.targetAccountId != null -> accountsMap[transaction.targetAccountId]?.name ?: ""
                    else -> ""
                }

                if (accountName.isNotEmpty()) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
            }

            // Amount Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$amountPrefix$currencySymbol${CurrencyUtils.formatWithoutSymbol(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        fontSize = 14.sp
                    )
                )

                Text(
                    text = transaction.paymentMethod,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

fun getCategoryIcon(category: String, type: String): ImageVector {
    val cat = category.lowercase()
    return when {
        cat.contains("food") || cat.contains("dining") || cat.contains("restaurant") -> Icons.Default.Restaurant
        cat.contains("rent") || cat.contains("hous") || cat.contains("home") -> Icons.Default.Home
        cat.contains("transport") || cat.contains("car") || cat.contains("transit") || cat.contains("fuel") -> Icons.Default.DirectionsCar
        cat.contains("shop") || cat.contains("grocer") -> Icons.Default.ShoppingBag
        cat.contains("bill") || cat.contains("utilit") -> Icons.Default.Receipt
        cat.contains("health") || cat.contains("medic") -> Icons.Default.LocalHospital
        cat.contains("educat") || cat.contains("school") -> Icons.Default.School
        cat.contains("travel") || cat.contains("flight") -> Icons.Default.Flight
        cat.contains("dps") || cat.contains("saving") || cat.contains("deposit") -> Icons.Default.Savings
        cat.contains("transfer") -> Icons.Default.SyncAlt
        type == TransactionType.INCOME.name || cat.contains("salary") -> Icons.Default.AttachMoney
        type == TransactionType.INTEREST.name -> Icons.Default.TrendingUp
        else -> Icons.Default.Category
    }
}
