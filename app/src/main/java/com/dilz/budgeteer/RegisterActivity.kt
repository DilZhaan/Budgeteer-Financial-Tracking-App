package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var nameInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var typeInput: EditText
    private lateinit var valueInput: EditText
    private lateinit var categorySpinner: Spinner
    private var isExpense = true // Default is expense
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        // Link UI elements
        nameInput = findViewById(R.id.input_name)
        descriptionInput = findViewById(R.id.input_description)
        typeInput = findViewById(R.id.input_value)
        valueInput = findViewById(R.id.spinner_income_outcome)
        categorySpinner = findViewById(R.id.category_spinner)

        // Set up return button
        findViewById<ImageButton>(R.id.return_button).setOnClickListener {
            finish()
        }

        // Set up add button
        findViewById<ImageButton>(R.id.imageButton).setOnClickListener {
            saveTransaction()
        }

        // Set default transaction type
        typeInput.setText(getString(R.string.expense))
        isExpense = true
        updateCategorySpinner()

        // Clear default text when fields are focused
        setupInputClearOnFocus(nameInput, getString(R.string.name))
        setupInputClearOnFocus(descriptionInput, getString(R.string.description))
        setupInputClearOnFocus(valueInput, "0")

        // Handle toggle between revenue and expense
        typeInput.setOnClickListener {
            toggleTransactionType()
        }

        // Add value formatter
        valueInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                isFormatting = true

                if (s != null && s.isNotEmpty() && !s.toString().equals("0")) {
                    try {
                        val digitsOnly = s.toString().replace(Regex("[^\\d]"), "")
                        val amount = if (digitsOnly.isEmpty()) 0.0 else digitsOnly.toDouble() / 100
                        valueInput.setText(String.format("%.2f", amount))
                        valueInput.setSelection(valueInput.text.length)
                    } catch (e: Exception) {
                        valueInput.setText("0.00")
                        valueInput.setSelection(valueInput.text.length)
                    }
                }

                isFormatting = false
            }
        })
    }

    private fun setupInputClearOnFocus(editText: EditText, defaultText: String) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && editText.text.toString() == defaultText) {
                editText.setText("")
            } else if (!hasFocus && editText.text.toString().isEmpty()) {
                editText.setText(defaultText)
            }
        }
    }

    private fun toggleTransactionType() {
        isExpense = !isExpense
        typeInput.setText(if (isExpense) getString(R.string.expense) else getString(R.string.revenue))
        typeInput.setCompoundDrawablesWithIntrinsicBounds(
            getDrawable(R.drawable.icon_money),
            null,
            getDrawable(if (isExpense) R.drawable.icon_suspended else R.drawable.icon_revenue),
            null
        )
        updateCategorySpinner()
    }

    private fun updateCategorySpinner() {
        // Load categories from SharedPreferences
        val categoriesJson = sharedPreferences.getString("user_categories", "[]")
        val jsonArray = JSONArray(categoriesJson)
        val categories = mutableListOf<String>()

        for (i in 0 until jsonArray.length()) {
            categories.add(jsonArray.getString(i))
        }

        // If no categories exist, add a default one
        if (categories.isEmpty()) {
            categories.add("Other")
            sharedPreferences.edit().putString("user_categories", JSONArray().put("Other").toString()).apply()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun saveTransaction() {
        // Get input values
        val name = nameInput.text.toString()
        val description = descriptionInput.text.toString()
        val category = categorySpinner.selectedItem.toString()

        // Parse value
        val valueText = valueInput.text.toString().replace(",", ".")
        val value = try {
            valueText.toFloat()
        } catch (e: NumberFormatException) {
            0f
        }

        // Validate inputs
        if (name == getString(R.string.name) || value <= 0) {
            Toast.makeText(this, R.string.validation_error, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create transaction record
            val transaction = JSONObject().apply {
                put("name", name)
                put("description", description)
                put("category", category)
                put("value", value)
                put("isExpense", isExpense)
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }

            Log.d(TAG, "New transaction created: $transaction")

            // Get current history as string
            val historyJson = sharedPreferences.getString("transactionHistory", "[]")
            Log.d(TAG, "Current history JSON: $historyJson")

            // Convert to JSONArray
            val historyArray = JSONArray(historyJson)
            Log.d(TAG, "Current history array length: ${historyArray.length()}")

            // Create a NEW JSONArray for updated history
            val newHistoryArray = JSONArray()

            // Add new transaction first (at index 0)
            newHistoryArray.put(transaction)
            Log.d(TAG, "Added new transaction to new array")

            // Then add all existing transactions
            for (i in 0 until historyArray.length()) {
                newHistoryArray.put(historyArray.get(i))
            }

            Log.d(TAG, "New history array length: ${newHistoryArray.length()}")

            // Save transaction data
            val editor = sharedPreferences.edit()

            // Update totals based on transaction type
            if (isExpense) {
                val totalExpense = sharedPreferences.getFloat("totalExpense", 0f) + value
                editor.putFloat("totalExpense", totalExpense)
                Log.d(TAG, "Updated totalExpense to: $totalExpense")
            } else {
                val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f) + value
                editor.putFloat("totalRevenue", totalRevenue)
                Log.d(TAG, "Updated totalRevenue to: $totalRevenue")
            }

            // Save the updated history JSON
            val newHistoryJson = newHistoryArray.toString()
            editor.putString("transactionHistory", newHistoryJson)

            // Use commit() for synchronous save
            val success = editor.commit()

            if (success) {
                // Verify the changes were saved
                val verifyJson = sharedPreferences.getString("transactionHistory", "[]")
                val verifyArray = JSONArray(verifyJson)
                Log.d(TAG, "Verification - stored history has ${verifyArray.length()} transactions")

                Toast.makeText(this, R.string.transaction_saved, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Log.e(TAG, "Failed to commit transaction changes!")
                Toast.makeText(this, "Error saving transaction", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}