package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CustomCategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.preferences.AppPreferences
import com.example.data.repository.FinanceRepository
import com.example.util.DateUtils
import com.example.util.DpsProgressSummary
import com.example.util.FdMaturityEstimate
import com.example.util.InterestCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppScreen {
    DASHBOARD,
    TRANSACTIONS,
    ADD_TRANSACTION,
    SAVINGS,
    REPORTS,
    SETTINGS,
    BUDGET_PLANNER,
    ACCOUNT_DETAIL
}

enum class SortOrder {
    NEWEST,
    OLDEST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT
}

enum class ReportKind(val displayName: String) {
    MONTHLY_INCOME_EXPENSE("Monthly Income & Expense"),
    DAILY_EXPENSE("Daily Expense Report"),
    CATEGORY_WISE("Category-wise Expense Report"),
    TRANSACTION_STATEMENT("Complete Transaction Statement"),
    ACCOUNT_STATEMENT("Savings Account Statement"),
    DPS_REPORT("DPS Installment Report"),
    FIXED_DEPOSIT_REPORT("Fixed Deposit & Interest Report"),
    BUDGET_PERFORMANCE("Monthly Budget Performance"),
    ANNUAL_SUMMARY("Annual Financial Summary")
}

data class UpcomingReminder(
    val accountId: Long,
    val title: String,
    val subtitle: String,
    val dueDate: Long,
    val amount: Double,
    val type: String // "DPS" or "FD"
)

data class MonthlyFinancials(
    val totalIncome: Double,
    val totalExpense: Double,
    val remainingBalance: Double,
    val totalSavingsDeposited: Double,
    val totalSavingsBalance: Double,
    val categoryExpenses: Map<String, Double>,
    val budgetLimit: Double,
    val budgetSpent: Double,
    val budgetRemaining: Double,
    val budgetPercentSpent: Float
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getDatabase(application)
    val preferences = AppPreferences(application)
    val repository = FinanceRepository(database, preferences)

    // Navigation & Screen State
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedAccountForDetail = MutableStateFlow<AccountEntity?>(null)
    val selectedAccountForDetail: StateFlow<AccountEntity?> = _selectedAccountForDetail.asStateFlow()

    private val _transactionToEdit = MutableStateFlow<TransactionEntity?>(null)
    val transactionToEdit: StateFlow<TransactionEntity?> = _transactionToEdit.asStateFlow()

    // App Lock State
    private val _isAppUnlocked = MutableStateFlow(!preferences.isPinLockEnabled)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Selected Month & Year (defaults to current)
    private val _selectedMonth = MutableStateFlow(DateUtils.currentMonth())
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(DateUtils.currentYear())
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Currency & Settings State
    private val _currencySymbol = MutableStateFlow(preferences.currencySymbol)
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _currencyCode = MutableStateFlow(preferences.currencyCode)
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    private val _dateFormat = MutableStateFlow(preferences.dateFormat)
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _themeMode = MutableStateFlow(preferences.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isPinLockEnabled = MutableStateFlow(preferences.isPinLockEnabled)
    val isPinLockEnabled: StateFlow<Boolean> = _isPinLockEnabled.asStateFlow()

    private val _isCarryForwardEnabled = MutableStateFlow(preferences.isCarryForwardBudgetEnabled)
    val isCarryForwardEnabled: StateFlow<Boolean> = _isCarryForwardEnabled.asStateFlow()

    // Transaction History Filters
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<String?>("ALL")
    val filterCategory = MutableStateFlow<String?>("ALL")
    val filterAccountId = MutableStateFlow<Long?>(null)
    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)
    val filterMinAmount = MutableStateFlow<Double?>(null)
    val filterMaxAmount = MutableStateFlow<Double?>(null)
    val sortOrder = MutableStateFlow(SortOrder.NEWEST)

    // Reports State
    val selectedReportKind = MutableStateFlow(ReportKind.MONTHLY_INCOME_EXPENSE)
    val reportAccountId = MutableStateFlow<Long?>(null)

    // Data streams from repository
    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CustomCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Month Budgets
    val currentMonthBudgets: StateFlow<List<BudgetEntity>> = combine(
        allBudgets, _selectedMonth, _selectedYear
    ) { budgets, month, year ->
        budgets.filter { it.month == month && it.year == year }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Financials calculation
    val monthlyFinancials: StateFlow<MonthlyFinancials> = combine(
        allTransactions, accounts, currentMonthBudgets, _selectedMonth, _selectedYear
    ) { txs, accs, budgets, month, year ->
        val start = DateUtils.getStartOfMonth(month, year)
        val end = DateUtils.getEndOfMonth(month, year)

        val monthTxs = txs.filter { it.date in start..end }

        val income = monthTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val expense = monthTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val savingsDeposited = monthTxs.filter { it.type == TransactionType.DEPOSIT.name }.sumOf { it.amount }
        val remaining = income - expense

        val totalSavingsBal = accs.filter {
            it.accountType != AccountType.CASH.name
        }.sumOf { it.currentBalance }

        val categoryExpMap = monthTxs
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val totalBudgetObj = budgets.firstOrNull { it.category == "TOTAL" }
        val budgetLimit = totalBudgetObj?.amountLimit ?: budgets.filter { it.category != "TOTAL" }.sumOf { it.amountLimit }
        val budgetSpent = expense
        val budgetRemaining = (budgetLimit - budgetSpent).coerceAtLeast(0.0)
        val percentSpent = if (budgetLimit > 0) ((budgetSpent / budgetLimit) * 100).toFloat() else 0f

        MonthlyFinancials(
            totalIncome = income,
            totalExpense = expense,
            remainingBalance = remaining,
            totalSavingsDeposited = savingsDeposited,
            totalSavingsBalance = totalSavingsBal,
            categoryExpenses = categoryExpMap,
            budgetLimit = budgetLimit,
            budgetSpent = budgetSpent,
            budgetRemaining = budgetRemaining,
            budgetPercentSpent = percentSpent
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyFinancials(0.0, 0.0, 0.0, 0.0, 0.0, emptyMap(), 0.0, 0.0, 0.0, 0f)
    )

    // Upcoming Reminders (DPS due dates, Fixed Deposit maturities)
    val upcomingReminders: StateFlow<List<UpcomingReminder>> = combine(
        accounts, allTransactions
    ) { accs, txs ->
        val reminders = mutableListOf<UpcomingReminder>()
        val now = System.currentTimeMillis()

        for (acc in accs) {
            when (acc.accountType) {
                AccountType.DPS.name, AccountType.RECURRING_DEPOSIT.name -> {
                    val summary = InterestCalculator.analyzeDps(acc, txs)
                    if (summary.nextDueDate != null) {
                        reminders.add(
                            UpcomingReminder(
                                accountId = acc.id,
                                title = "DPS Installment Due: ${acc.name}",
                                subtitle = "Installment #${summary.paidInstallments + 1} (${acc.institution})",
                                dueDate = summary.nextDueDate,
                                amount = acc.depositAmount,
                                type = "DPS"
                            )
                        )
                    }
                }
                AccountType.FIXED_DEPOSIT.name -> {
                    acc.maturityDate?.let { matDate ->
                        if (matDate >= now) {
                            val daysLeft = DateUtils.daysBetween(now, matDate)
                            if (daysLeft <= 60) {
                                val est = InterestCalculator.calculateFdMaturity(
                                    acc.depositAmount.takeIf { it > 0 } ?: acc.initialBalance,
                                    acc.interestRate,
                                    acc.openingDate,
                                    acc.maturityDate,
                                    acc.compoundingFrequency
                                )
                                reminders.add(
                                    UpcomingReminder(
                                        accountId = acc.id,
                                        title = "FD Maturing in $daysLeft days",
                                        subtitle = "${acc.name} (${acc.institution}) - Est. Maturity Value",
                                        dueDate = matDate,
                                        amount = est.maturityAmount,
                                        type = "FD"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        reminders.sortedBy { it.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Transactions for Transactions Screen
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions, searchQuery, filterType, filterCategory, filterAccountId,
        filterStartDate, filterEndDate, filterMinAmount, filterMaxAmount, sortOrder
    ) { params ->
        val txs = params[0] as List<TransactionEntity>
        val query = (params[1] as String).trim().lowercase()
        val type = params[2] as String?
        val category = params[3] as String?
        val accountId = params[4] as Long?
        val start = params[5] as Long?
        val end = params[6] as Long?
        val minAmt = params[7] as Double?
        val maxAmt = params[8] as Double?
        val sort = params[9] as SortOrder

        var result = txs

        if (query.isNotEmpty()) {
            result = result.filter {
                it.description.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.paymentMethod.lowercase().contains(query)
            }
        }

        if (type != null && type != "ALL") {
            result = result.filter { it.type == type }
        }

        if (category != null && category != "ALL") {
            result = result.filter { it.category == category }
        }

        if (accountId != null) {
            result = result.filter { it.sourceAccountId == accountId || it.targetAccountId == accountId }
        }

        if (start != null) {
            result = result.filter { it.date >= start }
        }

        if (end != null) {
            result = result.filter { it.date <= end }
        }

        if (minAmt != null) {
            result = result.filter { it.amount >= minAmt }
        }

        if (maxAmt != null) {
            result = result.filter { it.amount <= maxAmt }
        }

        when (sort) {
            SortOrder.NEWEST -> result.sortedByDescending { it.date }
            SortOrder.OLDEST -> result.sortedBy { it.date }
            SortOrder.HIGHEST_AMOUNT -> result.sortedByDescending { it.amount }
            SortOrder.LOWEST_AMOUNT -> result.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    // --- Navigation ---
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openAccountDetail(account: AccountEntity) {
        _selectedAccountForDetail.value = account
        _currentScreen.value = AppScreen.ACCOUNT_DETAIL
    }

    fun openAddTransaction(txToEdit: TransactionEntity? = null) {
        _transactionToEdit.value = txToEdit
        _currentScreen.value = AppScreen.ADD_TRANSACTION
    }

    // --- Month Selection ---
    fun setMonthAndYear(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value = _selectedYear.value - 1
        } else {
            _selectedMonth.value = _selectedMonth.value - 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value = _selectedYear.value + 1
        } else {
            _selectedMonth.value = _selectedMonth.value + 1
        }
    }

    // --- Transactions Actions ---
    fun saveTransaction(
        id: Long = 0,
        type: String,
        amount: Double,
        date: Long,
        category: String,
        paymentMethod: String,
        description: String,
        sourceAccountId: Long?,
        targetAccountId: Long?,
        isEstimatedInterest: Boolean = false,
        installmentMonthIndex: Int? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                id = id,
                type = type,
                amount = amount,
                date = date,
                category = category,
                paymentMethod = paymentMethod,
                description = description,
                sourceAccountId = sourceAccountId,
                targetAccountId = targetAccountId,
                isEstimatedInterest = isEstimatedInterest,
                installmentMonthIndex = installmentMonthIndex
            )

            if (id == 0L) {
                repository.addTransaction(tx)
            } else {
                repository.updateTransaction(tx)
            }
            onComplete()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    // --- Accounts Actions ---
    fun saveAccount(account: AccountEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (account.id == 0L) {
                repository.addAccount(account)
            } else {
                repository.updateAccount(account)
            }
            onComplete()
        }
    }

    fun deleteAccount(account: AccountEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            onComplete()
        }
    }

    // --- Budget Actions ---
    fun saveBudget(month: Int, year: Int, category: String, limit: Double) {
        viewModelScope.launch {
            repository.setBudget(
                BudgetEntity(
                    month = month,
                    year = year,
                    category = category,
                    amountLimit = limit,
                    carryForwardUnspent = _isCarryForwardEnabled.value
                )
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // --- Categories Actions ---
    fun addCustomCategory(name: String, type: String, colorHex: String) {
        viewModelScope.launch {
            repository.addCategory(
                CustomCategoryEntity(
                    name = name,
                    type = type,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteCustomCategory(category: CustomCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // --- Settings Actions ---
    fun setCurrency(code: String, symbol: String) {
        preferences.currencyCode = code
        preferences.currencySymbol = symbol
        _currencyCode.value = code
        _currencySymbol.value = symbol
    }

    fun setDateFormat(pattern: String) {
        preferences.dateFormat = pattern
        _dateFormat.value = pattern
    }

    fun setThemeMode(mode: String) {
        preferences.themeMode = mode
        _themeMode.value = mode
    }

    fun setCarryForwardBudget(enabled: Boolean) {
        preferences.isCarryForwardBudgetEnabled = enabled
        _isCarryForwardEnabled.value = enabled
    }

    fun setupPin(pin: String) {
        preferences.setPin(pin)
        _isPinLockEnabled.value = true
        _isAppUnlocked.value = true
    }

    fun disablePin() {
        preferences.disablePin()
        _isPinLockEnabled.value = false
        _isAppUnlocked.value = true
    }

    fun unlockApp(pin: String): Boolean {
        val success = preferences.verifyPin(pin)
        if (success) {
            _isAppUnlocked.value = true
        }
        return success
    }

    fun lockApp() {
        if (preferences.isPinLockEnabled) {
            _isAppUnlocked.value = false
        }
    }
}
