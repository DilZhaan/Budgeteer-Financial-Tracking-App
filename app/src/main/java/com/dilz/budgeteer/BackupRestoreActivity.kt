package com.dilz.budgeteer

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_restore)

        sharedPreferences = getSharedPreferences("BudgeteerPrefs", Context.MODE_PRIVATE)

        findViewById<Button>(R.id.btn_export).setOnClickListener {
            exportData()
        }

        findViewById<Button>(R.id.btn_import).setOnClickListener {
            importData()
        }
    }

    private fun exportData() {
        try {
            val transactionHistory = sharedPreferences.getString("transactionHistory", "[]")
            val budget = sharedPreferences.getFloat("monthly_budget", 0f)
            
            val backupData = JSONArray().apply {
                put(JSONArray(transactionHistory))
                put(budget)
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "budgeteer_backup_$timestamp.json"
            
            val file = File(getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { output ->
                output.write(backupData.toString().toByteArray())
            }

            Toast.makeText(this, "Backup created successfully: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        try {
            val backupFiles = getExternalFilesDir(null)?.listFiles { file ->
                file.name.startsWith("budgeteer_backup_") && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }

            if (backupFiles.isNullOrEmpty()) {
                Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
                return
            }

            // Get the most recent backup file
            val backupFile = backupFiles.first()
            val backupContent = FileReader(backupFile).use { it.readText() }
            val backupData = JSONArray(backupContent)

            // Restore transaction history
            val transactionHistory = backupData.getJSONArray(0)
            with(sharedPreferences.edit()) {
                putString("transactionHistory", transactionHistory.toString())
                apply()
            }

            // Restore budget
            val budget = backupData.getDouble(1).toFloat()
            with(sharedPreferences.edit()) {
                putFloat("monthly_budget", budget)
                apply()
            }

            // Recalculate total revenue and expense
            var totalRevenue = 0f
            var totalExpense = 0f

            for (i in 0 until transactionHistory.length()) {
                val transaction = transactionHistory.getJSONObject(i)
                val value = transaction.getDouble("value").toFloat()
                val isExpense = transaction.getBoolean("isExpense")

                if (isExpense) {
                    totalExpense += value
                } else {
                    totalRevenue += value
                }
            }

            // Save the recalculated totals
            with(sharedPreferences.edit()) {
                putFloat("totalRevenue", totalRevenue)
                putFloat("totalExpense", totalExpense)
                apply()
            }

            Toast.makeText(this, "Data restored successfully from ${backupFile.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error restoring backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 