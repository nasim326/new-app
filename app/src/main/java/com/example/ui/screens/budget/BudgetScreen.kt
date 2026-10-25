package com.example.ui.screens.budget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BudgetEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.MonthlyHeader
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.RoseExpense
import com.example.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val budgets by viewModel.currentMonthBudgets.collectAsStateWithLifecycle()
    val financials by viewModel.monthlyFinancials.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isCarryForwardEnabled by viewModel.isCarryForwardEnabled.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editingLimitText by remember { mutableStateOf("") }

    val expenseCategories = categories.filter { it.type == "EXPENSE" }.map { it.name }.ifEmpty {
        listOf("Food & Dining", "Rent & Housing", "Transport", "Shopping", "Bills & Utilities", "Healthcare", "Education", "Travel", "Others")
    }

    val totalBudget = budgets.firstOrNull { it.category == "TOTAL" }
    val categoryBudgets = budgets.filter { it.category != "TOTAL" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Budget Planning", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("budget_planner_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month navigation
            item {
                MonthlyHeader(
                    month = month,
                    year = year,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onPickerClick = {}
                )
            }

            // Carry Forward Unspent Budget Toggle
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Carry Forward Unspent Budgets", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "Roll over leftover budget funds into the next month",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                        Switch(
                            checked = isCarryForwardEnabled,
                            onCheckedChange = { viewModel.setCarryForwardBudget(it) }
                        )
                    }
                }
            }

            // Overall Total Budget Card
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
                            Column {
                                Text("Total Monthly Spending Limit", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                Text(
                                    text = if (totalBudget != null) "Active Budget" else "No total limit set",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                )
                            }
                            Button(
                                onClick = {
                                    editingCategory = "TOTAL"
                                    editingLimitText = totalBudget?.amountLimit?.toString() ?: ""
                                    showBudgetDialog = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (totalBudget != null) "Edit Limit" else "Set Limit", fontSize = 12.sp)
                            }
                        }

                        if (totalBudget != null) {
                            val spent = financials.totalExpense
                            val limit = totalBudget.amountLimit
                            val progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
                            val isExceeded = spent > limit
                            val isWarning = !isExceeded && progress >= 0.8f

                            val pColor = when {
                                isExceeded -> RoseExpense
                                isWarning -> AmberWarning
                                else -> EmeraldIncome
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .size(height = 10.dp, width = 0.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = pColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Spent: $currencySymbol${CurrencyUtils.formatWithoutSymbol(spent)} (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                Text("Remaining: $currencySymbol${CurrencyUtils.formatWithoutSymbol((limit - spent).coerceAtLeast(0.0))}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = pColor))
                            }
                        }
                    }
                }
            }

            // Category Budgets Header & Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category Budgets (${categoryBudgets.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                    Button(
                        onClick = {
                            editingCategory = expenseCategories.firstOrNull() ?: "Food & Dining"
                            editingLimitText = ""
                            showBudgetDialog = true
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Add Category Budget", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                    }
                }
            }

            if (categoryBudgets.isEmpty()) {
                item {
                    Text(
                        text = "No category-specific budgets set. Adding limits for food, rent, and transport helps enforce spending targets.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                items(categoryBudgets, key = { it.id }) { b ->
                    val spent = financials.categoryExpenses[b.category] ?: 0.0
                    val limit = b.amountLimit
                    val progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
                    val isExceeded = spent > limit
                    val isWarning = !isExceeded && progress >= 0.8f

                    val pColor = when {
                        isExceeded -> RoseExpense
                        isWarning -> AmberWarning
                        else -> EmeraldIncome
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(b.category, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Limit: $currencySymbol${CurrencyUtils.formatWithoutSymbol(limit)}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = b.category
                                            editingLimitText = b.amountLimit.toString()
                                            showBudgetDialog = true
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteBudget(b) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .size(height = 6.dp, width = 0.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = pColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Spent: $currencySymbol${CurrencyUtils.formatWithoutSymbol(spent)} (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                Text("Remaining: $currencySymbol${CurrencyUtils.formatWithoutSymbol((limit - spent).coerceAtLeast(0.0))}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = pColor, fontSize = 11.sp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Set / Edit Budget Dialog
    if (showBudgetDialog && editingCategory != null) {
        var catExpanded by remember { mutableStateOf(false) }
        var currentCat by remember { mutableStateOf(editingCategory!!) }
        var limitVal by remember { mutableStateOf(editingLimitText) }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(if (currentCat == "TOTAL") "Set Total Monthly Budget" else "Set Category Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (currentCat != "TOTAL") {
                        ExposedDropdownMenuBox(
                            expanded = catExpanded,
                            onExpandedChange = { catExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = currentCat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                expenseCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            currentCat = cat
                                            catExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitVal,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                limitVal = input
                            }
                        },
                        label = { Text("Budget Spending Limit ($currencySymbol)") },
                        placeholder = { Text("e.g. 500.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = limitVal.toDoubleOrNull()
                        if (num != null && num > 0.0) {
                            viewModel.saveBudget(month, year, currentCat, num)
                            showBudgetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
