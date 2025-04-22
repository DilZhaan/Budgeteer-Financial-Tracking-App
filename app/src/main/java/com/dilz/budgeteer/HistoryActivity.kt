package com.dilz.budgeteer

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentBalanceValue: TextView
    private lateinit var monthlyBalanceSheetValue: TextView
    private lateinit var categoryContainer: LinearLayout
    private val TAG = "HistoryActivity"
    private var currentFilterCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        // Initialize UI components
        findViewById<ImageButton>(R.id.btn_return).setOnClickListener {
            finish()
        }

        // Find balance TextViews
        currentBalanceValue = findViewById(R.id.current_balance_value)
        monthlyBalanceSheetValue = findViewById(R.id.monthly_balance_sheet_value)
        categoryContainer = findViewById(R.id.categorias)

        // Create category filter chips
        createCategoryChips()

        // Update financial summary
        updateFinancialSummary()

        // Display transactions
        displayAllTransactions()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        updateFinancialSummary()
        displayAllTransactions()
        createCategoryChips() // Refresh category chips
    }

    private fun updateFinancialSummary() {
        // Get financial data from SharedPreferences
        val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
        val totalExpense = sharedPreferences.getFloat("totalExpense", 0f)
        val balance = totalRevenue - totalExpense

        // Update UI with formatted values
        currentBalanceValue.text = String.format("R$ %.2f", balance)
        monthlyBalanceSheetValue.text = String.format("R$ %.2f", balance)

        Log.d(TAG, "Financial summary updated: Revenue=$totalRevenue, Expense=$totalExpense, Balance=$balance")
    }

    private fun createCategoryChips() {
        try {
            // Clear existing chips
            categoryContainer.removeAllViews()
            
            // Add "All" chip for showing all transactions
            val allChip = Chip(this).apply {
                text = "All"
                setTextColor(getColor(R.color.text_primary)) // Black text
                chipBackgroundColor = ColorStateList.valueOf(getColor(R.color.surface))
                isCheckable = true
                isChecked = currentFilterCategory == null
                setOnClickListener {
                    currentFilterCategory = null
                    displayAllTransactions()
                }
            }
            val allParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 8 }
            allChip.layoutParams = allParams
            categoryContainer.addView(allChip)
            
            // Get user-defined categories
            val categories = loadCategoriesFromSharedPreferences()
            
            // Add a chip for each category
            for (category in categories) {
                val chip = Chip(this).apply {
                    text = category
                    setTextColor(getColor(R.color.text_primary)) // Black text
                    chipBackgroundColor = ColorStateList.valueOf(getColor(R.color.surface))
                    
                    // Create a circular drawable for the chip icon
                    val circleDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(getCategoryColor(category))
                        setSize(24, 24)
                    }
                    
                    chipIcon = circleDrawable
                    isChipIconVisible = true
                    isCheckable = true
                    isChecked = category == currentFilterCategory
                    
                    // Set click listener to filter transactions
                    setOnClickListener {
                        currentFilterCategory = category
                        filterTransactionsByCategory(category)
                    }
                }
                
                // Set layout parameters with margin
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                chip.layoutParams = params
                
                // Add chip to container
                categoryContainer.addView(chip)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating category chips: ${e.message}")
        }
    }

    private fun getCategoryColor(category: String): Int {
        // Map categories to colors
        return when (category) {
            "Food" -> getColor(R.color.food_color)
            "Transportation" -> getColor(R.color.transport_color)
            "Housing" -> getColor(R.color.housing_color)
            "Leisure" -> getColor(R.color.leisure_color)
            "Education" -> getColor(R.color.education_color)
            "Health" -> getColor(R.color.health_color)
            "Clothing" -> getColor(R.color.clothing_color)
            "Bills" -> getColor(R.color.bills_color)
            "Salary" -> getColor(R.color.salary_color)
            "Investments" -> getColor(R.color.investments_color)
            else -> getColor(R.color.primary)
        }
    }

    private fun filterTransactionsByCategory(category: String) {
        try {
            // Get all transactions
            val historyJson = sharedPreferences.getString("transactionHistory", "[]")
            val historyArray = JSONArray(historyJson)
            
            // Get container for transactions
            val container = findViewById<LinearLayout>(R.id.transactions_container)
            container.removeAllViews()
            
            // Find empty state message view
            val emptyStateMessage = findViewById<TextView>(R.id.empty_state_message)
            
            // Filter and display transactions matching the category
            var hasTransactions = false
            for (i in 0 until historyArray.length()) {
                val transaction = historyArray.getJSONObject(i)
                val transactionCategory = transaction.optString("category", "")
                
                if (transactionCategory.equals(category, ignoreCase = true)) {
                    val view = createTransactionView(transaction, i)
                    container.addView(view)
                    hasTransactions = true
                }
            }
            
            // Show empty state message if no transactions match the filter
            if (!hasTransactions) {
                emptyStateMessage.text = "No transactions in category: $category"
                emptyStateMessage.visibility = View.VISIBLE
            } else {
                emptyStateMessage.visibility = View.GONE
            }
            
            // Show feedback toast
            Toast.makeText(this, "Showing transactions in category: $category", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error filtering transactions: ${e.message}")
        }
    }

    private fun displayAllTransactions() {
        try {
            // Get transaction history
            val historyJson = sharedPreferences.getString("transactionHistory", "[]")
            val historyArray = JSONArray(historyJson)

            Log.d(TAG, "Found ${historyArray.length()} transactions")

            // Find the empty state message TextView safely
            val emptyStateMessage = findViewById<TextView>(R.id.empty_state_message)
            
            if (historyArray.length() == 0) {
                // Show empty state
                emptyStateMessage?.visibility = View.VISIBLE
                return
            } else {
                emptyStateMessage?.visibility = View.GONE
            }

            // Get the ScrollView and content container using their IDs
            val scrollView = findViewById<ScrollView>(R.id.transactions_scrollview)
            val contentLayout = findViewById<LinearLayout>(R.id.transactions_container)
            
            if (scrollView == null || contentLayout == null) {
                Log.e(TAG, "Could not find ScrollView or content container")
                Toast.makeText(this, "Error: Layout elements not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Clear existing content
            contentLayout.removeAllViews()

            // Create a simple linear list of all transactions
            for (i in 0 until historyArray.length()) {
                try {
                    val transaction = historyArray.getJSONObject(i)
                    Log.d(TAG, "Creating view for transaction: $transaction")

                    val transactionView = createTransactionView(transaction, i)
                    contentLayout.addView(transactionView)
                } catch (e: Exception) {
                    Log.e(TAG, "Error displaying transaction at position $i", e)
                    // Continue with next transaction instead of failing
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying transactions", e)
            Toast.makeText(this, "Error loading transactions: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTransactionView(transaction: JSONObject, position: Int): View {
        try {
            // Inflate transaction item layout
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.item_transaction, null)
            
            // Get transaction data
            val name = transaction.getString("name")
            val description = transaction.optString("description", "")
            val value = transaction.getDouble("value").toFloat()
            val isExpense = transaction.getBoolean("isExpense")
            val dateStr = transaction.getString("date")
            val category = transaction.optString("category", "")

            // Format date and time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = try {
                dateFormat.parse(dateStr)
            } catch (e: Exception) {
                Date() // Fallback to current date if parse fails
            }
            
            val formattedDate = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date ?: Date())

            // Set transaction details
            view.findViewById<TextView>(R.id.transaction_name).text = name
            view.findViewById<TextView>(R.id.transaction_time).text = formattedDate
            view.findViewById<TextView>(R.id.transaction_value).text = String.format("R$ %.2f", value)
            
            // Display category if available
            if (category.isNotEmpty()) {
                val categoryText = view.findViewById<TextView>(R.id.transaction_category)
                categoryText.text = category
                categoryText.visibility = View.VISIBLE
                
                // Set category text color based on transaction type
                categoryText.setTextColor(getColor(R.color.text_secondary))
                
                // Add category background
                val categoryBackground = view.findViewById<View>(R.id.category_indicator)
                categoryBackground.visibility = View.VISIBLE
                
                // Set category indicator color based on category
                setCategoryIndicatorColor(categoryBackground, category)
            } else {
                view.findViewById<TextView>(R.id.transaction_category).visibility = View.GONE
                view.findViewById<View>(R.id.category_indicator).visibility = View.GONE
            }
            
            // Set icon and value color based on transaction type
            val icon = view.findViewById<ImageView>(R.id.transaction_icon)
            icon.setImageResource(if (isExpense) R.drawable.icon_expense else R.drawable.icon_revenue)
            icon.setColorFilter(getColor(if (isExpense) R.color.error else R.color.success))
            
            val valueText = view.findViewById<TextView>(R.id.transaction_value)
            valueText.setTextColor(getColor(if (isExpense) R.color.error else R.color.success))

            // Set description if available
            if (description.isNotEmpty()) {
                val descriptionText = view.findViewById<TextView>(R.id.transaction_description)
                descriptionText.text = description
                descriptionText.visibility = View.VISIBLE
            } else {
                view.findViewById<TextView>(R.id.transaction_description).visibility = View.GONE
            }

            // Set edit button click listener
            view.findViewById<ImageButton>(R.id.edit_button).setOnClickListener {
                showEditDialog(transaction, position)
            }

            // Set delete button click listener
            view.findViewById<ImageButton>(R.id.delete_button).setOnClickListener {
                showDeleteConfirmation(position)
            }

            return view
        } catch (e: Exception) {
            Log.e(TAG, "Error creating transaction view", e)
            // Return empty view in case of error
            val fallbackView = TextView(this)
            fallbackView.text = "Error displaying transaction"
            fallbackView.setPadding(16, 16, 16, 16)
            return fallbackView
        }
    }

    private fun setCategoryIndicatorColor(view: View, category: String) {
        // A map of default category colors
        val defaultColors = mapOf(
            "Food" to getColor(R.color.food_color),
            "Transportation" to getColor(R.color.transport_color),
            "Housing" to getColor(R.color.housing_color),
            "Leisure" to getColor(R.color.leisure_color),
            "Education" to getColor(R.color.education_color),
            "Health" to getColor(R.color.health_color),
            "Clothing" to getColor(R.color.clothing_color),
            "Bills" to getColor(R.color.bills_color),
            "Salary" to getColor(R.color.salary_color),
            "Investments" to getColor(R.color.investments_color)
        )
        
        // Set color based on category, or default to primary color
        val color = defaultColors[category] ?: getColor(R.color.primary)
        view.setBackgroundColor(color)
    }

    private fun showEditDialog(transaction: JSONObject, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Transaction")

        // Create layout for the dialog
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        // Name input
        val nameInput = android.widget.EditText(this)
        nameInput.hint = "Transaction Name"
        nameInput.setText(transaction.getString("name"))
        layout.addView(nameInput)

        // Value input
        val valueInput = android.widget.EditText(this)
        valueInput.hint = "Amount"
        valueInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        valueInput.setText(transaction.getDouble("value").toString())
        layout.addView(valueInput)

        // Type selection
        val typeSpinner = android.widget.Spinner(this)
        val typeAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter
        typeSpinner.setSelection(if (transaction.getBoolean("isExpense")) 1 else 0)
        layout.addView(typeSpinner)
        
        // Category selection
        val categoryLabel = TextView(this)
        categoryLabel.text = "Category"
        categoryLabel.setPadding(0, 16, 0, 4)
        layout.addView(categoryLabel)
        
        val categorySpinner = android.widget.Spinner(this)
        val categories = loadCategoriesFromSharedPreferences()
        val categoryAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        
        // Set current category if exists
        val currentCategory = transaction.optString("category", "")
        val categoryIndex = categories.indexOf(currentCategory)
        if (categoryIndex >= 0) {
            categorySpinner.setSelection(categoryIndex)
        }
        
        layout.addView(categorySpinner)

        builder.setView(layout)

        builder.setPositiveButton("Save") { dialog, which ->
            try {
                val newName = nameInput.text.toString()
                val newValue = valueInput.text.toString().toDouble()
                val isExpense = typeSpinner.selectedItemPosition == 1
                val newCategory = categorySpinner.selectedItem.toString()

                // Get current totals
                var totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
                var totalExpense = sharedPreferences.getFloat("totalExpense", 0f)

                // Remove old transaction from totals
                val oldValue = transaction.getDouble("value").toFloat()
                if (transaction.getBoolean("isExpense")) {
                    totalExpense -= oldValue
                } else {
                    totalRevenue -= oldValue
                }

                // Add new transaction to totals
                if (isExpense) {
                    totalExpense += newValue.toFloat()
                } else {
                    totalRevenue += newValue.toFloat()
                }

                // Update transaction
                transaction.put("name", newName)
                transaction.put("value", newValue)
                transaction.put("isExpense", isExpense)
                transaction.put("category", newCategory)

                // Update SharedPreferences
                val historyJson = sharedPreferences.getString("transactionHistory", "[]")
                val historyArray = JSONArray(historyJson)
                historyArray.put(position, transaction)
                
                // Save all changes
                sharedPreferences.edit()
                    .putString("transactionHistory", historyArray.toString())
                    .putFloat("totalRevenue", totalRevenue)
                    .putFloat("totalExpense", totalExpense)
                    .apply()

                // Update financial summary
                updateFinancialSummary()
                displayAllTransactions()

                Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error updating transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { dialog, which ->
                try {
                    // Get current history
                    val historyJson = sharedPreferences.getString("transactionHistory", "[]")
                    val historyArray = JSONArray(historyJson)
                    
                    // Get the transaction to be deleted
                    val transaction = historyArray.getJSONObject(position)
                    val value = transaction.getDouble("value").toFloat()
                    val isExpense = transaction.getBoolean("isExpense")

                    // Get current totals
                    var totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
                    var totalExpense = sharedPreferences.getFloat("totalExpense", 0f)

                    // Update totals
                    if (isExpense) {
                        totalExpense -= value
                    } else {
                        totalRevenue -= value
                    }

                    // Remove the transaction
                    val newArray = JSONArray()
                    for (i in 0 until historyArray.length()) {
                        if (i != position) {
                            newArray.put(historyArray.get(i))
                        }
                    }

                    // Save all changes
                    sharedPreferences.edit()
                        .putString("transactionHistory", newArray.toString())
                        .putFloat("totalRevenue", totalRevenue)
                        .putFloat("totalExpense", totalExpense)
                        .apply()

                    // Update financial summary
                    updateFinancialSummary()
                    displayAllTransactions()

                    Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error deleting transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadCategoriesFromSharedPreferences(): List<String> {
        try {
            // Load categories from SharedPreferences
            val categoriesJson = sharedPreferences.getString("user_categories", "[]")
            val jsonArray = JSONArray(categoriesJson)
            val categories = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                categories.add(jsonArray.getString(i))
            }
            
            // If no custom categories exist, use default ones
            if (categories.isEmpty()) {
                // Get default categories and resolve string resources
                val expenseCategories = resources.getStringArray(R.array.expense_categories)
                val revenueCategories = resources.getStringArray(R.array.revenue_categories)
                
                // Add all categories to the list
                categories.addAll(expenseCategories)
                categories.addAll(revenueCategories)
            }
            
            return categories
        } catch (e: Exception) {
            Log.e(TAG, "Error loading categories: ${e.message}")
            // Return default categories as fallback - use string resources when possible
            return listOf(
                getString(R.string.expense_food),
                getString(R.string.expense_transport),
                getString(R.string.expense_residence),
                getString(R.string.expense_leisure),
                getString(R.string.expense_other)
            )
        }
    }
}