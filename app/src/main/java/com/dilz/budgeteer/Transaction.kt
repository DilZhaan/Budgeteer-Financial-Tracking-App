package com.dilz.budgeteer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

data class Transaction(
    val name: String,
    val description: String,
    val category: String,
    val value: Float,
    val isExpense: Boolean,
    val date: Date
) {
    companion object {
        fun fromJson(json: JSONObject): Transaction {
            return Transaction(
                name = json.getString("name"),
                description = json.getString("description"),
                category = json.getString("category"),
                value = json.getDouble("value").toFloat(),
                isExpense = json.getBoolean("isExpense"),
                date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .parse(json.getString("date")) ?: Date()
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", description)
            put("category", category)
            put("value", value)
            put("isExpense", isExpense)
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date))
        }
    }
}