package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RoseExpense
import com.example.ui.theme.RoseLight
import com.example.util.CurrencyUtils

@Composable
fun SummaryDashboardGrid(
    income: Double,
    expense: Double,
    balance: Double,
    savings: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Total Income",
                amount = income,
                currencySymbol = currencySymbol,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = EmeraldIncome,
                containerBg = EmeraldLight.copy(alpha = 0.5f),
                iconBg = EmeraldDark,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_total_income")
            )
            MetricCard(
                title = "Total Expenses",
                amount = expense,
                currencySymbol = currencySymbol,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                accentColor = RoseExpense,
                containerBg = RoseLight.copy(alpha = 0.5f),
                iconBg = RoseDark,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_total_expenses")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Remaining Balance",
                amount = balance,
                currencySymbol = currencySymbol,
                icon = Icons.Default.AccountBalance,
                accentColor = if (balance >= 0) NavyPrimary else RoseExpense,
                containerBg = MaterialTheme.colorScheme.surfaceVariant,
                iconBg = if (balance >= 0) NavyPrimary else RoseDark,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_net_balance")
            )
            MetricCard(
                title = "Total Savings",
                amount = savings,
                currencySymbol = currencySymbol,
                icon = Icons.Default.Savings,
                accentColor = Color(0xFF2563EB),
                containerBg = Color(0xFFEFF6FF),
                iconBg = Color(0xFF1D4ED8),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_total_savings")
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: Double,
    currencySymbol: String,
    icon: ImageVector,
    accentColor: Color,
    containerBg: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = accentColor
                ),
                maxLines = 1
            )
        }
    }
}
