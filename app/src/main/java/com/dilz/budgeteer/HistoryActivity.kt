package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.View
import android.widget.LinearLayout

class HistoryActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentBalanceValue: TextView
    private lateinit var monthlyBalanceSheetValue: TextView
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        // Initialize UI components
        findViewById<ImageButton>(R.id.btn_return).setOnClickListener {
            finish()
        }

        currentBalanceValue = findViewById(R.id.current_balance_value)
        monthlyBalanceSheetValue = findViewById(R.id.monthly_balance_sheet_value)

        // The container that holds the history items
        historyContainer = findViewById(R.id.categorias_formulario)

        // Load and display financial data
        loadFinancialData()

        // Load and display transaction history
        loadTransactionHistory()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        loadFinancialData()
        loadTransactionHistory()
    }

    private fun loadFinancialData() {
        // Get financial data from SharedPreferences
        val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
        val totalExpense = sharedPreferences.getFloat("totalExpense", 0f)
        val currentBalance = totalRevenue - totalExpense

        // Calculate monthly balance
        val monthlyRevenue = getMonthlyAmount(true)
        val monthlyExpense = getMonthlyAmount(false)
        val monthlyBalance = monthlyRevenue - monthlyExpense

        // Update UI with formatted values
        currentBalanceValue.text = String.format("R$ %.2f", currentBalance)
        monthlyBalanceSheetValue.text = String.format("R$ %.2f", monthlyBalance)
    }

    private fun getMonthlyAmount(isRevenue: Boolean): Float {
        val historyJson = sharedPreferences.getString("transactionHistory", "[]")
        val historyArray = JSONArray(historyJson)

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var monthlyAmount = 0f

        // Calculate total for current month
        for (i in 0 until historyArray.length()) {
            val transaction = historyArray.getJSONObject(i)

            // Skip if transaction type doesn't match (revenue/expense)
            if (transaction.getBoolean("isExpense") == isRevenue) {
                continue
            }

            // Check if transaction is from current month
            val dateStr = transaction.getString("date")
            val transactionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)

            val transactionCalendar = Calendar.getInstance()
            if (transactionDate != null) {
                transactionCalendar.time = transactionDate
            }

            val transactionMonth = transactionCalendar.get(Calendar.MONTH)
            val transactionYear = transactionCalendar.get(Calendar.YEAR)

            if (transactionMonth == currentMonth && transactionYear == currentYear) {
                monthlyAmount += transaction.getDouble("value").toFloat()
            }
        }

        return monthlyAmount
    }

    private fun loadTransactionHistory() {
        try {
            // Get transaction history from SharedPreferences
            val historyJson = sharedPreferences.getString("transactionHistory", "[]")
            val historyArray = JSONArray(historyJson)

            // Find the ScrollView's content LinearLayout
            // This is a safer approach that works even if the view hierarchy is different
            var scrollViewContent: LinearLayout? = null

            if (historyContainer.childCount > 0) {
                val scrollView = historyContainer.getChildAt(0)
                if (scrollView is LinearLayout && scrollView.childCount > 0) {
                    scrollViewContent = scrollView
                }
            }

            if (scrollViewContent == null) {
                // Couldn't find the correct container
                return
            }

            // Group transactions by day
            val transactionsByDay = mutableMapOf<String, MutableList<JSONObject>>()

            // Format for day grouping (e.g., "Thursday", "Wednesday")
            val dateFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

            for (i in 0 until historyArray.length()) {
                val transaction = historyArray.getJSONObject(i)
                val dateStr = transaction.getString("date")
                val transactionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)

                if (transactionDate != null) {
                    val dayOfWeek = dateFormatter.format(transactionDate)

                    if (!transactionsByDay.containsKey(dayOfWeek)) {
                        transactionsByDay[dayOfWeek] = mutableListOf()
                    }

                    transactionsByDay[dayOfWeek]?.add(transaction)
                }
            }

            // If there are transactions to display, clear any placeholder data
            if (transactionsByDay.isNotEmpty()) {
                scrollViewContent.removeAllViews()
            }

            // Sort days by recency (most recent first)
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

            val sortedDays = transactionsByDay.keys.sortedBy { dayName ->
                // Calculate days ago
                getDaysFromToday(dayName)
            }

            // Add transactions for each day
            for (day in sortedDays) {
                val dayTransactions = transactionsByDay[day] ?: continue

                // Create day header TextView
                val dayHeader = TextView(this)
                dayHeader.text = getDayString(day)
                dayHeader.textSize = 18f
                dayHeader.setTextColor(getColor(R.color.black))

                // Add padding to the header
                val padding = resources.getDimensionPixelSize(R.dimen.day_header_padding)
                dayHeader.setPadding(padding, padding, padding, padding)

                scrollViewContent.addView(dayHeader)

                // Add transactions for this day
                for (transaction in dayTransactions) {
                    // Create and add transaction item view
                    val transactionView = createTransactionView(transaction)
                    scrollViewContent.addView(transactionView)
                }
            }
        } catch (e: Exception) {
            // Handle exceptions to prevent crashes
            e.printStackTrace()
        }
    }

    private fun getDaysFromToday(dayName: String): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)

        // Convert day name to day of week value
        val dayValue = when (dayName.toLowerCase(Locale.getDefault())) {
            "sunday" -> Calendar.SUNDAY
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> today // Default to today if unknown
        }

        // Calculate days ago (0 for today, 1 for yesterday, etc.)
        val daysAgo = if (dayValue <= today) {
            today - dayValue
        } else {
            7 - (dayValue - today)
        }

        return daysAgo
    }

    private fun getDayString(englishDay: String): String {
        // Convert English day names to your app's language (assuming Portuguese from your XML)
        return when (englishDay.toLowerCase(Locale.getDefault())) {
            "sunday" -> getString(R.string.sunday)
            "monday" -> getString(R.string.monday)
            "tuesday" -> getString(R.string.tuesday)
            "wednesday" -> getString(R.string.wednesday)
            "thursday" -> getString(R.string.thursday)
            "friday" -> getString(R.string.friday)
            "saturday" -> getString(R.string.saturday)
            else -> englishDay // Default fallback
        }
    }

    private fun createTransactionView(transaction: JSONObject): View {
        // Inflate a horizontal LinearLayout for the transaction item
        val transactionLayout = LinearLayout(this)
        transactionLayout.orientation = LinearLayout.HORIZONTAL
        transactionLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Add margins
        val layoutParams = transactionLayout.layoutParams as LinearLayout.LayoutParams
        layoutParams.setMargins(0, 20, 0, 0)
        transactionLayout.layoutParams = layoutParams

        // Get transaction data
        val name = transaction.getString("name")
        val value = transaction.getDouble("value").toFloat()
        val isExpense = transaction.getBoolean("isExpense")
        val dateStr = transaction.getString("date")

        // Parse date
        val transactionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFormat.format(transactionDate ?: Date())

        // 1. Icon (revenue or expense)
        val iconResId = if (isExpense) R.drawable.icon_expense else R.drawable.icon_revenue
        val iconView = android.widget.ImageView(this)
        iconView.setImageResource(iconResId)
        iconView.layoutParams = LinearLayout.LayoutParams(30, 30)
        transactionLayout.addView(iconView)

        // Add some spacing
        val space = View(this)
        space.layoutParams = LinearLayout.LayoutParams(10, 1)
        transactionLayout.addView(space)

        // 2. Name and time (vertical layout)
        val detailsLayout = LinearLayout(this)
        detailsLayout.orientation = LinearLayout.VERTICAL
        detailsLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )

        // Name TextView
        val nameTextView = TextView(this)
        nameTextView.text = name
        nameTextView.setTextColor(getColor(R.color.black))
        nameTextView.textSize = 15f
        detailsLayout.addView(nameTextView)

        // Time TextView
        val timeTextView = TextView(this)
        timeTextView.text = timeStr
        timeTextView.setTextColor(getColor(R.color.black))
        timeTextView.textSize = 10f
        detailsLayout.addView(timeTextView)

        transactionLayout.addView(detailsLayout)

        // 3. Value TextView
        val valueTextView = TextView(this)
        val formattedValue = String.format("R$ %.2f", value)
        valueTextView.text = formattedValue
        valueTextView.textSize = 15f

        // Set color based on transaction type
        val textColor = if (isExpense) {
            getColor(R.color.wine) // Expense - red
        } else {
            getColor(R.color.green) // Revenue - green
        }
        valueTextView.setTextColor(textColor)

        // Right-align the value
        valueTextView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        (valueTextView.layoutParams as LinearLayout.LayoutParams).gravity = android.view.Gravity.END

        transactionLayout.addView(valueTextView)

        return transactionLayout
    }
}