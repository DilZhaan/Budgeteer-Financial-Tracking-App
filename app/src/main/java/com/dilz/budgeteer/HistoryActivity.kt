package com.dilz.budgeteer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var filterSpinner: Spinner
    private lateinit var historyAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        listView = findViewById(R.id.listViewHistory)
        filterSpinner = findViewById(R.id.spinnerFilter)

        val categories = arrayOf("All", "Food", "Transport", "Residence", "Leisure")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        val historyData = listOf(
            "Food - $20.00 - 2025-04-14",
            "Leisure - $50.00 - 2025-04-15",
            "Transport - $15.00 - 2025-04-16"
        )
        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyData)
        listView.adapter = historyAdapter
    }
}