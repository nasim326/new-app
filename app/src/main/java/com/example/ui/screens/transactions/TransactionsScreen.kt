package com.example.ui.screens.transactions

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.SortOrder
import com.example.ui.components.TransactionItem
import com.example.util.CurrencyUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val accountsMap = accounts.associateBy { it.id }

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Calculated totals for filtered view
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val totalTransfers = transactions.filter { it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }

    val typeOptions = listOf("ALL") + TransactionType.values().map { it.name }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddTransaction() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_tx")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("transactions_screen")
        ) {
            // Top Search Bar & Filter Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search transactions, notes...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_search_tx")
                )

                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.testTag("btn_filter_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Type Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeOptions.forEach { typeName ->
                    val isSelected = filterType == typeName
                    val label = if (typeName == "ALL") "All Types" else TransactionType.valueOf(typeName).displayName
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.filterType.value = typeName },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("chip_type_$typeName")
                    )
                }
            }

            // Summary Pill for Current Filters
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${transactions.size} records found",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (totalIncome > 0) {
                            Text(
                                text = "+$currencySymbol${CurrencyUtils.formatWithoutSymbol(totalIncome)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        if (totalExpense > 0) {
                            Text(
                                text = "-$currencySymbol${CurrencyUtils.formatWithoutSymbol(totalExpense)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No matching transactions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Try adjusting your search terms or filter criteria",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.filterType.value = "ALL"
                                viewModel.filterCategory.value = "ALL"
                                viewModel.filterAccountId.value = null
                                viewModel.filterStartDate.value = null
                                viewModel.filterEndDate.value = null
                            }
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
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
    }

    // Filter & Sort Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Filter & Sort Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Sort Options
                Text(text = "Sort Order", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortOrder.values().forEach { order ->
                        val isSel = sortOrder == order
                        val label = when (order) {
                            SortOrder.NEWEST -> "Newest"
                            SortOrder.OLDEST -> "Oldest"
                            SortOrder.HIGHEST_AMOUNT -> "Highest"
                            SortOrder.LOWEST_AMOUNT -> "Lowest"
                        }
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.sortOrder.value = order },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Filter by Account
                if (accounts.isNotEmpty()) {
                    Text(text = "Filter by Account", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterAccId by viewModel.filterAccountId.collectAsStateWithLifecycle()
                        FilterChip(
                            selected = filterAccId == null,
                            onClick = { viewModel.filterAccountId.value = null },
                            label = { Text("All Accounts", fontSize = 11.sp) }
                        )
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = filterAccId == acc.id,
                                onClick = { viewModel.filterAccountId.value = acc.id },
                                label = { Text(acc.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.filterType.value = "ALL"
                            viewModel.filterCategory.value = "ALL"
                            viewModel.filterAccountId.value = null
                            viewModel.searchQuery.value = ""
                            viewModel.sortOrder.value = SortOrder.NEWEST
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
