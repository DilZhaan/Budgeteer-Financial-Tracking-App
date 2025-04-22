package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

class BudgetSetupActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var budgetInput: EditText
    private lateinit var saveButton: ImageButton
    private lateinit var saveButtonLarge: Button
    private lateinit var budgetManager: BudgetManager
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_setup)

        // Initialize SharedPreferences and BudgetManager
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)
        budgetManager = BudgetManager(sharedPreferences, this)

        // Link UI elements
        budgetInput = findViewById(R.id.budget_input)
        saveButton = findViewById(R.id.save_button)
        saveButtonLarge = findViewById(R.id.save_budget_button)

        // Set up return button
        findViewById<ImageButton>(R.id.return_button).setOnClickListener {
            finish()
        }

        // Set up save buttons (both toolbar and large button)
        saveButton.setOnClickListener {
            saveBudget()
        }
        
        saveButtonLarge.setOnClickListener {
            saveBudget()
        }

        // Add value formatter
        budgetInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s == null || s.isEmpty()) return
                
                // Remove the listener to avoid recursive calls
                budgetInput.removeTextChangedListener(this)
                
                try {
                    // Remove all non-numeric characters
                    val cleanString = s.toString().replace(Regex("[^\\d]"), "")
                    
                    // Convert to a decimal number (dividing by 100 to handle cents)
                    val parsed = if (cleanString.isEmpty()) 0.0 else cleanString.toDouble() / 100
                    
                    // Format the number as currency
                    val formatted = currencyFormat.format(parsed)
                    
                    // Set the formatted text
                    budgetInput.setText(formatted)
                    budgetInput.setSelection(formatted.length)
                } catch (e: Exception) {
                    // If there's an error, just use a default value
                    budgetInput.setText(currencyFormat.format(0))
                    budgetInput.setSelection(currencyFormat.format(0).length)
                }
                
                // Restore the listener
                budgetInput.addTextChangedListener(this)
            }
        })

        // Load current budget if exists
        val currentBudget = budgetManager.getMonthlyBudget()
        if (currentBudget > 0) {
            budgetInput.setText(currencyFormat.format(currentBudget))
        }
        
        // Focus on the budget input field when opening
        budgetInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(budgetInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveBudget() {
        try {
            // Extract the numeric value from the formatted string
            val budgetText = budgetInput.text.toString()
            val cleanString = budgetText.replace(Regex("[^\\d.]"), "")
            val budget = cleanString.toFloatOrNull() ?: 0f

            if (budget <= 0) {
                Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
                return
            }

            // Save the budget
            budgetManager.setMonthlyBudget(budget)
            
            // Show success message and finish activity
            Toast.makeText(this, "Monthly budget set successfully", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving budget: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 