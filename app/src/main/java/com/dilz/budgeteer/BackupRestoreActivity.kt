package com.dilz.budgeteer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var backupFilesRecyclerView: RecyclerView
    private lateinit var noBackupsText: TextView
    private lateinit var adapter: BackupFileAdapter
    private var backupFiles: List<File> = emptyList()
    
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importDataFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_restore)

        sharedPreferences = getSharedPreferences("BudgeteerPrefs", Context.MODE_PRIVATE)
        
        // Set up UI components
        findViewById<ImageButton>(R.id.btn_return).setOnClickListener {
            finish()
        }
        
        backupFilesRecyclerView = findViewById(R.id.backup_files_list)
        noBackupsText = findViewById(R.id.no_backups_text)
        
        // Set up RecyclerView
        adapter = BackupFileAdapter(emptyList()) { file ->
            showRestoreConfirmation(file)
        }
        
        backupFilesRecyclerView.layoutManager = LinearLayoutManager(this)
        backupFilesRecyclerView.adapter = adapter
        
        // Set up buttons
        findViewById<Button>(R.id.btn_export).setOnClickListener {
            exportData()
        }

        findViewById<Button>(R.id.btn_import).setOnClickListener {
            loadBackupFiles()
            if (backupFiles.isEmpty()) {
                // No existing backups, prompt to select a file from storage
                openDocumentLauncher.launch(arrayOf("application/json"))
            } else {
                // Show the list of available backups
                backupFilesRecyclerView.visibility = View.VISIBLE
                noBackupsText.visibility = View.GONE
                adapter.updateFiles(backupFiles)
            }
        }
        
        // Load backup files on startup
        loadBackupFiles()
    }
    
    private fun loadBackupFiles() {
        val backupDir = getExternalFilesDir(null)
        backupFiles = backupDir?.listFiles { file ->
            file.name.startsWith("budgeteer_backup_") && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
        
        if (backupFiles.isNotEmpty()) {
            backupFilesRecyclerView.visibility = View.VISIBLE
            noBackupsText.visibility = View.GONE
            adapter.updateFiles(backupFiles)
        } else {
            backupFilesRecyclerView.visibility = View.GONE
            noBackupsText.visibility = View.VISIBLE
        }
    }

    private fun exportData() {
        try {
            // Get transaction history
            val transactionHistory = sharedPreferences.getString("transactionHistory", "[]")
            
            // Get financial totals
            val totalRevenue = sharedPreferences.getFloat("totalRevenue", 0f)
            val totalExpense = sharedPreferences.getFloat("totalExpense", 0f)
            
            // Get user categories
            val userCategories = sharedPreferences.getString("user_categories", "[]")
            
            // Create a JSON object with all the data
            val backupData = JSONObject().apply {
                put("transactions", JSONArray(transactionHistory))
                put("totalRevenue", totalRevenue)
                put("totalExpense", totalExpense)
                put("userCategories", JSONArray(userCategories))
                put("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("appVersion", "1.0")
            }

            // Create a timestamped filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "budgeteer_backup_$timestamp.json"
            
            // Write the data to a file
            val file = File(getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { output ->
                output.write(backupData.toString(2).toByteArray()) // Pretty-print with indentation
            }

            // Show success message with file details
            val fileSizeKb = file.length() / 1024
            val message = "${getString(R.string.backup_created)}:\n$fileName ($fileSizeKb KB)"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            
            // Refresh the backup files list
            loadBackupFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.backup_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showRestoreConfirmation(file: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.select_backup_file)
            .setMessage("Are you sure you want to restore data from this backup? This will replace all your current data.")
            .setPositiveButton("Restore") { _, _ ->
                importDataFromFile(file)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importDataFromFile(file: File) {
        try {
            // Read the backup file
            val backupContent = FileReader(file).use { it.readText() }
            val backupData = JSONObject(backupContent)
            
            // Get the transaction history
            val transactionHistory = backupData.getJSONArray("transactions")
            
            // Get financial totals
            val totalRevenue = backupData.optDouble("totalRevenue", 0.0).toFloat()
            val totalExpense = backupData.optDouble("totalExpense", 0.0).toFloat()
            
            // Get user categories
            val userCategories = backupData.optJSONArray("userCategories") ?: JSONArray()
            
            // Save all data to SharedPreferences
            with(sharedPreferences.edit()) {
                putString("transactionHistory", transactionHistory.toString())
                putFloat("totalRevenue", totalRevenue)
                putFloat("totalExpense", totalExpense)
                putString("user_categories", userCategories.toString())
                apply()
            }

            Toast.makeText(this, getString(R.string.restore_success), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.restore_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun importDataFromUri(uri: Uri) {
        try {
            // Read the backup file from the URI
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader().use { it?.readText() } ?: throw Exception("Could not read file")
            
            // Parse the JSON
            val backupData = JSONObject(content)
            
            // Get the transaction history
            val transactionHistory = backupData.getJSONArray("transactions")
            
            // Get financial totals
            val totalRevenue = backupData.optDouble("totalRevenue", 0.0).toFloat()
            val totalExpense = backupData.optDouble("totalExpense", 0.0).toFloat()
            
            // Get user categories
            val userCategories = backupData.optJSONArray("userCategories") ?: JSONArray()
            
            // Save all data to SharedPreferences
            with(sharedPreferences.edit()) {
                putString("transactionHistory", transactionHistory.toString())
                putFloat("totalRevenue", totalRevenue)
                putFloat("totalExpense", totalExpense)
                putString("user_categories", userCategories.toString())
                apply()
            }

            Toast.makeText(this, getString(R.string.restore_success), Toast.LENGTH_LONG).show()
            
            // Save a copy to the app's backup directory
            val fileName = "budgeteer_backup_imported_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val file = File(getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { output ->
                contentResolver.openInputStream(uri)?.copyTo(output)
            }
            
            // Refresh the backup files list
            loadBackupFiles()
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.restore_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 