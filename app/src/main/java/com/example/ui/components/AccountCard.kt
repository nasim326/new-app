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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.data.local.entity.AccountType
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.NavyPrimary
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.DpsProgressSummary
import com.example.util.FdMaturityEstimate

@Composable
fun AccountCard(
    account: AccountEntity,
    currencySymbol: String,
    dateFormat: String,
    dpsSummary: DpsProgressSummary? = null,
    fdEstimate: FdMaturityEstimate? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDps = account.accountType == AccountType.DPS.name || account.accountType == AccountType.RECURRING_DEPOSIT.name
    val isFd = account.accountType == AccountType.FIXED_DEPOSIT.name
    val isEmergency = account.accountType == AccountType.EMERGENCY_FUND.name

    val typeIcon: ImageVector = when {
        isDps -> Icons.Default.HourglassTop
        isFd -> Icons.Default.LockClock
        isEmergency -> Icons.Default.Shield
        account.accountType == AccountType.CASH.name -> Icons.Default.Payments
        else -> Icons.Default.AccountBalance
    }

    val typeColor = when {
        isDps -> Color(0xFF8B5CF6)
        isFd -> Color(0xFF0284C7)
        isEmergency -> Color(0xFFD97706)
        else -> EmeraldIncome
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("acc_card_${account.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Icon, Account Name, Institution, Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = buildString {
                            append(account.institution)
                            if (account.accountNumberOrNickname.isNotBlank()) {
                                append(" • ${account.accountNumberOrNickname}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                Surface(
                    color = typeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = AccountType.valueOf(account.accountType).displayName.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Balance & Rate row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(account.currentBalance)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 19.sp
                        )
                    )
                }

                if (account.interestRate > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${account.interestRate}% p.a.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // DPS Specific Installment Progress Bar
            if (isDps && dpsSummary != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val progress = if (dpsSummary.totalInstallments > 0) {
                        (dpsSummary.paidInstallments.toFloat() / dpsSummary.totalInstallments.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Installments: ${dpsSummary.paidInstallments}/${dpsSummary.totalInstallments} paid",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (dpsSummary.missedInstallments > 0) {
                            Text(
                                text = "${dpsSummary.missedInstallments} missed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        } else {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = EmeraldIncome,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(height = 6.dp, width = 0.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (dpsSummary.missedInstallments > 0) Color(0xFFF59E0B) else EmeraldIncome,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (dpsSummary.nextDueDate != null) {
                        Text(
                            text = "Next installment due: ${DateUtils.formatDate(dpsSummary.nextDueDate, dateFormat)} ($currencySymbol${CurrencyUtils.formatWithoutSymbol(account.depositAmount)})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Fixed Deposit Specific Maturity info
            if (isFd && fdEstimate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Est. Maturity Value",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(fdEstimate.maturityAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldIncome,
                                fontSize = 13.sp
                            )
                        )
                    }

                    account.maturityDate?.let { matDate ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Matures On",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = DateUtils.formatDate(matDate, dateFormat),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
