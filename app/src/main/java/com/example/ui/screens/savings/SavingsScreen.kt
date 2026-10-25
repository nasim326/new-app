package com.example.ui.screens.savings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.ui.MainViewModel
import com.example.ui.components.AccountCard
import com.example.util.CurrencyUtils
import com.example.util.InterestCalculator

@Composable
fun SavingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()

    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredAccounts = if (selectedTypeFilter == "ALL") {
        accounts
    } else {
        accounts.filter { it.accountType == selectedTypeFilter }
    }

    val totalSavings = accounts.filter { it.accountType != AccountType.CASH.name }.sumOf { it.currentBalance }
    val dpsTotal = accounts.filter { it.accountType == AccountType.DPS.name || it.accountType == AccountType.RECURRING_DEPOSIT.name }.sumOf { it.currentBalance }
    val fdTotal = accounts.filter { it.accountType == AccountType.FIXED_DEPOSIT.name }.sumOf { it.currentBalance }
    val regTotal = accounts.filter { it.accountType == AccountType.REGULAR_SAVINGS.name }.sumOf { it.currentBalance }

    val filterOptions = listOf("ALL") + AccountType.values().map { it.name }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_account")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Savings Account")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("savings_screen")
        ) {
            // Screen Header & Total Savings Portfolio Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Savings & Deposit Accounts",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
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
                                text = "TOTAL SAVINGS PORTFOLIO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Text(
                            text = "$currencySymbol ${CurrencyUtils.formatWithoutSymbol(totalSavings)}",
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
                                text = "DPS: $currencySymbol${CurrencyUtils.formatWithoutSymbol(dpsTotal)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                            )
                            Text(
                                text = "FD: $currencySymbol${CurrencyUtils.formatWithoutSymbol(fdTotal)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                            )
                            Text(
                                text = "Regular: $currencySymbol${CurrencyUtils.formatWithoutSymbol(regTotal)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                            )
                        }
                    }
                }
            }

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { typeName ->
                    val isSelected = selectedTypeFilter == typeName
                    val label = if (typeName == "ALL") "All Accounts (${accounts.size})" else AccountType.valueOf(typeName).displayName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = typeName },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("chip_acc_type_$typeName")
                    )
                }
            }

            // Accounts List
            if (filteredAccounts.isEmpty()) {
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
                            text = "No accounts found in this category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("btn_create_acc_empty")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Text("Create Savings Account", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAccounts, key = { it.id }) { acc ->
                        val isDps = acc.accountType == AccountType.DPS.name || acc.accountType == AccountType.RECURRING_DEPOSIT.name
                        val isFd = acc.accountType == AccountType.FIXED_DEPOSIT.name

                        val dpsSummary = if (isDps) InterestCalculator.analyzeDps(acc, allTransactions) else null
                        val fdEstimate = if (isFd) {
                            InterestCalculator.calculateFdMaturity(
                                acc.depositAmount.takeIf { it > 0 } ?: acc.initialBalance,
                                acc.interestRate,
                                acc.openingDate,
                                acc.maturityDate,
                                acc.compoundingFrequency
                            )
                        } else null

                        AccountCard(
                            account = acc,
                            currencySymbol = currencySymbol,
                            dateFormat = dateFormat,
                            dpsSummary = dpsSummary,
                            fdEstimate = fdEstimate,
                            onClick = { viewModel.openAccountDetail(acc) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            onDismiss = { showAddDialog = false },
            onSave = { newAccount ->
                viewModel.saveAccount(newAccount) {
                    showAddDialog = false
                }
            }
        )
    }
}
