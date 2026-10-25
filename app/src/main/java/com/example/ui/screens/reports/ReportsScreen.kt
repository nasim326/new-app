package com.example.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.MainViewModel
import com.example.ui.ReportKind
import com.example.ui.components.MonthlyHeader
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.RoseExpense
import com.example.util.CsvExporter
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.PdfReportGenerator
import com.example.util.PrintReportHelper
import com.example.util.ReportData
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val selectedReport by viewModel.selectedReportKind.collectAsStateWithLifecycle()
    val reportAccountId by viewModel.reportAccountId.collectAsStateWithLifecycle()

    val accountsMap = accounts.associateBy { it.id }

    // Filter data based on selected report kind and period
    val startOfMonth = DateUtils.getStartOfMonth(month, year)
    val endOfMonth = DateUtils.getEndOfMonth(month, year)
    val startOfYear = DateUtils.getStartOfYear(year)
    val endOfYear = DateUtils.getEndOfYear(year)

    val reportTransactions: List<TransactionEntity> = when (selectedReport) {
        ReportKind.MONTHLY_INCOME_EXPENSE, ReportKind.CATEGORY_WISE, ReportKind.DAILY_EXPENSE, ReportKind.BUDGET_PERFORMANCE -> {
            allTransactions.filter { it.date in startOfMonth..endOfMonth }
        }
        ReportKind.TRANSACTION_STATEMENT -> {
            allTransactions
        }
        ReportKind.ACCOUNT_STATEMENT -> {
            if (reportAccountId != null) {
                allTransactions.filter { it.sourceAccountId == reportAccountId || it.targetAccountId == reportAccountId }
            } else allTransactions
        }
        ReportKind.DPS_REPORT -> {
            allTransactions.filter {
                val acc = it.targetAccountId?.let { id -> accountsMap[id] }
                acc?.accountType == AccountType.DPS.name || acc?.accountType == AccountType.RECURRING_DEPOSIT.name
            }
        }
        ReportKind.FIXED_DEPOSIT_REPORT -> {
            allTransactions.filter {
                val acc = it.targetAccountId?.let { id -> accountsMap[id] }
                acc?.accountType == AccountType.FIXED_DEPOSIT.name
            }
        }
        ReportKind.ANNUAL_SUMMARY -> {
            allTransactions.filter { it.date in startOfYear..endOfYear }
        }
    }

    val totalIncome = reportTransactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalExpense = reportTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense

    val reportingPeriod = when (selectedReport) {
        ReportKind.ANNUAL_SUMMARY -> "Year $year"
        ReportKind.TRANSACTION_STATEMENT -> "Complete Lifetime History"
        ReportKind.ACCOUNT_STATEMENT -> {
            val accName = reportAccountId?.let { accountsMap[it]?.name } ?: "All Accounts"
            "$accName (All Time)"
        }
        else -> "${DateUtils.getMonthName(month)} $year"
    }

    var reportMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Financial Reports & PDF Export",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp)
            )
        }

        // Report Type Dropdown Selector
        item {
            ExposedDropdownMenuBox(
                expanded = reportMenuExpanded,
                onExpandedChange = { reportMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedReport.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Statement / Report Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reportMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("dropdown_report_type"),
                    shape = RoundedCornerShape(14.dp)
                )

                ExposedDropdownMenu(
                    expanded = reportMenuExpanded,
                    onDismissRequest = { reportMenuExpanded = false }
                ) {
                    ReportKind.values().forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(kind.displayName) },
                            onClick = {
                                viewModel.selectedReportKind.value = kind
                                reportMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Account Filter if Account Statement selected
        if (selectedReport == ReportKind.ACCOUNT_STATEMENT && accounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = reportAccountId == null,
                        onClick = { viewModel.reportAccountId.value = null },
                        label = { Text("All Accounts") }
                    )
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = reportAccountId == acc.id,
                            onClick = { viewModel.reportAccountId.value = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }
        }

        // Month Selector for monthly reports
        if (selectedReport in listOf(ReportKind.MONTHLY_INCOME_EXPENSE, ReportKind.CATEGORY_WISE, ReportKind.DAILY_EXPENSE, ReportKind.BUDGET_PERFORMANCE)) {
            item {
                MonthlyHeader(
                    month = month,
                    year = year,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onPickerClick = {}
                )
            }
        }

        // Report Summary Card
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
                        Column {
                            Text(
                                text = selectedReport.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                            Text(
                                text = "Period: $reportingPeriod",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${reportTransactions.size} Records",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                            Text("+$currencySymbol${CurrencyUtils.formatWithoutSymbol(totalIncome)}", style = MaterialTheme.typography.bodyMedium.copy(color = EmeraldIncome, fontWeight = FontWeight.Bold))
                        }
                        Column {
                            Text("Total Expenses", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                            Text("-$currencySymbol${CurrencyUtils.formatWithoutSymbol(totalExpense)}", style = MaterialTheme.typography.bodyMedium.copy(color = RoseExpense, fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Savings", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                            val savingsColor = if (netSavings >= 0) EmeraldIncome else RoseExpense
                            Text("$currencySymbol${CurrencyUtils.formatWithoutSymbol(netSavings)}", style = MaterialTheme.typography.bodyMedium.copy(color = savingsColor, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Export Actions Row (PDF, Print, CSV)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export PDF & Share
                    Button(
                        onClick = {
                            try {
                                val pdfFile = PdfReportGenerator.generateReportPdf(
                                    context,
                                    ReportData(
                                        title = selectedReport.displayName,
                                        period = reportingPeriod,
                                        transactions = reportTransactions,
                                        accounts = accountsMap,
                                        totalIncome = totalIncome,
                                        totalExpense = totalExpense,
                                        netSavings = netSavings,
                                        currencySymbol = currencySymbol
                                    )
                                )
                                PrintReportHelper.shareFile(context, pdfFile, "application/pdf", "Share Financial Report")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_pdf"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Export PDF", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                    }

                    // Native Print
                    Button(
                        onClick = {
                            try {
                                val pdfFile = PdfReportGenerator.generateReportPdf(
                                    context,
                                    ReportData(
                                        title = selectedReport.displayName,
                                        period = reportingPeriod,
                                        transactions = reportTransactions,
                                        accounts = accountsMap,
                                        totalIncome = totalIncome,
                                        totalExpense = totalExpense,
                                        netSavings = netSavings,
                                        currencySymbol = currencySymbol
                                    )
                                )
                                PrintReportHelper.printPdf(context, pdfFile, selectedReport.displayName)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_print_report"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Print", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                    }

                    // Export CSV
                    OutlinedButton(
                        onClick = {
                            try {
                                val csvFile = CsvExporter.exportTransactionsToCsv(context, reportTransactions, accountsMap)
                                PrintReportHelper.shareFile(context, csvFile, "text/csv", "Share CSV Export")
                            } catch (e: Exception) {
                                Toast.makeText(context, "CSV error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_csv"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("CSV", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                    }
                }
            }
        }

        // Preview Table Header
        item {
            Text(
                text = "Statement Preview (${reportTransactions.size} rows)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
        }

        // Rows Preview
        if (reportTransactions.isEmpty()) {
            item {
                Text(
                    text = "No transaction data for this reporting period.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            items(reportTransactions.take(30), key = { it.id }) { tx ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.description.ifBlank { tx.category },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                maxLines = 1
                            )
                            Text(
                                text = "${DateUtils.formatDate(tx.date, dateFormat)} • ${tx.type} • ${tx.category}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp),
                                maxLines = 1
                            )
                        }

                        val amtColor = if (tx.type == TransactionType.EXPENSE.name) RoseExpense else EmeraldIncome
                        val amtSign = if (tx.type == TransactionType.EXPENSE.name) "-" else "+"
                        Text(
                            text = "$amtSign$currencySymbol${CurrencyUtils.formatWithoutSymbol(tx.amount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = amtColor, fontSize = 13.sp)
                        )
                    }
                }
            }
        }
    }
}
