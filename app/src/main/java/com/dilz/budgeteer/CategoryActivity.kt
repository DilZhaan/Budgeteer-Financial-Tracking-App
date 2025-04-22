package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray

class CategoryActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var categoryList: RecyclerView
    private lateinit var categoryInput: EditText
    private lateinit var addButton: Button
    private lateinit var categories: MutableList<String>
    private lateinit var adapter: CategoryAdapter

    companion object {
        private const val CATEGORIES_KEY = "user_categories"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgeteerPrefs", MODE_PRIVATE)

        // Link UI elements
        categoryList = findViewById(R.id.category_list)
        categoryInput = findViewById(R.id.category_input)
        addButton = findViewById(R.id.add_category_button)

        // Set up return button
        findViewById<ImageButton>(R.id.return_button).setOnClickListener {
            finish()
        }

        // Initialize categories list
        categories = mutableListOf()
        
        // Set up RecyclerView
        adapter = CategoryAdapter(categories) { position ->
            showDeleteConfirmationDialog(position)
        }
        
        categoryList.layoutManager = LinearLayoutManager(this)
        categoryList.adapter = adapter

        // Load existing categories
        loadCategories()

        // Set up add button
        addButton.setOnClickListener {
            addCategory()
        }
    }

    private fun loadCategories() {
        try {
            // Load categories from SharedPreferences
            val categoriesJson = sharedPreferences.getString(CATEGORIES_KEY, "[]")
            val jsonArray = JSONArray(categoriesJson)
            categories.clear()

            for (i in 0 until jsonArray.length()) {
                categories.add(jsonArray.getString(i))
            }

            // Update adapter
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCategories() {
        try {
            val jsonArray = JSONArray()
            categories.forEach { category ->
                jsonArray.put(category)
            }
            sharedPreferences.edit().putString(CATEGORIES_KEY, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving categories: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addCategory() {
        val newCategory = categoryInput.text.toString().trim()
        if (newCategory.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        if (categories.contains(newCategory)) {
            Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Add to the list and update the adapter
            categories.add(newCategory)
            adapter.notifyItemInserted(categories.size - 1)
            categoryInput.text.clear()
            
            // Hide keyboard
            hideKeyboard()

            // Save to SharedPreferences
            saveCategories()

            Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error adding category: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeleteConfirmationDialog(position: Int) {
        if (position in categories.indices) {
            val categoryToDelete = categories[position]
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '$categoryToDelete'?")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Delete") { dialog, _ ->
                    deleteCategory(position)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun deleteCategory(position: Int) {
        if (position in categories.indices) {
            val categoryToDelete = categories[position]
            
            try {
                categories.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveCategories()
                Toast.makeText(this, "Category '$categoryToDelete' deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error deleting category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
} 