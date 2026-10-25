package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CustomCategoryEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupRestoreHelper {

    suspend fun createBackupJsonFile(context: Context, database: AppDatabase): File =
        withContext(Dispatchers.IO) {
            val root = JSONObject()
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())
            root.put("appName", "My Budget & Savings Tracker")

            // Accounts
            val accounts = database.accountDao().getAllAccounts()
            val accountsArray = JSONArray()
            for (acc in accounts) {
                val accObj = JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("accountType", acc.accountType)
                    put("institution", acc.institution)
                    put("accountNumberOrNickname", acc.accountNumberOrNickname)
                    put("openingDate", acc.openingDate)
                    put("initialBalance", acc.initialBalance)
                    put("currentBalance", acc.currentBalance)
                    put("interestRate", acc.interestRate)
                    put("depositAmount", acc.depositAmount)
                    put("maturityDate", acc.maturityDate ?: JSONObject.NULL)
                    put("totalInstallments", acc.totalInstallments)
                    put("compoundingFrequency", acc.compoundingFrequency)
                    put("dayOfMonthDue", acc.dayOfMonthDue)
                    put("notes", acc.notes)
                    put("isArchived", acc.isArchived)
                }
                accountsArray.put(accObj)
            }
            root.put("accounts", accountsArray)

            // Transactions
            val transactions = database.transactionDao().getAllTransactions()
            val txArray = JSONArray()
            for (tx in transactions) {
                val txObj = JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("date", tx.date)
                    put("category", tx.category)
                    put("paymentMethod", tx.paymentMethod)
                    put("description", tx.description)
                    put("sourceAccountId", tx.sourceAccountId ?: JSONObject.NULL)
                    put("targetAccountId", tx.targetAccountId ?: JSONObject.NULL)
                    put("isEstimatedInterest", tx.isEstimatedInterest)
                    put("installmentMonthIndex", tx.installmentMonthIndex ?: JSONObject.NULL)
                }
                txArray.put(txObj)
            }
            root.put("transactions", txArray)

            // Budgets
            val budgets = database.budgetDao().getAllBudgets()
            val budgetsArray = JSONArray()
            for (b in budgets) {
                val bObj = JSONObject().apply {
                    put("id", b.id)
                    put("month", b.month)
                    put("year", b.year)
                    put("category", b.category)
                    put("amountLimit", b.amountLimit)
                    put("carryForwardUnspent", b.carryForwardUnspent)
                }
                budgetsArray.put(bObj)
            }
            root.put("budgets", budgetsArray)

            // Categories
            val categories = database.customCategoryDao().getAllCategories()
            val catArray = JSONArray()
            for (c in categories) {
                val cObj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("type", c.type)
                    put("iconName", c.iconName)
                    put("colorHex", c.colorHex)
                }
                catArray.put(cObj)
            }
            root.put("categories", catArray)

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val backupFile = File(context.cacheDir, "BudgetTracker_Backup_$dateStr.json")
            FileOutputStream(backupFile).use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
            backupFile
        }

    suspend fun restoreBackupFromJson(jsonString: String, database: AppDatabase): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)

                // 1. Restore Accounts
                if (root.has("accounts")) {
                    val accountsArray = root.getJSONArray("accounts")
                    for (i in 0 until accountsArray.length()) {
                        val obj = accountsArray.getJSONObject(i)
                        val acc = AccountEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            accountType = obj.getString("accountType"),
                            institution = obj.getString("institution"),
                            accountNumberOrNickname = obj.optString("accountNumberOrNickname", ""),
                            openingDate = obj.optLong("openingDate", System.currentTimeMillis()),
                            initialBalance = obj.optDouble("initialBalance", 0.0),
                            currentBalance = obj.optDouble("currentBalance", 0.0),
                            interestRate = obj.optDouble("interestRate", 0.0),
                            depositAmount = obj.optDouble("depositAmount", 0.0),
                            maturityDate = if (obj.isNull("maturityDate")) null else obj.optLong("maturityDate"),
                            totalInstallments = obj.optInt("totalInstallments", 0),
                            compoundingFrequency = obj.optString("compoundingFrequency", "ANNUALLY"),
                            dayOfMonthDue = obj.optInt("dayOfMonthDue", 1),
                            notes = obj.optString("notes", ""),
                            isArchived = obj.optBoolean("isArchived", false)
                        )
                        database.accountDao().insertAccount(acc)
                    }
                }

                // 2. Restore Transactions
                if (root.has("transactions")) {
                    val txArray = root.getJSONArray("transactions")
                    val txList = mutableListOf<TransactionEntity>()
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        val tx = TransactionEntity(
                            id = obj.optLong("id", 0),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            category = obj.getString("category"),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            description = obj.optString("description", ""),
                            sourceAccountId = if (obj.isNull("sourceAccountId")) null else obj.optLong("sourceAccountId"),
                            targetAccountId = if (obj.isNull("targetAccountId")) null else obj.optLong("targetAccountId"),
                            isEstimatedInterest = obj.optBoolean("isEstimatedInterest", false),
                            installmentMonthIndex = if (obj.isNull("installmentMonthIndex")) null else obj.optInt("installmentMonthIndex")
                        )
                        txList.add(tx)
                    }
                    database.transactionDao().insertAll(txList)
                }

                // 3. Restore Budgets
                if (root.has("budgets")) {
                    val bArray = root.getJSONArray("budgets")
                    for (i in 0 until bArray.length()) {
                        val obj = bArray.getJSONObject(i)
                        val b = BudgetEntity(
                            id = obj.optLong("id", 0),
                            month = obj.getInt("month"),
                            year = obj.getInt("year"),
                            category = obj.getString("category"),
                            amountLimit = obj.getDouble("amountLimit"),
                            carryForwardUnspent = obj.optBoolean("carryForwardUnspent", false)
                        )
                        database.budgetDao().insertOrUpdate(b)
                    }
                }

                // 4. Restore Categories
                if (root.has("categories")) {
                    val cArray = root.getJSONArray("categories")
                    for (i in 0 until cArray.length()) {
                        val obj = cArray.getJSONObject(i)
                        val c = CustomCategoryEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            type = obj.getString("type"),
                            iconName = obj.optString("iconName", "Category"),
                            colorHex = obj.optString("colorHex", "#3B82F6")
                        )
                        database.customCategoryDao().insertCategory(c)
                    }
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}
