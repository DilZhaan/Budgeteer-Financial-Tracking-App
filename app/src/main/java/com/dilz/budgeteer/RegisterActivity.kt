package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
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
    private lateinit var categoryInput: EditText
    private lateinit var typeInput: EditText
    private lateinit var valueInput: EditText
    private lateinit var addButton: ImageButton
    private lateinit var returnButton: ImageButton

    private var isExpense = true // Default is expense
    private val textWatchers = ArrayList<TextWatcher>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        // Link UI elements
        nameInput = findViewById(R.id.input_name)
        descriptionInput = findViewById(R.id.input_description)
        categoryInput = findViewById(R.id.input_categoria)
        typeInput = findViewById(R.id.input_value) // This is for revenue/expense selection
        valueInput = findViewById(R.id.spinner_income_outcome) // This is for the amount
        addButton = findViewById(R.id.imageButton)
        returnButton = findViewById(R.id.return_button)

        // Set default transaction type
        typeInput.setText(getString(R.string.expense))

        // Clear default text when fields are focused
        setupInputClearOnFocus(nameInput, getString(R.string.name))
        setupInputClearOnFocus(descriptionInput, getString(R.string.description))
        setupInputClearOnFocus(categoryInput, getString(R.string.category))

        // Handle toggle between revenue and expense
        typeInput.setOnClickListener {
            toggleTransactionType()
        }

        // Format value input as currency
        val valueWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != "0" && valueInput.hasFocus()) {
                    formatAsCurrency(s)
                }
            }
        }

        valueInput.addTextChangedListener(valueWatcher)
        textWatchers.add(valueWatcher)

        // Handle return button click
        returnButton.setOnClickListener {
            finish()
        }

        // Handle add button click
        addButton.setOnClickListener {
            saveTransaction()
        }
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
        typeInput.setText(if (isExpense) R.string.expense else R.string.revenue)
    }

    private fun formatAsCurrency(s: Editable?) {
        if (s == null) return

        // Remove non-numeric characters
        val cleanString = s.toString().replace(Regex("[^\\d]"), "")

        if (cleanString.isEmpty()) {
            valueInput.setText("0")
            valueInput.setSelection(1)
            return
        }

        try {
            // Handle TextWatchers to prevent infinite loops
            for (watcher in textWatchers) {
                valueInput.removeTextChangedListener(watcher)
            }

            // Convert to decimal value
            val parsed = cleanString.toDouble() / 100

            // Format as currency
            val formatted = String.format(Locale.getDefault(), "%.2f", parsed)

            // Update text
            valueInput.setText(formatted)
            valueInput.setSelection(formatted.length)

            // Add TextWatchers back
            for (watcher in textWatchers) {
                valueInput.addTextChangedListener(watcher)
            }
        } catch (e: Exception) {
            // Handle formatting errors
            valueInput.setText("0.00")
            valueInput.setSelection(4)
        }
    }

    private fun saveTransaction() {
        // Get input values
        val name = nameInput.text.toString()
        val description = descriptionInput.text.toString()
        val category = categoryInput.text.toString()
        val valueText = valueInput.text.toString()

        // Validate inputs
        if (name == getString(R.string.name) ||
            category == getString(R.string.category) ||
            valueText == "0" || valueText == "0.00") {

            Toast.makeText(this, getString(R.string.validation_error), Toast.LENGTH_SHORT).show()
            return
        }

        // Parse value
        val value = try {
            valueText.replace(",", ".").toFloat()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, getString(R.string.validation_error), Toast.LENGTH_SHORT).show()
            return
        }

        // Save transaction data
        val editor = sharedPreferences.edit()

        // Update totals based on transaction type
        if (isExpense) {
            val totalExpense = sharedPreferences.getFloat("totalExpense", 0f) + value
            editor.putFloat("totalExpense", totalExpense)
        } else {
            val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f) + value
            editor.putFloat("totalRevenue", totalRevenue)
        }

        // Create transaction record
        val transaction = JSONObject().apply {
            put("name", name)
            put("description", description)
            put("category", category)
            put("value", value)
            put("isExpense", isExpense)
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }

        // Get and update transaction history
        val historyJson = sharedPreferences.getString("transactionHistory", "[]")
        val historyArray = JSONArray(historyJson)
        historyArray.put(0, transaction) // Add to beginning of array (newest first)

        // Save updated history
        editor.putString("transactionHistory", historyArray.toString())
        editor.apply()

        Toast.makeText(this, getString(R.string.transaction_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}