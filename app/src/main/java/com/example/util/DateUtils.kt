package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    val datePatterns = listOf(
        "dd MMM yyyy",
        "yyyy-MM-dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy"
    )

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            val fallback = SimpleDateFormat("dd MMM yyyy", Locale.US)
            fallback.format(Date(timestamp))
        }
    }

    fun getMonthName(monthNumber: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, monthNumber - 1)
        val sdf = SimpleDateFormat("MMMM", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getShortMonthName(monthNumber: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, monthNumber - 1)
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getStartOfMonth(month: Int, year: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfMonth(month: Int, year: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartOfYear(year: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfYear(year: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun daysBetween(from: Long, to: Long): Long {
        val diff = to - from
        return (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    }

    fun currentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    fun currentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }
}
