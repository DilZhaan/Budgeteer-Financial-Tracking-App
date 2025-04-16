package com.dilz.budgeteer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class CategoryActivity : AppCompatActivity() {

    private lateinit var categoryListView: ListView
    private lateinit var addCategoryButton: Button
    private lateinit var categoryEditText: EditText
    private lateinit var categoriesAdapter: ArrayAdapter<String>
    private val categories = mutableListOf("Food", "Transport", "Residence", "Leisure")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        categoryListView = findViewById(R.id.listViewCategories)
        addCategoryButton = findViewById(R.id.buttonAddCategory)
        categoryEditText = findViewById(R.id.editTextCategory)

        categoriesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        categoryListView.adapter = categoriesAdapter

        addCategoryButton.setOnClickListener {
            val newCategory = categoryEditText.text.toString()
            if (newCategory.isNotBlank()) {
                categories.add(newCategory)
                categoriesAdapter.notifyDataSetChanged()
                categoryEditText.text.clear()
            }
        }
    }
}