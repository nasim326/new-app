package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.TransactionEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.ChartSlice
import com.example.ui.components.DonutChart
import com.example.ui.components.MonthlyHeader
import com.example.ui.components.SummaryDashboardGrid
import com.example.ui.components.TransactionItem
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.RoseExpense
import com.example.util.CurrencyUtils
import com.example.util.DateUtils

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val financials by viewModel.monthlyFinancials.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val upcomingReminders by viewModel.upcomingReminders.collectAsStateWithLifecycle()

    val accountsMap = accounts.associateBy { it.id }

    // Recent 5 transactions
    val recentTransactions = allTransactions.take(5)

    // Slices for Category Donut Chart
    val categoryColors = listOf(
        Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF3B82F6),
        Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF06B6D4), Color(0xFF14B8A6), Color(0xFFF97316),
        Color(0xFF84CC16), Color(0xFF64748B)
    )

    val chartSlices = financials.categoryExpenses.entries.mapIndexed { index, entry ->
        ChartSlice(
            label = entry.key,
            value = entry.value,
            color = categoryColors[index % categoryColors.size]
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Top Title & Settings row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Budget & Savings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "Financial Dashboard & Wealth Planner",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }

                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    modifier = Modifier.testTag("btn_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Monthly Period Selector
        item {
            MonthlyHeader(
                month = month,
                year = year,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onPickerClick = { /* Can cycle or show month selector */ }
            )
        }

        // 4-Card Summary Grid (Income, Expense, Balance, Savings)
        item {
            SummaryDashboardGrid(
                income = financials.totalIncome,
                expense = financials.totalExpense,
                balance = financials.remainingBalance,
                savings = financials.totalSavingsBalance,
                currencySymbol = currencySymbol
            )
        }

        // Monthly Budget Progress Card
        item {
            BudgetOverviewCard(
                financials = financials,
                currencySymbol = currencySymbol,
                onManageBudgets = { viewModel.navigateTo(AppScreen.BUDGET_PLANNER) }
            )
        }

        // Upcoming Reminders Banner (DPS & Fixed Deposits)
        if (upcomingReminders.isNotEmpty()) {
            item {
                UpcomingRemindersSection(
                    reminders = upcomingReminders,
                    currencySymbol = currencySymbol,
                    dateFormat = dateFormat,
                    onAccountClick = { accId ->
                        accountsMap[accId]?.let { viewModel.openAccountDetail(it) }
                    }
                )
            }
        }

        // Spending by Category Donut Chart
        if (financials.totalExpense > 0 && chartSlices.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Expense Breakdown",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DonutChart(
                                slices = chartSlices,
                                size = 150.dp,
                                strokeWidth = 20.dp,
                                centerTitle = "$currencySymbol${CurrencyUtils.formatWithoutSymbol(financials.totalExpense)}",
                                centerSubtitle = "Total Spent"
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                chartSlices.take(4).forEach { slice ->
                                    val percent = if (financials.totalExpense > 0) {
                                        ((slice.value / financials.totalExpense) * 100).toInt()
                                    } else 0
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(slice.color, CircleShape)
                                        )
                                        Text(
                                            text = "${slice.label} ($percent%)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Savings Accounts Quick Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Savings & Deposit Accounts",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.SAVINGS) }) {
                        Text(
                            text = "View All (${accounts.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (accounts.isEmpty()) {
                    Text(
                        text = "No savings accounts added yet. Tap Savings tab to add your DPS or Fixed Deposit.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(accounts) { acc ->
                            AccountQuickCard(
                                account = acc,
                                currencySymbol = currencySymbol,
                                onClick = { viewModel.openAccountDetail(acc) }
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                TextButton(onClick = { viewModel.navigateTo(AppScreen.TRANSACTIONS) }) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No transactions recorded yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        OutlinedButton(
                            onClick = { viewModel.openAddTransaction() },
                            modifier = Modifier.testTag("btn_add_first_tx")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Add First Transaction", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        } else {
            items(recentTransactions) { tx ->
                TransactionItem(
                    transaction = tx,
                    currencySymbol = currencySymbol,
                    accountsMap = accountsMap,
                    dateFormat = dateFormat,
                    onClick = { viewModel.openAddTransaction(tx) }
                )
            }
        }
    }
}

@Composable
fun BudgetOverviewCard(
    financials: com.example.ui.MonthlyFinancials,
    currencySymbol: String,
    onManageBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgetLimit = financials.budgetLimit
    val spent = financials.budgetSpent
    val remaining = financials.budgetRemaining
    val percentSpent = financials.budgetPercentSpent

    val progress = if (budgetLimit > 0) (spent / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f

    val isOverBudget = budgetLimit > 0 && spent > budgetLimit
    val isWarning = budgetLimit > 0 && percentSpent >= 80f && !isOverBudget

    val progressColor = when {
        isOverBudget -> RoseExpense
        isWarning -> AmberWarning
        else -> EmeraldIncome
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_overview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Monthly Budget Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    if (isOverBudget) {
                        Surface(color = RoseExpense.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "Exceeded!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = RoseExpense,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isWarning) {
                        Surface(color = AmberWarning.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "80%+ Used",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberWarning,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                TextButton(onClick = onManageBudgets) {
                    Text("Plan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (budgetLimit <= 0.0) {
                Text(
                    text = "No budget limit set for this month. Set a budget to track spending discipline.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(height = 8.dp, width = 0.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Spent (${percentSpent.toInt()}%)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(spent)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(remaining)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverBudget) RoseExpense else EmeraldIncome,
                                fontSize = 13.sp
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Limit",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(budgetLimit)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingRemindersSection(
    reminders: List<com.example.ui.UpcomingReminder>,
    currencySymbol: String,
    dateFormat: String,
    onAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_reminders_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Upcoming DPS & Maturity Reminders",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                )
            }

            reminders.take(3).forEach { rem ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAccountClick(rem.accountId) },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rem.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = "Due: ${DateUtils.formatDate(rem.dueDate, dateFormat)} • ${rem.subtitle}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        }

                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(rem.amount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountQuickCard(
    account: com.example.data.local.entity.AccountEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(width = 170.dp, height = 110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = account.institution,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }

            Column {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                )
                Text(
                    text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(account.currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
