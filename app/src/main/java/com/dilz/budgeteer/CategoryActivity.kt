package com.dilz.budgeteer

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class CategoryActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var categoryList: ListView
    private lateinit var categoryInput: EditText
    private lateinit var addButton: Button
    private lateinit var categories: MutableList<String>
    private lateinit var adapter: ArrayAdapter<String>

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

        // Load existing categories
        loadCategories()

        // Set up add button
        addButton.setOnClickListener {
            addCategory()
        }

        // Set up delete on long click
        categoryList.setOnItemLongClickListener { _, _, position, _ ->
            deleteCategory(position)
            true
        }
    }

    private fun loadCategories() {
        // Load categories from SharedPreferences
        val categoriesJson = sharedPreferences.getString(CATEGORIES_KEY, "[]")
        val jsonArray = JSONArray(categoriesJson)
        categories = mutableListOf()

        for (i in 0 until jsonArray.length()) {
            categories.add(jsonArray.getString(i))
        }

        // Create adapter and set it to the list view
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        categoryList.adapter = adapter
    }

    private fun saveCategories() {
        val jsonArray = JSONArray()
        categories.forEach { category ->
            jsonArray.put(category)
        }
        sharedPreferences.edit().putString(CATEGORIES_KEY, jsonArray.toString()).apply()
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

        // Add to the list and update the adapter
        categories.add(newCategory)
        adapter.notifyDataSetChanged()
        categoryInput.text.clear()

        // Save to SharedPreferences
        saveCategories()

        Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun deleteCategory(position: Int) {
        if (position in categories.indices) {
            val categoryToDelete = categories[position]
            categories.removeAt(position)
            adapter.notifyDataSetChanged()
            saveCategories()
            Toast.makeText(this, "Category '$categoryToDelete' deleted", Toast.LENGTH_SHORT).show()
        }
    }
} 