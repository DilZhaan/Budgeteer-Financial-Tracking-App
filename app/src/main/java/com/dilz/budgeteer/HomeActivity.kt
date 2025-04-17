package com.dilz.budgeteer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var balanceTextView: TextView
    private lateinit var revenueTextView: TextView
    private lateinit var expenseTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        // Find view components from your layout - using safe approach
        balanceTextView = findViewById(R.id.textView4)

        // Get TextViews for revenue and expense values
        // Use safe approaches to find these views
        try {
            val revenueParent = findViewById<View>(R.id.receita).parent as LinearLayout
            val revenueNestedLayout = revenueParent.getChildAt(1) as LinearLayout
            revenueTextView = revenueNestedLayout.getChildAt(1) as TextView

            val expenseParent = findViewById<View>(R.id.expense).parent as LinearLayout
            val expenseNestedLayout = expenseParent.getChildAt(1) as LinearLayout
            expenseTextView = expenseNestedLayout.getChildAt(1) as TextView
        } catch (e: Exception) {
            // Fallback if view hierarchy doesn't match
            // Log the error but don't crash
            // This will avoid crashes if the layout doesn't match expectations
        }

        // Set click listener for register button
        findViewById<ImageButton>(R.id.registrar).setOnClickListener {
            onRegisterClick(it)
        }

        // Update financial summary
        updateFinancialSummary()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to the activity
        updateFinancialSummary()
    }

    private fun updateFinancialSummary() {
        // Get financial data from SharedPreferences
        val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
        val totalExpense = sharedPreferences.getFloat("totalExpense", 0f)
        val balance = totalRevenue - totalExpense

        // Update UI with formatted values - handle null views gracefully
        balanceTextView.text = String.format("R$ %.2f", balance)
        try {
            revenueTextView.text = String.format("R$ %.2f", totalRevenue)
            expenseTextView.text = String.format("R$ %.2f", totalExpense)
        } catch (e: Exception) {
            // Handle case where views might not be initialized
        }
    }

    fun onHistoryClick(view: View) {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    fun onCategoryClick(view: View) {
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
    }

    fun onRegisterClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    fun onDashboardClick(view: View) {
        // Placeholder for dashboard action
        // Uncomment if you implement DashboardActivity
        // val intent = Intent(this, DashboardActivity::class.java)
        // startActivity(intent)
    }
}