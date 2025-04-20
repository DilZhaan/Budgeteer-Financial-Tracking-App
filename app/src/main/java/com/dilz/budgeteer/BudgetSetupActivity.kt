package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BudgetSetupActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var budgetInput: EditText
    private lateinit var budgetManager: BudgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_setup)

        // Initialize SharedPreferences and BudgetManager
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)
        budgetManager = BudgetManager(sharedPreferences, this)

        // Link UI elements
        budgetInput = findViewById(R.id.budget_input)

        // Set up return button
        findViewById<ImageButton>(R.id.return_button).setOnClickListener {
            finish()
        }

        // Set up save button
        findViewById<ImageButton>(R.id.save_button).setOnClickListener {
            saveBudget()
        }

        // Add value formatter
        budgetInput.addTextChangedListener(object : TextWatcher {
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
                        budgetInput.setText(String.format("%.2f", amount))
                        budgetInput.setSelection(budgetInput.text.length)
                    } catch (e: Exception) {
                        budgetInput.setText("0.00")
                        budgetInput.setSelection(budgetInput.text.length)
                    }
                }

                isFormatting = false
            }
        })

        // Load current budget if exists
        val currentBudget = budgetManager.getMonthlyBudget()
        if (currentBudget > 0) {
            budgetInput.setText(String.format("%.2f", currentBudget))
        }
    }

    private fun saveBudget() {
        val budgetText = budgetInput.text.toString().replace(",", ".")
        val budget = try {
            budgetText.toFloat()
        } catch (e: NumberFormatException) {
            0f
        }

        if (budget <= 0) {
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        budgetManager.setMonthlyBudget(budget)
        Toast.makeText(this, "Monthly budget set successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
} 