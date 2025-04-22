package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.UUID

class RegisterActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var nameInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var typeInput: TextInputEditText
    private lateinit var valueInput: EditText
    private lateinit var categoryDropdown: MaterialAutoCompleteTextView
    private var isExpense = true // Default is expense
    private val TAG = "RegisterActivity"
    private lateinit var budgetManager: BudgetManager

    // Added to handle Material Design components
    private lateinit var typeButtonGroup: com.google.android.material.button.MaterialButtonToggleGroup
    private lateinit var transactionDrawable: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)
        budgetManager = BudgetManager(sharedPreferences, this)

        // Link UI elements
        nameInput = findViewById(R.id.name_input)
        descriptionInput = findViewById(R.id.description_input)
        typeInput = findViewById(R.id.transaction_type_input)
        valueInput = findViewById(R.id.value_input)
        categoryDropdown = findViewById(R.id.category_dropdown)
        typeButtonGroup = findViewById(R.id.type_toggle_group)
        transactionDrawable = findViewById(R.id.transaction_icon)

        // Set up return button
        findViewById<ImageButton>(R.id.return_button).setOnClickListener {
            finish()
        }

        // Set up add button (FAB)
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_save).setOnClickListener {
            saveTransaction()
        }

        // Set up save button (for the button at the bottom of the form)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.save_button)?.setOnClickListener {
            saveTransaction()
        }

        // Set up transaction type toggle
        typeButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.expense_button -> {
                        isExpense = true
                        typeInput.setText(getString(R.string.expense))
                        transactionDrawable.setImageResource(R.drawable.expense_icon)
                    }
                    R.id.revenue_button -> {
                        isExpense = false
                        typeInput.setText(getString(R.string.revenue))
                        transactionDrawable.setImageResource(R.drawable.revenue_icon)
                    }
                }
                updateCategorySpinner()
            }
        }

        // Set default transaction type (expense)
        typeButtonGroup.check(R.id.expense_button)
        
        // Set up value input formatter
        valueInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.isNotEmpty()) {
                    try {
                        // Format as currency
                        val value = s.toString().replace("[^\\d]".toRegex(), "").toDouble() / 100
                        val formattedValue = NumberFormat.getCurrencyInstance().format(value)
                        
                        // Remove the listener to avoid recursion
                        valueInput.removeTextChangedListener(this)
                        
                        // Update text
                        valueInput.setText(formattedValue)
                        valueInput.setSelection(formattedValue.length)
                        
                        // Add the listener back
                        valueInput.addTextChangedListener(this)
                    } catch (e: Exception) {
                        Log.e("RegisterActivity", "Error formatting value: ${e.message}")
                    }
                }
            }
        })

        updateCategorySpinner()
    }

    private fun updateCategorySpinner() {
        try {
            // Load categories from SharedPreferences
            val categoriesJson = sharedPreferences.getString("user_categories", "[]")
            val jsonArray = JSONArray(categoriesJson)
            val categories = mutableListOf<String>()

            // Parse categories from JSON
            for (i in 0 until jsonArray.length()) {
                categories.add(jsonArray.getString(i))
            }

            // If no categories exist, fall back to default categories
            if (categories.isEmpty()) {
                val defaultCategories = if (isExpense) {
                    resources.getStringArray(R.array.expense_categories).toList()
                } else {
                    resources.getStringArray(R.array.revenue_categories).toList()
                }
                categories.addAll(defaultCategories)
            }
            
            // Create adapter for dropdown
            val adapter = ArrayAdapter(this, R.layout.dropdown_item, categories)
            
            // Set the adapter for the AutoCompleteTextView
            categoryDropdown.setAdapter(adapter)
            
            // Set a default value
            if (categories.isNotEmpty()) {
                categoryDropdown.setText(categories[0], false)
            }
        } catch (e: Exception) {
            // If there's an error, log it and fall back to default categories
            Log.e(TAG, "Error loading categories: ${e.message}")
            
            // Fall back to default categories
            val defaultCategories = if (isExpense) {
                resources.getStringArray(R.array.expense_categories)
            } else {
                resources.getStringArray(R.array.revenue_categories)
            }
            
            val adapter = ArrayAdapter(this, R.layout.dropdown_item, defaultCategories)
            categoryDropdown.setAdapter(adapter)
            
            if (defaultCategories.isNotEmpty()) {
                categoryDropdown.setText(defaultCategories[0], false)
            }
        }
    }

    private fun saveTransaction() {
        // Validate inputs
        val name = nameInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val valueText = valueInput.text.toString().trim()
        val category = categoryDropdown.text.toString()

        if (name.isEmpty() || valueText.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse value (handling currency format)
        val value = try {
            // Extract numeric value from currency string
            valueText.replace("[^\\d.]".toRegex(), "").toFloatOrNull() ?: 0f
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid value format", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create transaction object using the Transaction class
            val transaction = Transaction(
                name = name,
                description = description,
                category = category,
                value = value,
                isExpense = isExpense,
                date = Date()
            )

            // Convert to JSON for storage
            val transactionJson = transaction.toJson()
            Log.d(TAG, "New transaction created: $transactionJson")

            // Get current history as string
            val historyJson = sharedPreferences.getString("transactionHistory", "[]")
            Log.d(TAG, "Current history JSON: $historyJson")

            // Convert to JSONArray
            val historyArray = JSONArray(historyJson)
            Log.d(TAG, "Current history array length: ${historyArray.length()}")

            // Add new transaction at the beginning
            val newHistoryArray = JSONArray()
            newHistoryArray.put(transactionJson) // Add new transaction first
            
            // Add existing transactions
            for (i in 0 until historyArray.length()) {
                newHistoryArray.put(historyArray.get(i))
            }

            // Update financial totals
            val editor = sharedPreferences.edit()
            
            // Update totals based on transaction type
            if (isExpense) {
                val totalExpense = sharedPreferences.getFloat("totalExpense", 0f) + value
                editor.putFloat("totalExpense", totalExpense)
            } else {
                val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f) + value
                editor.putFloat("totalRevenue", totalRevenue)
            }
            
            // Save transaction history
            editor.putString("transactionHistory", newHistoryArray.toString())
            editor.apply()
            
            // Check budget thresholds if this is an expense
            if (isExpense) {
                budgetManager.checkBudgetStatusAfterTransaction()
                budgetManager.checkBudgetThresholdsWithNotifications()
            }
            
            Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction: ${e.message}")
            Toast.makeText(this, "Error saving transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}