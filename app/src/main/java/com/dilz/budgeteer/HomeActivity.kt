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
import androidx.constraintlayout.widget.ConstraintLayout
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

            // Find the history card (transaction container) more directly
            // This method looks for the last LinearLayout in the content area which should be our card
            val historyCard = findHistoryCard()

            if (historyCard == null) {
                Log.e(TAG, "Could not find history card")
                return
            }

            // Clear the existing content (remove all the hardcoded examples)
            historyCard.removeAllViews()

            if (historyArray.length() == 0) {
                // No transactions to display
                val noTransactionsText = TextView(this)
                noTransactionsText.text = "No transactions to display"
                noTransactionsText.textSize = 16f
                noTransactionsText.setTextColor(getColor(R.color.black))
                noTransactionsText.gravity = android.view.Gravity.CENTER
                historyCard.addView(noTransactionsText)
                return
            }

            // Convert transaction dates to day of week and group by day
            val transactionsByDay = mutableMapOf<String, MutableList<org.json.JSONObject>>()

            for (i in 0 until historyArray.length()) {
                val transaction = historyArray.getJSONObject(i)
                val dateStr = transaction.getString("date")
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)

                if (date != null) {
                    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)

                    if (!transactionsByDay.containsKey(dayOfWeek)) {
                        transactionsByDay[dayOfWeek] = mutableListOf()
                    }

                    transactionsByDay[dayOfWeek]?.add(transaction)
                }
            }

            // Sort days by how recent they are
            val sortedDays = transactionsByDay.keys.sortedByDescending { dayName ->
                val calendarDayValue = when (dayName.lowercase(Locale.getDefault())) {
                    "sunday" -> Calendar.SUNDAY
                    "monday" -> Calendar.MONDAY
                    "tuesday" -> Calendar.TUESDAY
                    "wednesday" -> Calendar.WEDNESDAY
                    "thursday" -> Calendar.THURSDAY
                    "friday" -> Calendar.FRIDAY
                    "saturday" -> Calendar.SATURDAY
                    else -> -1
                }

                // Return the calendar day value for sorting
                calendarDayValue
            }

            // Display transactions for up to 2 days
            var daysDisplayed = 0

            for (day in sortedDays) {
                if (daysDisplayed >= 2) break

                val transactions = transactionsByDay[day] ?: continue

                // Add day header
                val dayHeader = TextView(this)
                dayHeader.text = translateDayOfWeek(day)
                dayHeader.setTextColor(getColor(R.color.black))
                dayHeader.textSize = 16f
                historyCard.addView(dayHeader)

                // Add transactions for this day (up to 2)
                var transactionsDisplayed = 0
                for (transaction in transactions) {
                    if (transactionsDisplayed >= 2) break

                    // Create a row for this transaction
                    val row = createTransactionRow(transaction)
                    historyCard.addView(row)

                    transactionsDisplayed++
                }

                daysDisplayed++
            }

            Log.d(TAG, "Successfully displayed recent transactions on home page")

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying recent transactions: ${e.message}", e)
        }
    }

    // A more reliable way to find the history card
    private fun findHistoryCard(): LinearLayout? {
        // Find the history header first (we know its ID)
        val historyHeader = findViewById<TextView>(R.id.historico_h2)
        if (historyHeader == null) {
            Log.e(TAG, "Could not find history header")
            return null
        }

        // Get the parent view which should be a LinearLayout
        val parentView = historyHeader.parent as? LinearLayout
        if (parentView == null) {
            Log.e(TAG, "History header parent is not a LinearLayout")
            return null
        }

        // The history card should be the next view after the header
        val historyCardIndex = parentView.indexOfChild(historyHeader) + 1
        if (historyCardIndex >= parentView.childCount) {
            Log.e(TAG, "No view after history header")
            return null
        }

        val historyCard = parentView.getChildAt(historyCardIndex) as? LinearLayout
        if (historyCard == null) {
            Log.e(TAG, "View after history header is not a LinearLayout")
            return null
        }

        return historyCard
    }

    private fun translateDayOfWeek(englishDay: String): String {
        return when (englishDay.lowercase(Locale.getDefault())) {
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

    private fun createTransactionRow(transaction: org.json.JSONObject): LinearLayout {
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
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())

        // Left container (icon + name)
        val leftContainer = LinearLayout(this)
        leftContainer.orientation = LinearLayout.HORIZONTAL
        leftContainer.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f // weight
        )
        leftContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        // Color indicator (using a hardcoded size since we don't have the dimension resource)
        val colorIndicator = ImageView(this)
        val iconSize = (15 * resources.displayMetrics.density).toInt() // Convert 15dp to pixels
        colorIndicator.layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)

        colorIndicator.setImageResource(
            if (isExpense) R.drawable.color_red else R.drawable.color_green
        )
        colorIndicator.contentDescription =
            if (isExpense) getString(R.string.desc_img_expense) else getString(R.string.desc_img_revenue)

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

    fun onHistoryClick(view: View) {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    fun onRegisterClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}