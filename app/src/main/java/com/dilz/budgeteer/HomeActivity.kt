package com.dilz.budgeteer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var balanceTextView: TextView
    private lateinit var revenueTextView: TextView
    private lateinit var expenseTextView: TextView
    private lateinit var budgetTextView: TextView
    private lateinit var budgetProgressTextView: TextView
    private lateinit var budgetWarningTextView: TextView
    private lateinit var categoryAnalysisLayout: LinearLayout
    private lateinit var budgetManager: BudgetManager
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)
        budgetManager = BudgetManager(sharedPreferences, this)

        // Find necessary TextViews
        balanceTextView = findViewById(R.id.textView4)
        revenueTextView = findViewById(R.id.revenue_amount)
        expenseTextView = findViewById(R.id.expense_amount)
        budgetTextView = findViewById(R.id.budget_amount)
        budgetProgressTextView = findViewById(R.id.budget_progress)
        budgetWarningTextView = findViewById(R.id.budget_warning)
        categoryAnalysisLayout = findViewById(R.id.category_analysis_layout)

        // Initialize navigation drawer components
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        // Update financial summary
        updateFinancialSummary()
        updateBudgetInfo()
        updateCategoryAnalysis()

        // Setup navigation drawer
        setupNavigationDrawer()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to the activity
        updateFinancialSummary()
        updateBudgetInfo()
        updateCategoryAnalysis()
    }

    private fun updateFinancialSummary() {
        // Get financial data from SharedPreferences
        val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
        val totalExpense = sharedPreferences.getFloat("totalExpense", 0f)
        val balance = totalRevenue - totalExpense

        Log.d(TAG, "Financial summary: Revenue=$totalRevenue, Expense=$totalExpense, Balance=$balance")

        // Update TextViews
        balanceTextView.text = String.format("R$ %.2f", balance)
        revenueTextView.text = String.format("R$ %.2f", totalRevenue)
        expenseTextView.text = String.format("R$ %.2f", totalExpense)
    }

    private fun updateBudgetInfo() {
        val budget = budgetManager.getMonthlyBudget()
        val progress = budgetManager.getBudgetProgress()
        val shouldShowWarning = budgetManager.shouldShowBudgetWarning()

        budgetTextView.text = String.format("R$ %.2f", budget)
        budgetProgressTextView.text = String.format("%.1f%%", progress)

        if (shouldShowWarning) {
            budgetWarningTextView.visibility = View.VISIBLE
            budgetWarningTextView.text = "Warning: You've spent ${progress.toInt()}% of your monthly budget!"
        } else {
            budgetWarningTextView.visibility = View.GONE
        }
    }

    private fun updateCategoryAnalysis() {
        categoryAnalysisLayout.removeAllViews()

        // Get total expenses for the current month
        val totalExpenses = budgetManager.getCurrentMonthExpenses()
        if (totalExpenses == 0f) {
            val noDataText = TextView(this)
            noDataText.text = "No expense data available for this month"
            noDataText.textSize = 16f
            noDataText.setTextColor(getColor(R.color.black))
            categoryAnalysisLayout.addView(noDataText)
            return
        }

        // Create a header
        val header = TextView(this)
        header.text = "Category-wise Spending"
        header.textSize = 18f
        header.setTextColor(getColor(R.color.black))
        categoryAnalysisLayout.addView(header)

        // Load categories from SharedPreferences
        val categoriesJson = sharedPreferences.getString("user_categories", "[]")
        val jsonArray = JSONArray(categoriesJson)
        val categories = mutableListOf<String>()

        for (i in 0 until jsonArray.length()) {
            categories.add(jsonArray.getString(i))
        }

        // Add analysis for each category
        for (category in categories) {
            val categoryExpense = budgetManager.getCategoryExpenses(category)
            if (categoryExpense > 0) {
                val percentage = (categoryExpense / totalExpenses) * 100
                val categoryRow = createCategoryRow(category, categoryExpense, percentage)
                categoryAnalysisLayout.addView(categoryRow)
            }
        }
    }

    private fun createCategoryRow(category: String, amount: Float, percentage: Float): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.setPadding(0, 8, 0, 8)

        val categoryText = TextView(this)
        categoryText.text = category
        categoryText.textSize = 16f
        categoryText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val amountText = TextView(this)
        amountText.text = String.format("R$ %.2f (%.1f%%)", amount, percentage)
        amountText.textSize = 16f
        amountText.gravity = android.view.Gravity.END

        row.addView(categoryText)
        row.addView(amountText)
        return row
    }

    private fun createTransactionRow(transaction: org.json.JSONObject): LinearLayout {
        // Create horizontal layout for the transaction
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.gravity = android.view.Gravity.CENTER_VERTICAL
        layout.setPadding(0, 5, 0, 5)

        // Extract transaction data
        val name = transaction.getString("name")
        val value = transaction.getDouble("value").toFloat()
        val isExpense = transaction.getBoolean("isExpense")
        val dateStr = transaction.getString("date")

        // Format time
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())

        // Left container (icon + name)
        val leftContainer = LinearLayout(this)
        leftContainer.orientation = LinearLayout.HORIZONTAL
        leftContainer.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f // weight
        )
        leftContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        // Color indicator (using a hardcoded size since we don't have the dimension resource)
        val colorIndicator = ImageView(this)
        val iconSize = (15 * resources.displayMetrics.density).toInt() // Convert 15dp to pixels
        colorIndicator.layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)

        colorIndicator.setImageResource(
            if (isExpense) R.drawable.color_red else R.drawable.color_green
        )
        colorIndicator.contentDescription =
            if (isExpense) getString(R.string.desc_img_expense) else getString(R.string.desc_img_revenue)

        leftContainer.addView(colorIndicator)

        // Transaction name
        val nameView = TextView(this)
        nameView.text = name
        nameView.textSize = 14f
        nameView.setTextColor(getColor(R.color.black))
        nameView.setPadding(5, 0, 5, 0)
        leftContainer.addView(nameView)

        layout.addView(leftContainer)

        // Time
        val timeView = TextView(this)
        timeView.text = timeStr
        timeView.textSize = 14f
        timeView.setTextColor(getColor(R.color.black))
        layout.addView(timeView)

        // Value
        val valueView = TextView(this)
        valueView.text = String.format("R$ %.2f", value)
        valueView.textSize = 14f
        valueView.setTextColor(getColor(R.color.black))
        valueView.setPadding(10, 0, 0, 0)
        layout.addView(valueView)

        return layout
    }

    fun onHistoryClick(view: View) {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    fun onRegisterClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    fun onCategoryClick(view: View) {
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
    }

    fun onBudgetSetupClick(view: View) {
        val intent = Intent(this, BudgetSetupActivity::class.java)
        startActivity(intent)
    }

    private fun setupNavigationDrawer() {
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already in HomeActivity
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetSetupActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_backup -> {
                    startActivity(Intent(this, BackupRestoreActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_signout -> {
                    // Handle sign out
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}