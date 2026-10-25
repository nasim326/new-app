package com.example.util

import android.content.Context
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        accounts: Map<Long, AccountEntity>
    ): File {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvHeader = "ID,Date,Type,Category,Amount,Payment Method,Description,Source Account,Target Account,Is Estimated Interest,Installment Month\n"

        val sb = StringBuilder()
        sb.append(csvHeader)

        for (tx in transactions) {
            val dateStr = sdf.format(Date(tx.date))
            val srcName = tx.sourceAccountId?.let { accounts[it]?.name ?: "Account #$it" } ?: ""
            val tgtName = tx.targetAccountId?.let { accounts[it]?.name ?: "Account #$it" } ?: ""
            val cleanDesc = tx.description.replace(",", ";").replace("\n", " ")
            val cleanCat = tx.category.replace(",", ";")

            sb.append("${tx.id},")
            sb.append("\"$dateStr\",")
            sb.append("\"${tx.type}\",")
            sb.append("\"$cleanCat\",")
            sb.append("${tx.amount},")
            sb.append("\"${tx.paymentMethod}\",")
            sb.append("\"$cleanDesc\",")
            sb.append("\"$srcName\",")
            sb.append("\"$tgtName\",")
            sb.append("${tx.isEstimatedInterest},")
            sb.append("${tx.installmentMonthIndex ?: ""}\n")
        }

        val file = File(context.cacheDir, "Transactions_Export_${System.currentTimeMillis()}.csv")
        FileOutputStream(file).use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
        return file
    }
}
