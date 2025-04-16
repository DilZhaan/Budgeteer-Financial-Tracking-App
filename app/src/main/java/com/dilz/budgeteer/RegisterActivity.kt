package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var inputCategory: EditText
    private lateinit var inputValue: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        inputCategory = findViewById(R.id.inputCategoria)
        inputValue = findViewById(R.id.inputValue)
        registerButton = findViewById(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val category = inputCategory.text.toString()
            val value = inputValue.text.toString().toFloatOrNull()

            if (category.isBlank() || value == null) {
                Toast.makeText(this, "Please fill all fields correctly!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val editor = sharedPreferences.edit()
            if (value > 0) {
                val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f) + value
                editor.putFloat("totalRevenue", totalRevenue)
            } else {
                val totalExpense = sharedPreferences.getFloat("totalExpense", 0f) + Math.abs(value)
                editor.putFloat("totalExpense", totalExpense)
            }
            editor.apply()

            Toast.makeText(this, "Transaction registered!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}