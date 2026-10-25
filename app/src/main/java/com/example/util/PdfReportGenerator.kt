package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportData(
    val title: String,
    val period: String,
    val transactions: List<TransactionEntity>,
    val accounts: Map<Long, AccountEntity> = emptyMap(),
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val currencySymbol: String
)

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 22f

    fun generateReportPdf(context: Context, data: ReportData): File {
        val pdfDocument = PdfDocument()

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 41, 66) // Brand Navy
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val subHeaderPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }

        val rowEvenBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
        }

        val incomePaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val expensePaint = Paint().apply {
            color = Color.rgb(239, 68, 68)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        val generationDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        val itemsPerPage = 22
        val totalTransactions = data.transactions.size
        val totalPages = ((totalTransactions + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)

        var txIndex = 0
        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = MARGIN

            // Header Banner
            canvas.drawText("My Budget & Savings Tracker", MARGIN, y + 14f, headerPaint)
            y += 24f
            canvas.drawText("${data.title}  |  Period: ${data.period}", MARGIN, y + 10f, subHeaderPaint)
            canvas.drawText("Generated: $generationDate", PAGE_WIDTH - MARGIN - 140f, y + 10f, subHeaderPaint)
            y += 18f

            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f

            // On Page 1: Draw Financial Summary Box
            if (pageNumber == 1) {
                val boxHeight = 44f
                val boxWidth = (PAGE_WIDTH - (MARGIN * 2)) / 3f

                // Income box
                canvas.drawRect(MARGIN, y, MARGIN + boxWidth - 5f, y + boxHeight, tableHeaderBgPaint)
                canvas.drawText("TOTAL INCOME", MARGIN + 10f, y + 16f, subHeaderPaint)
                canvas.drawText(
                    "${data.currencySymbol} ${CurrencyUtils.formatWithoutSymbol(data.totalIncome)}",
                    MARGIN + 10f,
                    y + 34f,
                    incomePaint
                )

                // Expense box
                canvas.drawRect(MARGIN + boxWidth, y, MARGIN + (boxWidth * 2) - 5f, y + boxHeight, tableHeaderBgPaint)
                canvas.drawText("TOTAL EXPENSES", MARGIN + boxWidth + 10f, y + 16f, subHeaderPaint)
                canvas.drawText(
                    "${data.currencySymbol} ${CurrencyUtils.formatWithoutSymbol(data.totalExpense)}",
                    MARGIN + boxWidth + 10f,
                    y + 34f,
                    expensePaint
                )

                // Net Savings box
                canvas.drawRect(MARGIN + (boxWidth * 2), y, PAGE_WIDTH - MARGIN, y + boxHeight, tableHeaderBgPaint)
                canvas.drawText("NET SAVINGS", MARGIN + (boxWidth * 2) + 10f, y + 16f, subHeaderPaint)
                val savingsPaint = if (data.netSavings >= 0) incomePaint else expensePaint
                canvas.drawText(
                    "${data.currencySymbol} ${CurrencyUtils.formatWithoutSymbol(data.netSavings)}",
                    MARGIN + (boxWidth * 2) + 10f,
                    y + 34f,
                    savingsPaint
                )

                y += boxHeight + 16f
            }

            // Table Header
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + LINE_HEIGHT, tableHeaderBgPaint)
            val headerY = y + 15f
            canvas.drawText("Date", MARGIN + 6f, headerY, boldPaint)
            canvas.drawText("Type", MARGIN + 70f, headerY, boldPaint)
            canvas.drawText("Category", MARGIN + 140f, headerY, boldPaint)
            canvas.drawText("Description", MARGIN + 240f, headerY, boldPaint)
            canvas.drawText("Amount", PAGE_WIDTH - MARGIN - 80f, headerY, boldPaint)

            y += LINE_HEIGHT

            // Table Rows
            val rowsInThisPage = if (pageNumber == 1) itemsPerPage - 3 else itemsPerPage
            var count = 0

            while (txIndex < totalTransactions && count < rowsInThisPage) {
                val tx = data.transactions[txIndex]

                if (count % 2 == 1) {
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + LINE_HEIGHT, rowEvenBgPaint)
                }

                val rowY = y + 15f
                val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(tx.date))
                canvas.drawText(dateStr, MARGIN + 6f, rowY, textPaint)
                canvas.drawText(tx.type.take(9), MARGIN + 70f, rowY, textPaint)
                canvas.drawText(tx.category.take(16), MARGIN + 140f, rowY, textPaint)
                canvas.drawText(tx.description.take(24), MARGIN + 240f, rowY, textPaint)

                val amountStr = "${if (tx.type == TransactionType.EXPENSE.name) "-" else "+"}${data.currencySymbol}${CurrencyUtils.formatWithoutSymbol(tx.amount)}"
                val amtPaint = if (tx.type == TransactionType.EXPENSE.name) expensePaint else incomePaint
                canvas.drawText(amountStr, PAGE_WIDTH - MARGIN - 80f, rowY, amtPaint)

                canvas.drawLine(MARGIN, y + LINE_HEIGHT, PAGE_WIDTH - MARGIN, y + LINE_HEIGHT, linePaint)
                y += LINE_HEIGHT
                txIndex++
                count++
            }

            // Footer
            val footerY = PAGE_HEIGHT - 25f
            canvas.drawLine(MARGIN, footerY - 10f, PAGE_WIDTH - MARGIN, footerY - 10f, linePaint)
            canvas.drawText("Confidential Financial Report", MARGIN, footerY, subHeaderPaint)
            canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN - 70f, footerY, subHeaderPaint)

            pdfDocument.finishPage(page)
        }

        val outputFile = File(context.cacheDir, "Financial_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }
}
