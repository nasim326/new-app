package com.example.ui.screens.savings

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CompoundingFrequency
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    accountToEdit: AccountEntity? = null,
    currencySymbol: String,
    dateFormat: String,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    val context = LocalContext.current
    val isEditing = accountToEdit != null

    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var selectedType by remember {
        mutableStateOf(accountToEdit?.accountType ?: AccountType.REGULAR_SAVINGS.name)
    }
    var institution by remember { mutableStateOf(accountToEdit?.institution ?: "") }
    var accountNumber by remember { mutableStateOf(accountToEdit?.accountNumberOrNickname ?: "") }
    var openingDate by remember { mutableLongStateOf(accountToEdit?.openingDate ?: System.currentTimeMillis()) }
    var initialBalanceText by remember {
        mutableStateOf(accountToEdit?.initialBalance?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var interestRateText by remember {
        mutableStateOf(accountToEdit?.interestRate?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var depositAmountText by remember {
        mutableStateOf(accountToEdit?.depositAmount?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var totalInstallmentsText by remember {
        mutableStateOf(accountToEdit?.totalInstallments?.let { if (it > 0) it.toString() else "" } ?: "36")
    }
    var compoundingFrequency by remember {
        mutableStateOf(accountToEdit?.compoundingFrequency ?: CompoundingFrequency.ANNUALLY.name)
    }
    var dayOfMonthDueText by remember {
        mutableStateOf(accountToEdit?.dayOfMonthDue?.toString() ?: "5")
    }
    var maturityDate by remember { mutableStateOf(accountToEdit?.maturityDate) }
    var notes by remember { mutableStateOf(accountToEdit?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isDps = selectedType == AccountType.DPS.name || selectedType == AccountType.RECURRING_DEPOSIT.name
    val isFd = selectedType == AccountType.FIXED_DEPOSIT.name

    val openDatePicker = remember {
        val cal = Calendar.getInstance().apply { timeInMillis = openingDate }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply { set(year, month, day) }
                openingDate = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val maturityDatePicker = remember {
        val cal = Calendar.getInstance().apply {
            timeInMillis = maturityDate ?: (System.currentTimeMillis() + (365L * 24 * 3600 * 1000))
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply { set(year, month, day) }
                maturityDate = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Account" else "Add Savings / Deposit Account",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Account Type Chips
                Text("Account Scheme Type", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type.name,
                            onClick = { selectedType = type.name },
                            label = { Text(type.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                // Account Name & Institution
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name *") },
                    placeholder = { Text("e.g. City Bank 5-Yr DPS") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_account_name")
                )

                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Bank or Financial Institution *") },
                    placeholder = { Text("e.g. City Bank, HSBC, Al Rajhi") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_account_institution")
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number / Nickname") },
                    placeholder = { Text("e.g. *8820 or DPS-01") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Opening Date & Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Opening Date", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(DateUtils.formatDate(openingDate, dateFormat), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                    OutlinedButton(onClick = { openDatePicker.show() }) {
                        Text("Change")
                    }
                }

                if (!isEditing) {
                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                initialBalanceText = input
                            }
                        },
                        label = { Text("Initial Balance ($currencySymbol)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Interest Rate
                OutlinedTextField(
                    value = interestRateText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            interestRateText = input
                        }
                    },
                    label = { Text("Annual Interest Rate (% p.a.)") },
                    placeholder = { Text("e.g. 7.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // DPS Specific Fields
                if (isDps) {
                    OutlinedTextField(
                        value = depositAmountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                depositAmountText = input
                            }
                        },
                        label = { Text("Monthly Installment Amount ($currencySymbol) *") },
                        placeholder = { Text("500.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = totalInstallmentsText,
                            onValueChange = { totalInstallmentsText = it },
                            label = { Text("Total Installments") },
                            placeholder = { Text("36") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dayOfMonthDueText,
                            onValueChange = { dayOfMonthDueText = it },
                            label = { Text("Due Day (1-31)") },
                            placeholder = { Text("5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Fixed Deposit Specific Fields
                if (isFd) {
                    OutlinedTextField(
                        value = depositAmountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                depositAmountText = input
                            }
                        },
                        label = { Text("Principal Deposit Amount ($currencySymbol) *") },
                        placeholder = { Text("10000.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    var freqExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = freqExpanded,
                        onExpandedChange = { freqExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = CompoundingFrequency.valueOf(compoundingFrequency).displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Compounding Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = freqExpanded,
                            onDismissRequest = { freqExpanded = false }
                        ) {
                            CompoundingFrequency.values().forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.displayName) },
                                    onClick = {
                                        compoundingFrequency = freq.name
                                        freqExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Maturity Date (for DPS, FD, or any savings scheme)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Maturity Date (Optional)", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            text = maturityDate?.let { DateUtils.formatDate(it, dateFormat) } ?: "Not set",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    OutlinedButton(onClick = { maturityDatePicker.show() }) {
                        Text("Set Date")
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Terms") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter an account name"
                        return@Button
                    }
                    if (institution.isBlank()) {
                        errorMessage = "Please enter the bank or institution name"
                        return@Button
                    }
                    val initBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                    val rate = interestRateText.toDoubleOrNull() ?: 0.0
                    val depAmt = depositAmountText.toDoubleOrNull() ?: 0.0
                    val totalInst = totalInstallmentsText.toIntOrNull() ?: 0
                    val dueDay = dayOfMonthDueText.toIntOrNull() ?: 1

                    errorMessage = null

                    val updatedAccount = (accountToEdit ?: AccountEntity(
                        name = name.trim(),
                        accountType = selectedType,
                        institution = institution.trim(),
                        openingDate = openingDate,
                        initialBalance = initBal,
                        currentBalance = initBal
                    )).copy(
                        name = name.trim(),
                        accountType = selectedType,
                        institution = institution.trim(),
                        accountNumberOrNickname = accountNumber.trim(),
                        openingDate = openingDate,
                        interestRate = rate,
                        depositAmount = depAmt,
                        totalInstallments = totalInst,
                        compoundingFrequency = compoundingFrequency,
                        dayOfMonthDue = dueDay,
                        maturityDate = maturityDate,
                        notes = notes.trim()
                    )

                    onSave(updatedAccount)
                },
                modifier = Modifier.testTag("btn_confirm_account")
            ) {
                Text(if (isEditing) "Update" else "Save Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
