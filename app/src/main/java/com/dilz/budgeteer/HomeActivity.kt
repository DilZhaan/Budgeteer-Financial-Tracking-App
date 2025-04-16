package com.dilz.budgeteer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var balanceTextView: TextView
    private lateinit var revenueTextView: TextView
    private lateinit var expenseTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)
        balanceTextView = findViewById(R.id.textView4)
        revenueTextView = findViewById(R.id.textViewRevenue)
        expenseTextView = findViewById(R.id.textViewExpense)

        updateSummary()
    }

    private fun updateSummary() {
        val revenue = sharedPreferences.getFloat("totalRevenue", 0f)
        val expense = sharedPreferences.getFloat("totalExpense", 0f)
        val balance = revenue - expense

        balanceTextView.text = getString(R.string.balance_amount_template, balance)
        revenueTextView.text = getString(R.string.amount_template, revenue)
        expenseTextView.text = getString(R.string.amount_template, expense)
    }

    fun onHistoryClick(view: View) {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    fun onCategoryClick(view: View) {
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
    }

    fun onSignOutClick(view: View) {
        Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}