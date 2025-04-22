package com.dilz.budgeteer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import java.util.Timer
import java.util.TimerTask

/**
 * Service that runs in the background to monitor budget thresholds
 * and trigger notifications when appropriate.
 */
class BudgetMonitorService : Service() {
    
    private val TAG = "BudgetMonitorService"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var budgetManager: BudgetManager
    private lateinit var timer: Timer
    
    // Check interval (every 15 minutes)
    private val CHECK_INTERVAL: Long = 15 * 60 * 1000
    
    // Keys for tracking notification state
    private val KEY_WARNING_NOTIFIED = "budget_warning_notified"
    private val KEY_EXCEEDED_NOTIFIED = "budget_exceeded_notified"
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BudgetMonitorService created")
        
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", Context.MODE_PRIVATE)
        budgetManager = BudgetManager(sharedPreferences, applicationContext)
        
        startMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BudgetMonitorService started")
        
        // If service gets killed, restart it
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        Log.d(TAG, "BudgetMonitorService destroyed")
    }
    
    private fun startMonitoring() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkBudgetStatus()
            }
        }, 0, CHECK_INTERVAL)
    }
    
    private fun checkBudgetStatus() {
        val budget = budgetManager.getMonthlyBudget()
        val expenses = budgetManager.getCurrentMonthExpenses()
        
        if (budget <= 0f) return
        
        val percentage = (expenses / budget) * 100
        Log.d(TAG, "Budget check: Used $percentage% of budget")
        
        // Check if budget is exceeded (100%+)
        if (percentage >= 100) {
            val alreadyNotified = sharedPreferences.getBoolean(KEY_EXCEEDED_NOTIFIED, false)
            if (!alreadyNotified) {
                val overBudgetAmount = expenses - budget
                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showBudgetExceededNotification(percentage, overBudgetAmount)
                
                // Mark as notified
                sharedPreferences.edit().putBoolean(KEY_EXCEEDED_NOTIFIED, true).apply()
                Log.d(TAG, "Budget exceeded notification sent")
            }
        } 
        // Check if warning threshold is reached (80%)
        else if (percentage >= 80) {
            val alreadyNotified = sharedPreferences.getBoolean(KEY_WARNING_NOTIFIED, false)
            if (!alreadyNotified) {
                val remainingAmount = budget - expenses
                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showBudgetWarningNotification(percentage, remainingAmount)
                
                // Mark as notified
                sharedPreferences.edit().putBoolean(KEY_WARNING_NOTIFIED, true).apply()
                Log.d(TAG, "Budget warning notification sent")
            }
        }
    }
    
    companion object {
        /**
         * Start the budget monitoring service
         */
        fun startService(context: Context) {
            val intent = Intent(context, BudgetMonitorService::class.java)
            context.startService(intent)
        }
        
        /**
         * Reset notification states for a new month or when budget changes
         */
        fun resetNotificationStates(context: Context) {
            val prefs = context.getSharedPreferences("BudgeteerPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("budget_warning_notified", false)
                .putBoolean("budget_exceeded_notified", false)
                .apply()
            Log.d("BudgetMonitor", "Notification states reset")
        }
    }
} 