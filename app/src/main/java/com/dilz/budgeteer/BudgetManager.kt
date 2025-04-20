package com.dilz.budgeteer

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Date

class BudgetManager(private val sharedPreferences: SharedPreferences, private val context: Context) {
    companion object {
        private const val BUDGET_KEY = "monthly_budget"
        private const val LAST_NOTIFICATION_KEY = "last_notification_threshold"
        private val THRESHOLDS = listOf(75, 90, 100)
    }

    private val notificationHelper = NotificationHelper(context)

    fun setMonthlyBudget(amount: Float) {
        sharedPreferences.edit().putFloat(BUDGET_KEY, amount).apply()
        // Reset notification thresholds when budget is updated
        sharedPreferences.edit().putInt(LAST_NOTIFICATION_KEY, 0).apply()
    }

    fun getMonthlyBudget(): Float {
        return sharedPreferences.getFloat(BUDGET_KEY, 0f)
    }

    fun getCurrentMonthExpenses(): Float {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val historyJson = sharedPreferences.getString("transactionHistory", "[]")
        val historyArray = org.json.JSONArray(historyJson)

        var totalExpenses = 0f
        for (i in 0 until historyArray.length()) {
            val transaction = historyArray.getJSONObject(i)
            val dateStr = transaction.getString("date")
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .parse(dateStr) ?: continue

            val transactionCalendar = Calendar.getInstance()
            transactionCalendar.time = date

            if (transaction.getBoolean("isExpense") &&
                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                transactionCalendar.get(Calendar.YEAR) == currentYear) {
                totalExpenses += transaction.getDouble("value").toFloat()
            }
        }
        return totalExpenses
    }

    fun getCategoryExpenses(category: String): Float {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val historyJson = sharedPreferences.getString("transactionHistory", "[]")
        val historyArray = org.json.JSONArray(historyJson)

        var categoryExpenses = 0f
        for (i in 0 until historyArray.length()) {
            val transaction = historyArray.getJSONObject(i)
            val dateStr = transaction.getString("date")
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .parse(dateStr) ?: continue

            val transactionCalendar = Calendar.getInstance()
            transactionCalendar.time = date

            if (transaction.getBoolean("isExpense") &&
                transaction.getString("category") == category &&
                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                transactionCalendar.get(Calendar.YEAR) == currentYear) {
                categoryExpenses += transaction.getDouble("value").toFloat()
            }
        }
        return categoryExpenses
    }

    fun getCategoryExpensesPercentage(category: String): Float {
        val totalExpenses = getCurrentMonthExpenses()
        if (totalExpenses == 0f) return 0f
        return (getCategoryExpenses(category) / totalExpenses) * 100
    }

    fun shouldShowBudgetWarning(): Boolean {
        val budget = getMonthlyBudget()
        if (budget == 0f) return false
        val expenses = getCurrentMonthExpenses()
        return expenses >= (budget * 0.8f)
    }

    fun getBudgetProgress(): Float {
        val budget = getMonthlyBudget()
        if (budget == 0f) return 0f
        val expenses = getCurrentMonthExpenses()
        val progress = (expenses / budget) * 100

        // Check thresholds and show notifications
        checkBudgetThresholds(expenses.toDouble(), budget.toDouble())

        return progress
    }

    private fun checkBudgetThresholds(currentSpending: Double, budget: Double) {
        val percentage = (currentSpending / budget) * 100
        val lastNotifiedThreshold = sharedPreferences.getInt(LAST_NOTIFICATION_KEY, 0)
        
        for (threshold in THRESHOLDS) {
            if (percentage >= threshold && threshold > lastNotifiedThreshold) {
                notificationHelper.showBudgetThresholdNotification(threshold, currentSpending, budget)
                sharedPreferences.edit().putInt(LAST_NOTIFICATION_KEY, threshold).apply()
                break // Only notify for the highest threshold reached
            }
        }
    }
} 