package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CompoundingFrequency
import com.example.data.local.entity.CustomCategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepository(
    private val database: AppDatabase,
    private val preferences: AppPreferences
) {
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val categoryDao = database.customCategoryDao()

    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccountsFlow()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgetsFlow()
    val allCategories: Flow<List<CustomCategoryEntity>> = categoryDao.getAllCategoriesFlow()

    fun getAccountFlow(id: Long): Flow<AccountEntity?> = accountDao.getAccountByIdFlow(id)

    fun getTransactionsForAccountFlow(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForAccountFlow(accountId)

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(start, end)

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(month, year)

    suspend fun getBudgetsForMonthSync(month: Int, year: Int): List<BudgetEntity> =
        withContext(Dispatchers.IO) {
            budgetDao.getBudgetsForMonthSync(month, year)
        }

    suspend fun getTransactionsForAccountSync(accountId: Long): List<TransactionEntity> =
        withContext(Dispatchers.IO) {
            transactionDao.getTransactionsForAccount(accountId)
        }

    suspend fun getAllTransactionsSync(): List<TransactionEntity> =
        withContext(Dispatchers.IO) {
            transactionDao.getAllTransactions()
        }

    suspend fun getAllAccountsSync(): List<AccountEntity> =
        withContext(Dispatchers.IO) {
            accountDao.getAllAccounts()
        }

    // --- Accounts Management ---
    suspend fun addAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        val initialBal = account.initialBalance
        val accountToSave = account.copy(currentBalance = initialBal)
        val id = accountDao.insertAccount(accountToSave)

        // If there's an initial balance > 0, record an opening balance transaction
        if (initialBal > 0) {
            transactionDao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.DEPOSIT.name,
                    amount = initialBal,
                    date = account.openingDate,
                    category = "Opening Balance",
                    paymentMethod = "Initial Deposit",
                    description = "Initial balance for ${account.name}",
                    targetAccountId = id
                )
            )
        }
        id
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.deleteAccount(account)
    }

    // --- Transactions Management with Balance Updates ---
    suspend fun addTransaction(tx: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val txId = transactionDao.insertTransaction(tx)
        applyBalanceEffect(tx, isReverse = false)
        txId
    }

    suspend fun updateTransaction(newTx: TransactionEntity) = withContext(Dispatchers.IO) {
        val oldTx = transactionDao.getTransactionById(newTx.id)
        if (oldTx != null) {
            // Revert old effect
            applyBalanceEffect(oldTx, isReverse = true)
        }
        transactionDao.updateTransaction(newTx)
        // Apply new effect
        applyBalanceEffect(newTx, isReverse = false)
    }

    suspend fun deleteTransaction(tx: TransactionEntity) = withContext(Dispatchers.IO) {
        applyBalanceEffect(tx, isReverse = true)
        transactionDao.deleteTransaction(tx)
    }

    private suspend fun applyBalanceEffect(tx: TransactionEntity, isReverse: Boolean) {
        val multiplier = if (isReverse) -1.0 else 1.0

        when (tx.type) {
            TransactionType.INCOME.name -> {
                // If deposited to an account, increase account balance
                tx.targetAccountId?.let { accId ->
                    val acc = accountDao.getAccountById(accId)
                    if (acc != null) {
                        val newBal = acc.currentBalance + (tx.amount * multiplier)
                        accountDao.updateBalance(accId, newBal)
                    }
                }
            }
            TransactionType.EXPENSE.name -> {
                // If paid from an account, decrease account balance
                tx.sourceAccountId?.let { accId ->
                    val acc = accountDao.getAccountById(accId)
                    if (acc != null) {
                        val newBal = acc.currentBalance - (tx.amount * multiplier)
                        accountDao.updateBalance(accId, newBal)
                    }
                }
            }
            TransactionType.TRANSFER.name -> {
                // Transfer between user's own accounts: Decrement source, increment target
                tx.sourceAccountId?.let { srcId ->
                    val srcAcc = accountDao.getAccountById(srcId)
                    if (srcAcc != null) {
                        val newBal = srcAcc.currentBalance - (tx.amount * multiplier)
                        accountDao.updateBalance(srcId, newBal)
                    }
                }
                tx.targetAccountId?.let { tgtId ->
                    val tgtAcc = accountDao.getAccountById(tgtId)
                    if (tgtAcc != null) {
                        val newBal = tgtAcc.currentBalance + (tx.amount * multiplier)
                        accountDao.updateBalance(tgtId, newBal)
                    }
                }
            }
            TransactionType.DEPOSIT.name -> {
                // Deposit to savings/DPS/FD
                tx.sourceAccountId?.let { srcId ->
                    val srcAcc = accountDao.getAccountById(srcId)
                    if (srcAcc != null) {
                        val newBal = srcAcc.currentBalance - (tx.amount * multiplier)
                        accountDao.updateBalance(srcId, newBal)
                    }
                }
                tx.targetAccountId?.let { tgtId ->
                    val tgtAcc = accountDao.getAccountById(tgtId)
                    if (tgtAcc != null) {
                        val newBal = tgtAcc.currentBalance + (tx.amount * multiplier)
                        accountDao.updateBalance(tgtId, newBal)
                    }
                }
            }
            TransactionType.WITHDRAWAL.name -> {
                // Withdrawal from savings
                tx.sourceAccountId?.let { srcId ->
                    val srcAcc = accountDao.getAccountById(srcId)
                    if (srcAcc != null) {
                        val newBal = srcAcc.currentBalance - (tx.amount * multiplier)
                        accountDao.updateBalance(srcId, newBal)
                    }
                }
                tx.targetAccountId?.let { tgtId ->
                    val tgtAcc = accountDao.getAccountById(tgtId)
                    if (tgtAcc != null) {
                        val newBal = tgtAcc.currentBalance + (tx.amount * multiplier)
                        accountDao.updateBalance(tgtId, newBal)
                    }
                }
            }
            TransactionType.INTEREST.name -> {
                // Actual interest earned on an account
                if (!tx.isEstimatedInterest) {
                    tx.targetAccountId?.let { tgtId ->
                        val tgtAcc = accountDao.getAccountById(tgtId)
                        if (tgtAcc != null) {
                            val newBal = tgtAcc.currentBalance + (tx.amount * multiplier)
                            accountDao.updateBalance(tgtId, newBal)
                        }
                    }
                }
            }
        }
    }

    // --- Budget Management ---
    suspend fun setBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertOrUpdate(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudget(budget)
    }

    // --- Category Management ---
    suspend fun addCategory(category: CustomCategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CustomCategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    // --- Initial Seed & Setup ---
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        if (!preferences.isFirstLaunch) return@withContext

        // Default Categories
        val defaultCategories = listOf(
            // Expense
            CustomCategoryEntity(name = "Food & Dining", type = "EXPENSE", iconName = "Restaurant", colorHex = "#EF4444"),
            CustomCategoryEntity(name = "Rent & Housing", type = "EXPENSE", iconName = "Home", colorHex = "#F59E0B"),
            CustomCategoryEntity(name = "Transport", type = "EXPENSE", iconName = "DirectionsCar", colorHex = "#3B82F6"),
            CustomCategoryEntity(name = "Shopping", type = "EXPENSE", iconName = "ShoppingBag", colorHex = "#EC4899"),
            CustomCategoryEntity(name = "Bills & Utilities", type = "EXPENSE", iconName = "Receipt", colorHex = "#8B5CF6"),
            CustomCategoryEntity(name = "Healthcare", type = "EXPENSE", iconName = "LocalHospital", colorHex = "#10B981"),
            CustomCategoryEntity(name = "Education", type = "EXPENSE", iconName = "School", colorHex = "#06B6D4"),
            CustomCategoryEntity(name = "Travel", type = "EXPENSE", iconName = "Flight", colorHex = "#14B8A6"),
            CustomCategoryEntity(name = "Entertainment", type = "EXPENSE", iconName = "Movie", colorHex = "#F97316"),
            CustomCategoryEntity(name = "Groceries", type = "EXPENSE", iconName = "LocalGroceryStore", colorHex = "#84CC16"),
            CustomCategoryEntity(name = "Others", type = "EXPENSE", iconName = "MoreHoriz", colorHex = "#64748B"),

            // Income
            CustomCategoryEntity(name = "Salary", type = "INCOME", iconName = "AttachMoney", colorHex = "#10B981"),
            CustomCategoryEntity(name = "Business Income", type = "INCOME", iconName = "Store", colorHex = "#3B82F6"),
            CustomCategoryEntity(name = "Bonuses", type = "INCOME", iconName = "CardGiftcard", colorHex = "#F59E0B"),
            CustomCategoryEntity(name = "Interest Earned", type = "INCOME", iconName = "TrendingUp", colorHex = "#8B5CF6"),
            CustomCategoryEntity(name = "Freelance", type = "INCOME", iconName = "Laptop", colorHex = "#06B6D4"),
            CustomCategoryEntity(name = "Investments", type = "INCOME", iconName = "ShowChart", colorHex = "#14B8A6"),
            CustomCategoryEntity(name = "Other Income", type = "INCOME", iconName = "MoreHoriz", colorHex = "#64748B")
        )
        categoryDao.insertDefaultCategories(defaultCategories)

        // Seed realistic starter accounts and transactions for immediate visual delight
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Regular Checking Account
        val mainAccId = accountDao.insertAccount(
            AccountEntity(
                name = "Main Cash & Checking",
                accountType = AccountType.REGULAR_SAVINGS.name,
                institution = "Primary Bank",
                accountNumberOrNickname = "*8821",
                openingDate = now - (60L * 24 * 3600 * 1000),
                initialBalance = 3200.00,
                currentBalance = 3200.00,
                interestRate = 2.0,
                notes = "Daily transaction account"
            )
        )

        // 2. DPS Account (5-year scheme, 36/60 installments)
        val dpsMaturityCal = Calendar.getInstance().apply { add(Calendar.YEAR, 3) }
        val dpsAccId = accountDao.insertAccount(
            AccountEntity(
                name = "Dream Home DPS",
                accountType = AccountType.DPS.name,
                institution = "City Bank",
                accountNumberOrNickname = "DPS-0912",
                openingDate = now - (90L * 24 * 3600 * 1000),
                initialBalance = 1500.00,
                currentBalance = 1500.00,
                interestRate = 8.5,
                depositAmount = 500.00,
                maturityDate = dpsMaturityCal.timeInMillis,
                totalInstallments = 36,
                dayOfMonthDue = 5,
                notes = "3-year monthly savings scheme"
            )
        )

        // 3. Fixed Deposit / FDR
        val fdrMaturityCal = Calendar.getInstance().apply { add(Calendar.MONTH, 9) }
        val fdrAccId = accountDao.insertAccount(
            AccountEntity(
                name = "Fixed Deposit (1-Year)",
                accountType = AccountType.FIXED_DEPOSIT.name,
                institution = "Standard Chartered",
                accountNumberOrNickname = "FD-4420",
                openingDate = now - (90L * 24 * 3600 * 1000),
                initialBalance = 10000.00,
                currentBalance = 10000.00,
                interestRate = 7.5,
                depositAmount = 10000.00,
                maturityDate = fdrMaturityCal.timeInMillis,
                compoundingFrequency = CompoundingFrequency.QUARTERLY.name,
                notes = "High-yield fixed term deposit"
            )
        )

        // 4. Emergency Fund
        val emergencyAccId = accountDao.insertAccount(
            AccountEntity(
                name = "Emergency Reserve",
                accountType = AccountType.EMERGENCY_FUND.name,
                institution = "Reserve Vault",
                accountNumberOrNickname = "SAFE-01",
                openingDate = now - (120L * 24 * 3600 * 1000),
                initialBalance = 5000.00,
                currentBalance = 5000.00,
                interestRate = 4.0,
                notes = "6 months living expenses"
            )
        )

        // Current Month Starter Transactions
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curYear = cal.get(Calendar.YEAR)

        // Monthly Budget
        budgetDao.insertOrUpdate(
            BudgetEntity(
                month = curMonth,
                year = curYear,
                category = "TOTAL",
                amountLimit = 2500.00
            )
        )
        budgetDao.insertOrUpdate(
            BudgetEntity(
                month = curMonth,
                year = curYear,
                category = "Food & Dining",
                amountLimit = 600.00
            )
        )
        budgetDao.insertOrUpdate(
            BudgetEntity(
                month = curMonth,
                year = curYear,
                category = "Rent & Housing",
                amountLimit = 1000.00
            )
        )
        budgetDao.insertOrUpdate(
            BudgetEntity(
                month = curMonth,
                year = curYear,
                category = "Transport",
                amountLimit = 300.00
            )
        )

        // Starter Transactions
        val starterTxs = listOf(
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 4500.00,
                date = now - (5L * 24 * 3600 * 1000),
                category = "Salary",
                paymentMethod = "Bank Transfer",
                description = "Monthly Professional Salary",
                targetAccountId = mainAccId
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 950.00,
                date = now - (4L * 24 * 3600 * 1000),
                category = "Rent & Housing",
                paymentMethod = "Bank Transfer",
                description = "Apartment Monthly Rent",
                sourceAccountId = mainAccId
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 145.50,
                date = now - (3L * 24 * 3600 * 1000),
                category = "Food & Dining",
                paymentMethod = "Credit Card",
                description = "Family Dinner & Groceries",
                sourceAccountId = mainAccId
            ),
            TransactionEntity(
                type = TransactionType.DEPOSIT.name,
                amount = 500.00,
                date = now - (2L * 24 * 3600 * 1000),
                category = "DPS Installment",
                paymentMethod = "Bank Transfer",
                description = "Installment #3 Paid for Dream Home DPS",
                sourceAccountId = mainAccId,
                targetAccountId = dpsAccId,
                installmentMonthIndex = 3
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 65.00,
                date = now - (1L * 24 * 3600 * 1000),
                category = "Transport",
                paymentMethod = "Cash",
                description = "Fuel & Metro Transit",
                sourceAccountId = mainAccId
            ),
            TransactionEntity(
                type = TransactionType.INTEREST.name,
                amount = 62.50,
                date = now - (1L * 24 * 3600 * 1000),
                category = "Interest Earned",
                paymentMethod = "Automatic",
                description = "Quarterly Interest Credit on FDR",
                targetAccountId = fdrAccId,
                isEstimatedInterest = false
            )
        )

        for (tx in starterTxs) {
            transactionDao.insertTransaction(tx)
        }

        preferences.isFirstLaunch = false
    }
}
