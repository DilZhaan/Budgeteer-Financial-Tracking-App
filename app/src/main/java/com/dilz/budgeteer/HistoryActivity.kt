package com.dilz.budgeteer

import android.content.SharedPreferences
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentBalanceValue: TextView
    private lateinit var monthlyBalanceSheetValue: TextView
    private val TAG = "HistoryActivity"

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

        // Log current transaction history for debugging
        val historyJson = sharedPreferences.getString("transactionHistory", "[]")
        Log.d(TAG, "Transaction history in SharedPreferences: $historyJson")

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

    private fun displayAllTransactions() {
        try {
            // Get transaction history
            val historyJson = sharedPreferences.getString("transactionHistory", "[]")
            val historyArray = JSONArray(historyJson)

            Log.d(TAG, "Found ${historyArray.length()} transactions")

            if (historyArray.length() == 0) {
                // No transactions to display
                Toast.makeText(this, "No transactions found", Toast.LENGTH_SHORT).show()
                return
            }

            // Find the container for the transaction history
            val containerLayout = findViewById<LinearLayout>(R.id.categorias_formulario)
            if (containerLayout == null) {
                Log.e(TAG, "Could not find history container")
                return
            }

            // Find the ScrollView inside the container
            var scrollView: ScrollView? = null
            for (i in 0 until containerLayout.childCount) {
                val child = containerLayout.getChildAt(i)
                if (child is ScrollView) {
                    scrollView = child
                    break
                }
            }

            if (scrollView == null) {
                Log.e(TAG, "Could not find ScrollView")
                return
            }

            // Find or create the inner LinearLayout
            var contentLayout: LinearLayout? = null
            if (scrollView.childCount > 0 && scrollView.getChildAt(0) is LinearLayout) {
                contentLayout = scrollView.getChildAt(0) as LinearLayout
            } else {
                contentLayout = LinearLayout(this)
                contentLayout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                contentLayout.orientation = LinearLayout.VERTICAL
                scrollView.removeAllViews()
                scrollView.addView(contentLayout)
            }

            // Clear existing content
            contentLayout.removeAllViews()

            // Create a simple linear list of all transactions (for testing)
            for (i in 0 until historyArray.length()) {
                val transaction = historyArray.getJSONObject(i)
                Log.d(TAG, "Creating view for transaction: $transaction")

                val transactionView = createTransactionView(transaction, i)
                contentLayout.addView(transactionView)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying transactions", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTransactionView(transaction: JSONObject, position: Int): View {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Add padding and margins
        layout.setPadding(0, 20, 0, 20)

        // Get transaction data
        val name = transaction.getString("name")
        val value = transaction.getDouble("value").toFloat()
        val isExpense = transaction.getBoolean("isExpense")
        val dateStr = transaction.getString("date")

        // Format date and time
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
        val formattedDate = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date ?: Date())

        // Create icon
        val icon = ImageView(this)
        icon.setImageResource(if (isExpense) R.drawable.icon_expense else R.drawable.icon_revenue)
        icon.layoutParams = LinearLayout.LayoutParams(40, 40)
        (icon.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 20, 0)
        layout.addView(icon)

        // Create info section (vertical)
        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        infoLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f // Weight
        )

        // Transaction name
        val nameText = TextView(this)
        nameText.text = name
        nameText.setTextColor(getColor(R.color.black))
        nameText.textSize = 16f
        infoLayout.addView(nameText)

        // Date
        val dateText = TextView(this)
        dateText.text = formattedDate
        dateText.setTextColor(getColor(R.color.black))
        dateText.textSize = 12f
        infoLayout.addView(dateText)

        layout.addView(infoLayout)

        // Amount
        val amountText = TextView(this)
        amountText.text = String.format("R$ %.2f", value)
        amountText.setTextColor(getColor(if (isExpense) R.color.wine else R.color.green))
        amountText.textSize = 16f
        amountText.gravity = Gravity.END
        amountText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layout.addView(amountText)

        // Add edit and delete buttons
        val buttonsLayout = LinearLayout(this)
        buttonsLayout.orientation = LinearLayout.HORIZONTAL
        buttonsLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonsLayout.setPadding(10, 0, 0, 0)

        // Edit button
        val editButton = ImageButton(this)
        editButton.setImageResource(R.drawable.ic_edit)
        editButton.background = null
        editButton.setOnClickListener {
            showEditDialog(transaction, position)
        }
        buttonsLayout.addView(editButton)

        // Delete button
        val deleteButton = ImageButton(this)
        deleteButton.setImageResource(R.drawable.ic_delete)
        deleteButton.background = null
        deleteButton.setOnClickListener {
            showDeleteConfirmation(position)
        }
        buttonsLayout.addView(deleteButton)

        layout.addView(buttonsLayout)

        // Add divider
        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1 // Height of 1 pixel
        )
        divider.setBackgroundColor(getColor(R.color.light_gray))

        // Create a container for both the transaction and divider
        val containerLayout = LinearLayout(this)
        containerLayout.orientation = LinearLayout.VERTICAL
        containerLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        containerLayout.addView(layout)
        containerLayout.addView(divider)

        return containerLayout
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
        val adapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
        typeSpinner.setSelection(if (transaction.getBoolean("isExpense")) 1 else 0)
        layout.addView(typeSpinner)

        builder.setView(layout)

        builder.setPositiveButton("Save") { dialog, which ->
            try {
                val newName = nameInput.text.toString()
                val newValue = valueInput.text.toString().toDouble()
                val isExpense = typeSpinner.selectedItemPosition == 1

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
}