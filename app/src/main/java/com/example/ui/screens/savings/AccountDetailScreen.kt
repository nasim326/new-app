package com.example.ui.screens.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.TransactionItem
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseExpense
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.InterestCalculator
import com.example.util.PdfReportGenerator
import com.example.util.PrintReportHelper
import com.example.util.ReportData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedAccount by viewModel.selectedAccountForDetail.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateTo(AppScreen.SAVINGS)
    }

    if (selectedAccount == null) {
        viewModel.navigateTo(AppScreen.SAVINGS)
        return
    }

    // Keep account instance fresh from accounts list
    val account = accounts.firstOrNull { it.id == selectedAccount!!.id } ?: selectedAccount!!

    val accountTransactions = allTransactions.filter {
        it.sourceAccountId == account.id || it.targetAccountId == account.id
    }
    val accountsMap = accounts.associateBy { it.id }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isDps = account.accountType == AccountType.DPS.name || account.accountType == AccountType.RECURRING_DEPOSIT.name
    val isFd = account.accountType == AccountType.FIXED_DEPOSIT.name

    val dpsSummary = if (isDps) InterestCalculator.analyzeDps(account, accountTransactions) else null
    val fdEstimate = if (isFd) {
        InterestCalculator.calculateFdMaturity(
            account.depositAmount.takeIf { it > 0 } ?: account.initialBalance,
            account.interestRate,
            account.openingDate,
            account.maturityDate,
            account.compoundingFrequency
        )
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(account.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(account.institution, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SAVINGS) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Account")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Account", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("account_detail_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AccountType.valueOf(account.accountType).displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            if (account.accountNumberOrNickname.isNotBlank()) {
                                Text(
                                    text = account.accountNumberOrNickname,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                                )
                            }
                        }

                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(account.currentBalance)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Opened: ${DateUtils.formatDate(account.openingDate, dateFormat)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                            )
                            if (account.interestRate > 0) {
                                Text(
                                    text = "Interest: ${account.interestRate}% p.a.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions: Deposit, Withdraw, Transfer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.openAddTransaction(
                                TransactionEntity(
                                    type = TransactionType.DEPOSIT.name,
                                    amount = if (isDps && account.depositAmount > 0) account.depositAmount else 0.0,
                                    category = if (isDps) "DPS Installment" else "Savings Deposit",
                                    targetAccountId = account.id,
                                    description = "Deposit into ${account.name}"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Deposit", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.openAddTransaction(
                                TransactionEntity(
                                    type = TransactionType.WITHDRAWAL.name,
                                    amount = 0.0,
                                    category = "Savings Withdrawal",
                                    sourceAccountId = account.id,
                                    description = "Withdrawal from ${account.name}"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Withdraw", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.openAddTransaction(
                                TransactionEntity(
                                    type = TransactionType.TRANSFER.name,
                                    amount = 0.0,
                                    category = "Account Transfer",
                                    sourceAccountId = account.id,
                                    description = "Transfer from ${account.name}"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Transfer", fontSize = 12.sp)
                    }
                }
            }

            // DPS Installment Progress Card
            if (isDps && dpsSummary != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("DPS Installment Tracker", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                }
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "${account.dayOfMonthDue}th of every month",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            val progress = if (dpsSummary.totalInstallments > 0) {
                                (dpsSummary.paidInstallments.toFloat() / dpsSummary.totalInstallments.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .size(height = 8.dp, width = 0.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = EmeraldIncome,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // 4-box DPS stats
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Paid", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp))
                                    Text("${dpsSummary.paidInstallments} Inst.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldIncome))
                                }
                                Column {
                                    Text("Missed", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp))
                                    Text("${dpsSummary.missedInstallments} Inst.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (dpsSummary.missedInstallments > 0) RoseExpense else MaterialTheme.colorScheme.onSurface))
                                }
                                Column {
                                    Text("Remaining", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp))
                                    Text("${dpsSummary.remainingInstallments} Inst.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Plan", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp))
                                    Text("${dpsSummary.totalInstallments} Inst.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            // Maturity Estimate
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Est. Maturity Value:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                        Text("$currencySymbol ${CurrencyUtils.formatWithoutSymbol(dpsSummary.estimatedMaturityAmount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldIncome, fontSize = 11.sp))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Est. Total Interest:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                        Text("$currencySymbol ${CurrencyUtils.formatWithoutSymbol(dpsSummary.estimatedTotalInterest)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
                                    }
                                    if (dpsSummary.nextDueDate != null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Next Due Date:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                            Text(DateUtils.formatDate(dpsSummary.nextDueDate, dateFormat), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fixed Deposit Compound Interest Card
            if (isFd && fdEstimate != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    Icon(imageVector = Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("FDR Compound Interest Model", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                }
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = account.compoundingFrequency,
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Principal", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                                    Text("$currencySymbol ${CurrencyUtils.formatWithoutSymbol(fdEstimate.principal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                                Column {
                                    Text("Est. Interest", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                                    Text("$currencySymbol ${CurrencyUtils.formatWithoutSymbol(fdEstimate.estimatedInterest)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldIncome))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Maturity Value", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                                    Text("$currencySymbol ${CurrencyUtils.formatWithoutSymbol(fdEstimate.maturityAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                }
                            }

                            account.maturityDate?.let { matDate ->
                                val daysLeft = DateUtils.daysBetween(System.currentTimeMillis(), matDate)
                                Text(
                                    text = "Matures on ${DateUtils.formatDate(matDate, dateFormat)} ($daysLeft days remaining)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Export Statement Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account Transactions (${accountTransactions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )

                    OutlinedButton(
                        onClick = {
                            val income = accountTransactions.filter { it.type == TransactionType.INCOME.name || it.type == TransactionType.DEPOSIT.name }.sumOf { it.amount }
                            val expense = accountTransactions.filter { it.type == TransactionType.EXPENSE.name || it.type == TransactionType.WITHDRAWAL.name }.sumOf { it.amount }
                            val file = PdfReportGenerator.generateReportPdf(
                                context,
                                ReportData(
                                    title = "Account Statement: ${account.name}",
                                    period = "All Time",
                                    transactions = accountTransactions,
                                    accounts = accountsMap,
                                    totalIncome = income,
                                    totalExpense = expense,
                                    netSavings = account.currentBalance,
                                    currencySymbol = currencySymbol
                                )
                            )
                            PrintReportHelper.shareFile(context, file, "application/pdf", "Account Statement")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("PDF Statement", modifier = Modifier.padding(start = 4.dp), fontSize = 11.sp)
                    }
                }
            }

            if (accountTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions yet for this account.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                items(accountTransactions) { tx ->
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

    if (showEditDialog) {
        AddAccountDialog(
            accountToEdit = account,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.saveAccount(updated) {
                    showEditDialog = false
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete \"${account.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(account) {
                            showDeleteConfirm = false
                            viewModel.navigateTo(AppScreen.SAVINGS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
