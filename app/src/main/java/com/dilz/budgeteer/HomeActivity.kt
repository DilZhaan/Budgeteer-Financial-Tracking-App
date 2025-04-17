package com.dilz.budgeteer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.ImageView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var balanceTextView: TextView
    private lateinit var revenueTextView: TextView
    private lateinit var expenseTextView: TextView
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        // Find necessary TextViews
        balanceTextView = findViewById(R.id.textView4) // Balance amount
        revenueTextView = findViewById(R.id.revenue_amount) // Using the IDs you added
        expenseTextView = findViewById(R.id.expense_amount) // Using the IDs you added

        // Update financial summary
        updateFinancialSummary()

        // Display recent transactions
        displayRecentTransactions()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to the activity
        updateFinancialSummary()
        displayRecentTransactions()
    }

    private fun updateFinancialSummary() {
        // Get financial data from SharedPreferences
        val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
        val totalExpense = sharedPreferences.getFloat("totalExpense", 0f)
        val balance = totalRevenue - totalExpense

        Log.d(TAG, "Financial summary: Revenue=$totalRevenue, Expense=$totalExpense, Balance=$balance")

        // Always update the balance TextView
        balanceTextView.text = String.format("R$ %.2f", balance)

        // Update revenue TextView if initialized
        if (::revenueTextView.isInitialized) {
            revenueTextView.text = String.format("R$ %.2f", totalRevenue)
            Log.d(TAG, "Updated revenue TextView to: " + String.format("R$ %.2f", totalRevenue))
        } else {
            Log.e(TAG, "Revenue TextView not initialized!")
        }

        // Update expense TextView if initialized
        if (::expenseTextView.isInitialized) {
            expenseTextView.text = String.format("R$ %.2f", totalExpense)
            Log.d(TAG, "Updated expense TextView to: " + String.format("R$ %.2f", totalExpense))
        } else {
            Log.e(TAG, "Expense TextView not initialized!")
        }
    }

    private fun displayRecentTransactions() {
        try {
            // Get transaction history
            val historyJson = sharedPreferences.getString("transactionHistory", "[]")
            val historyArray = JSONArray(historyJson)

            Log.d(TAG, "Displaying recent transactions. Total transactions: ${historyArray.length()}")

            if (historyArray.length() == 0) {
                // No transactions to display
                return
            }

            // Find the container for recent transactions
            val transactionContainer = findViewById<LinearLayout>(R.id.historico_h2).parent as? LinearLayout
            if (transactionContainer == null) {
                Log.e(TAG, "Transaction container not found")
                return
            }

            // Find the history card (the last child of the container)
            var historyCard: LinearLayout? = null
            for (i in 0 until transactionContainer.childCount) {
                val child = transactionContainer.getChildAt(i)
                if (child is LinearLayout && i == transactionContainer.childCount - 1) {
                    historyCard = child
                    break
                }
            }

            if (historyCard == null) {
                Log.e(TAG, "History card not found")
                return
            }

            // Clear the existing content
            historyCard.removeAllViews()

            // Group transactions by day
            val transactionsByDay = groupTransactionsByDay(historyArray)

            // Sort days by recency
            val sortedDays = transactionsByDay.keys.sortedBy { dayName ->
                getDaysFromToday(dayName)
            }

            // Display up to 2 most recent days
            var daysShown = 0
            for (dayName in sortedDays) {
                if (daysShown >= 2) break // Only show 2 days max

                val transactions = transactionsByDay[dayName] ?: continue

                // Add day header
                val dayHeader = TextView(this)
                dayHeader.text = getDayString(dayName)
                dayHeader.textColor = getColor(R.color.black)
                dayHeader.textSize = 16f
                historyCard.addView(dayHeader)

                // Add up to 2 transactions per day
                var transactionsShown = 0
                for (transaction in transactions) {
                    if (transactionsShown >= 2) break // Only show 2 transactions per day

                    // Create transaction view
                    val transactionView = createTransactionView(transaction)
                    historyCard.addView(transactionView)

                    transactionsShown++
                }

                daysShown++
            }

            Log.d(TAG, "Successfully displayed recent transactions")

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying recent transactions", e)
        }
    }

    private fun groupTransactionsByDay(historyArray: JSONArray): Map<String, MutableList<org.json.JSONObject>> {
        val transactionsByDay = mutableMapOf<String, MutableList<org.json.JSONObject>>()

        for (i in 0 until historyArray.length()) {
            try {
                val transaction = historyArray.getJSONObject(i)
                val dateStr = transaction.getString("date")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val transactionDate = dateFormat.parse(dateStr)

                if (transactionDate != null) {
                    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(transactionDate)

                    if (!transactionsByDay.containsKey(dayOfWeek)) {
                        transactionsByDay[dayOfWeek] = mutableListOf()
                    }

                    transactionsByDay[dayOfWeek]?.add(transaction)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing transaction", e)
            }
        }

        return transactionsByDay
    }

    private fun createTransactionView(transaction: org.json.JSONObject): LinearLayout {
        // Create horizontal layout for the transaction
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.gravity = android.view.Gravity.CENTER_VERTICAL
        layout.setPadding(0, 5, 0, 5)

        // Extract transaction data
        val name = transaction.getString("name")
        val value = transaction.getDouble("value").toFloat()
        val isExpense = transaction.getBoolean("isExpense")
        val dateStr = transaction.getString("date")

        // Format time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.parse(dateStr) ?: Date()
        val timeStr = timeFormat.format(date)

        // Left container (icon + name)
        val leftContainer = LinearLayout(this)
        leftContainer.orientation = LinearLayout.HORIZONTAL
        leftContainer.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f // weight
        )
        leftContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        // Color indicator
        val colorIndicator = ImageView(this)
        colorIndicator.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.icon_size_small),
            resources.getDimensionPixelSize(R.dimen.icon_size_small)
        )
        colorIndicator.setImageResource(
            if (isExpense) R.drawable.color_red else R.drawable.color_green
        )
        leftContainer.addView(colorIndicator)

        // Transaction name
        val nameView = TextView(this)
        nameView.text = name
        nameView.textSize = 14f
        nameView.setTextColor(getColor(R.color.black))
        nameView.setPadding(5, 0, 5, 0)
        leftContainer.addView(nameView)

        layout.addView(leftContainer)

        // Time
        val timeView = TextView(this)
        timeView.text = timeStr
        timeView.textSize = 14f
        timeView.setTextColor(getColor(R.color.black))
        layout.addView(timeView)

        // Value
        val valueView = TextView(this)
        valueView.text = String.format("R$ %.2f", value)
        valueView.textSize = 14f
        valueView.setTextColor(getColor(R.color.black))
        valueView.setPadding(10, 0, 0, 0)
        layout.addView(valueView)

        return layout
    }

    private fun getDaysFromToday(dayName: String): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)

        val dayValue = when (dayName.toLowerCase(Locale.getDefault())) {
            "sunday" -> Calendar.SUNDAY
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> today
        }

        val daysAgo = if (dayValue <= today) {
            today - dayValue
        } else {
            7 - (dayValue - today)
        }

        return daysAgo
    }

    private fun getDayString(englishDay: String): String {
        return when (englishDay.toLowerCase(Locale.getDefault())) {
            "sunday" -> getString(R.string.sunday)
            "monday" -> getString(R.string.monday)
            "tuesday" -> getString(R.string.tuesday)
            "wednesday" -> getString(R.string.wednesday)
            "thursday" -> getString(R.string.thursday)
            "friday" -> getString(R.string.friday)
            "saturday" -> getString(R.string.saturday)
            else -> englishDay
        }
    }

    fun onHistoryClick(view: View) {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    fun onRegisterClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    // Add this function for debugging if needed
    private fun resetAllData() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.putFloat("totalRevenue", 0f)
        editor.putFloat("totalExpense", 0f)
        editor.putString("transactionHistory", "[]")
        editor.apply()

        updateFinancialSummary()
        Log.d(TAG, "All data has been reset")
    }
}