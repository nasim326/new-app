package com.example.ui.screens.add

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.EmeraldIncome
import com.example.ui.theme.RoseExpense
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val txToEdit by viewModel.transactionToEdit.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    val isEditing = txToEdit != null

    var selectedType by remember(txToEdit) {
        mutableStateOf(txToEdit?.type ?: TransactionType.EXPENSE.name)
    }

    var amountText by remember(txToEdit) {
        mutableStateOf(txToEdit?.amount?.let { if (it > 0) it.toString() else "" } ?: "")
    }

    var selectedDate by remember(txToEdit) {
        mutableLongStateOf(txToEdit?.date ?: System.currentTimeMillis())
    }

    var selectedCategory by remember(txToEdit, selectedType) {
        mutableStateOf(
            txToEdit?.category ?: if (selectedType == TransactionType.INCOME.name) "Salary" else "Food & Dining"
        )
    }

    var selectedPaymentMethod by remember(txToEdit) {
        mutableStateOf(txToEdit?.paymentMethod ?: "Cash")
    }

    var description by remember(txToEdit) {
        mutableStateOf(txToEdit?.description ?: "")
    }

    var sourceAccountId by remember(txToEdit) {
        mutableStateOf<Long?>(txToEdit?.sourceAccountId)
    }

    var targetAccountId by remember(txToEdit) {
        mutableStateOf<Long?>(txToEdit?.targetAccountId)
    }

    var isEstimatedInterest by remember(txToEdit) {
        mutableStateOf(txToEdit?.isEstimatedInterest ?: false)
    }

    var installmentMonthIndexText by remember(txToEdit) {
        mutableStateOf(txToEdit?.installmentMonthIndex?.toString() ?: "")
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Date Picker Dialog setup
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth)
                selectedDate = newCal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val paymentMethods = listOf("Cash", "Bank Transfer", "Credit Card", "Debit Card", "Mobile Banking", "Cheque", "Other")

    // Filter categories matching current type
    val categoryList = categories
        .filter {
            if (selectedType == TransactionType.INCOME.name) it.type == "INCOME" else it.type == "EXPENSE"
        }
        .map { it.name }
        .ifEmpty {
            if (selectedType == TransactionType.INCOME.name) {
                listOf("Salary", "Business Income", "Bonuses", "Interest", "Freelance", "Other Income")
            } else {
                listOf("Food & Dining", "Rent & Housing", "Transport", "Shopping", "Bills & Utilities", "Healthcare", "Education", "Travel", "Others")
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("btn_delete_tx")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("add_tx_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector Chips
            Text(text = "Transaction Type", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.values().forEach { type ->
                    val isSelected = selectedType == type.name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedType = type.name
                            if (type == TransactionType.INCOME) {
                                selectedCategory = "Salary"
                            } else if (type == TransactionType.EXPENSE) {
                                selectedCategory = "Food & Dining"
                            } else if (type == TransactionType.TRANSFER) {
                                selectedCategory = "Account Transfer"
                            } else if (type == TransactionType.DEPOSIT) {
                                selectedCategory = "DPS / Deposit"
                            } else if (type == TransactionType.INTEREST) {
                                selectedCategory = "Interest Earned"
                            }
                        },
                        label = { Text(type.displayName, fontSize = 12.sp) },
                        modifier = Modifier.testTag("type_chip_${type.name}")
                    )
                }
            }

            // Amount Input Card with Currency Symbol
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                        amountText = input
                    }
                },
                label = { Text("Amount ($currencySymbol)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = when (selectedType) {
                        TransactionType.EXPENSE.name -> RoseExpense
                        TransactionType.INCOME.name -> EmeraldIncome
                        else -> MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_amount")
            )

            // Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Date", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(
                        text = DateUtils.formatDate(selectedDate, dateFormat),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.testTag("btn_pick_date")
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Change Date", modifier = Modifier.padding(start = 6.dp))
                }
            }

            // Category Selection (with dropdown and custom addition)
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("dropdown_category"),
                    shape = RoundedCornerShape(14.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categoryList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("+ Add Custom Category", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                            }
                        },
                        onClick = {
                            categoryExpanded = false
                            showAddCategoryDialog = true
                        }
                    )
                }
            }

            // Account Selection depending on transaction type
            if (accounts.isNotEmpty()) {
                when (selectedType) {
                    TransactionType.EXPENSE.name -> {
                        AccountSelector(
                            label = "Paid From Account",
                            selectedAccountId = sourceAccountId,
                            accounts = accounts,
                            onSelect = { sourceAccountId = it }
                        )
                    }
                    TransactionType.INCOME.name -> {
                        AccountSelector(
                            label = "Deposit To Account (Optional)",
                            selectedAccountId = targetAccountId,
                            accounts = accounts,
                            onSelect = { targetAccountId = it }
                        )
                    }
                    TransactionType.TRANSFER.name -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountSelector(
                                label = "Transfer From Account *",
                                selectedAccountId = sourceAccountId,
                                accounts = accounts,
                                onSelect = { sourceAccountId = it }
                            )
                            AccountSelector(
                                label = "Transfer To Account *",
                                selectedAccountId = targetAccountId,
                                accounts = accounts,
                                onSelect = { targetAccountId = it }
                            )
                            Text(
                                text = "Note: Transfers move funds between your accounts without counting as income or expense.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                    }
                    TransactionType.DEPOSIT.name -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountSelector(
                                label = "Target Savings/DPS Account *",
                                selectedAccountId = targetAccountId,
                                accounts = accounts,
                                onSelect = { targetAccountId = it }
                            )
                            AccountSelector(
                                label = "Source Account (Paid From)",
                                selectedAccountId = sourceAccountId,
                                accounts = accounts,
                                onSelect = { sourceAccountId = it }
                            )
                        }
                    }
                    TransactionType.WITHDRAWAL.name -> {
                        AccountSelector(
                            label = "Withdraw From Savings Account *",
                            selectedAccountId = sourceAccountId,
                            accounts = accounts,
                            onSelect = { sourceAccountId = it }
                        )
                    }
                    TransactionType.INTEREST.name -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountSelector(
                                label = "Account Credited *",
                                selectedAccountId = targetAccountId,
                                accounts = accounts,
                                onSelect = { targetAccountId = it }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Estimated vs Actual", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        text = if (isEstimatedInterest) "Estimated (Projection only, does not alter balance)" else "Actual Interest (Credited to balance)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    )
                                }
                                Switch(
                                    checked = !isEstimatedInterest,
                                    onCheckedChange = { isEstimatedInterest = !it }
                                )
                            }
                        }
                    }
                }
            }

            // DPS Installment Month Number if applicable
            if (selectedType == TransactionType.DEPOSIT.name) {
                OutlinedTextField(
                    value = installmentMonthIndexText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*$"))) {
                            installmentMonthIndexText = input
                        }
                    },
                    label = { Text("Installment Number (e.g., 3 for month #3)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment Method
            Text(text = "Payment Method", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { selectedPaymentMethod = method },
                        label = { Text(method, fontSize = 12.sp) }
                    )
                }
            }

            // Description / Notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                placeholder = { Text("e.g., Weekly groceries, Monthly salary...") },
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_description")
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Action Button
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (selectedType == TransactionType.TRANSFER.name && sourceAccountId == targetAccountId) {
                        errorMessage = "Source and Target accounts must be different for transfers"
                        return@Button
                    }
                    errorMessage = null

                    viewModel.saveTransaction(
                        id = txToEdit?.id ?: 0L,
                        type = selectedType,
                        amount = amt,
                        date = selectedDate,
                        category = selectedCategory,
                        paymentMethod = selectedPaymentMethod,
                        description = description,
                        sourceAccountId = sourceAccountId,
                        targetAccountId = targetAccountId,
                        isEstimatedInterest = isEstimatedInterest,
                        installmentMonthIndex = installmentMonthIndexText.toIntOrNull()
                    ) {
                        viewModel.navigateTo(AppScreen.DASHBOARD)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_tx"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Text(
                    text = if (isEditing) "Update Transaction" else "Save Transaction",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && txToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to permanently delete this transaction? Any account balances affected by this transaction will be automatically restored.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(txToEdit!!)
                        showDeleteConfirm = false
                        viewModel.navigateTo(AppScreen.DASHBOARD)
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

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Custom Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.addCustomCategory(
                                name = newCatName.trim(),
                                type = if (selectedType == TransactionType.INCOME.name) "INCOME" else "EXPENSE",
                                colorHex = "#3B82F6"
                            )
                            selectedCategory = newCatName.trim()
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelector(
    label: String,
    selectedAccountId: Long?,
    accounts: List<AccountEntity>,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAcc = accounts.firstOrNull { it.id == selectedAccountId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedAcc?.let { "${it.name} (${it.institution})" } ?: "None / Cash",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None / Cash Outside Account") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text("${acc.name} (${acc.institution}) - Bal: $${CurrencyUtils.formatWithoutSymbol(acc.currentBalance)}") },
                    onClick = {
                        onSelect(acc.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
