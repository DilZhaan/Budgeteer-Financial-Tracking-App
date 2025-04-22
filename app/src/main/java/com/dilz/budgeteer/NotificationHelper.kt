package com.dilz.budgeteer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Helper class to manage local notifications for budget alerts
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "budgeteer_channel"
        const val BUDGET_WARNING_ID = 1001
        const val BUDGET_EXCEEDED_ID = 1002
    }

    /**
     * Create notification channels for Android 8.0 and above
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Alerts"
            val descriptionText = "Notifications related to your budget status"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show a notification when the user is approaching their budget limit
     * @param percentUsed percentage of budget used
     * @param remainingAmount amount remaining in the budget
     */
    fun showBudgetWarningNotification(percentUsed: Float, remainingAmount: Float) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val formatter = java.text.NumberFormat.getCurrencyInstance()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget)
            .setContentTitle("Budget Warning")
            .setContentText("You've used ${percentUsed.toInt()}% of your monthly budget!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've used ${percentUsed.toInt()}% of your monthly budget. " +
                        "You have ${formatter.format(remainingAmount)} remaining this month."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setLights(Color.RED, 500, 500)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(BUDGET_WARNING_ID, builder.build())
            } catch (e: SecurityException) {
                // Handle missing notification permission
            }
        }
    }

    /**
     * Show a notification when the user has exceeded their budget
     * @param percentUsed percentage of budget used
     * @param overBudgetAmount amount over the budget
     */
    fun showBudgetExceededNotification(percentUsed: Float, overBudgetAmount: Float) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val formatter = java.text.NumberFormat.getCurrencyInstance()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget)
            .setContentTitle("Budget Exceeded!")
            .setContentText("You've exceeded your monthly budget by ${formatter.format(overBudgetAmount)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've exceeded your monthly budget by ${formatter.format(overBudgetAmount)}. " +
                        "Consider reviewing your expenses for this month."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(Color.RED, 3000, 3000)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(BUDGET_EXCEEDED_ID, builder.build())
            } catch (e: SecurityException) {
                // Handle missing notification permission
            }
        }
    }
} 