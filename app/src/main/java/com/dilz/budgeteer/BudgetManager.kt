package com.dilz.budgeteer

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Date

class BudgetManager(private val sharedPreferences: SharedPreferences, private val context: Context) {
    companion object {
        private const val BUDGET_KEY = "monthly_budget"
        private const val LAST_NOTIFICATION_KEY = "last_notification_threshold"
        private const val WARNING_THRESHOLD = 80 // Show warning at 80% of budget
        private const val CRITICAL_THRESHOLD = 100 // Show critical notification at 100%
    }

    private val notificationHelper = NotificationHelper(context)

    init {
        // Initialize the notification channel
        notificationHelper.createNotificationChannel()
    }

    fun setMonthlyBudget(amount: Float) {
        sharedPreferences.edit().putFloat(BUDGET_KEY, amount).apply()
        // Reset notification thresholds when budget is updated
        sharedPreferences.edit().putInt(LAST_NOTIFICATION_KEY, 0).apply()
        
        // Reset notification states in service and start monitoring
        BudgetMonitorService.resetNotificationStates(context)
        BudgetMonitorService.startService(context)
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
        return expenses >= (budget * (WARNING_THRESHOLD / 100f))
    }

    fun getBudgetProgress(): Float {
        val budget = getMonthlyBudget()
        if (budget == 0f) return 0f
        val expenses = getCurrentMonthExpenses()
        val progress = (expenses / budget) * 100
        
        return progress
    }
    
    /**
     * Check budget thresholds and show notifications if necessary
     * This should be called explicitly when we want to check and possibly show notifications
     */
    fun checkBudgetThresholdsWithNotifications() {
        val budget = getMonthlyBudget()
        if (budget <= 0f) return
        
        val expenses = getCurrentMonthExpenses()
        checkBudgetThresholds(expenses, budget)
    }
    
    private fun checkBudgetThresholds(currentSpending: Float, budget: Float) {
        if (budget <= 0) return
        
        val percentage = (currentSpending / budget) * 100
        val lastNotifiedThreshold = sharedPreferences.getInt(LAST_NOTIFICATION_KEY, 0)
        
        when {
            // Budget exceeded notification
            percentage >= CRITICAL_THRESHOLD && lastNotifiedThreshold < CRITICAL_THRESHOLD -> {
                val overBudgetAmount = currentSpending - budget
                notificationHelper.showBudgetExceededNotification(percentage, overBudgetAmount)
                sharedPreferences.edit().putInt(LAST_NOTIFICATION_KEY, CRITICAL_THRESHOLD).apply()
            }
            
            // Budget warning notification
            percentage >= WARNING_THRESHOLD && lastNotifiedThreshold < WARNING_THRESHOLD -> {
                val remainingAmount = budget - currentSpending
                notificationHelper.showBudgetWarningNotification(percentage, remainingAmount)
                sharedPreferences.edit().putInt(LAST_NOTIFICATION_KEY, WARNING_THRESHOLD).apply()
            }
        }
    }
    
    /**
     * Check budget status after new transaction is added and start budget monitoring service
     * @return true if a threshold has been reached, false otherwise
     */
    fun checkBudgetStatusAfterTransaction(): Boolean {
        val budget = getMonthlyBudget()
        val expenses = getCurrentMonthExpenses()
        
        if (budget > 0) {
            val percentage = (expenses / budget) * 100
            
            // Start or refresh the budget monitoring service
            BudgetMonitorService.resetNotificationStates(context)
            BudgetMonitorService.startService(context)
            
            return percentage >= WARNING_THRESHOLD
        }
        return false
    }
    
    /**
     * Reset notification thresholds for the new month
     * Call this at the beginning of each month
     */
    fun resetMonthlyNotificationThresholds() {
        sharedPreferences.edit().putInt(LAST_NOTIFICATION_KEY, 0).apply()
        BudgetMonitorService.resetNotificationStates(context)
    }
} 