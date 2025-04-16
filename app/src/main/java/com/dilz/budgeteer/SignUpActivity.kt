package com.dilz.budgeteer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

class SignUpActivity : AppCompatActivity() {
    private lateinit var edtFullName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI components
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.editTextText2)  // This should be renamed to edtEmail in XML for better readability
        edtPassword = findViewById(R.id.edtpwd)
        edtConfirmPassword = findViewById(R.id.edtConPwd)
        btnSignUp = findViewById(R.id.btnSignUp)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Set up sign up button click listener
        btnSignUp.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
        // Get input values
        val fullName = edtFullName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString()
        val confirmPassword = edtConfirmPassword.text.toString()

        // Validate inputs
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return
        }

        // Check if user already exists
        if (isEmailAlreadyRegistered(email)) {
            showError(edtEmail, "Email already registered")
            return
        }

        // Save user data
        saveUserData(fullName, email, password)

        // Show success message
        Snackbar.make(findViewById(R.id.main), "Registration successful!", Snackbar.LENGTH_LONG).show()

        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close the current activity
    }

    private fun validateInputs(fullName: String, email: String, password: String, confirmPassword: String): Boolean {
        // Clear previous errors
        edtFullName.error = null
        edtEmail.error = null
        edtPassword.error = null
        edtConfirmPassword.error = null

        // Validate full name
        if (fullName.isEmpty()) {
            showError(edtFullName, "Full name is required")
            return false
        }

        // Validate email
        if (email.isEmpty()) {
            showError(edtEmail, "Email is required")
            return false
        }
        if (!isValidEmail(email)) {
            showError(edtEmail, "Invalid email format")
            return false
        }

        // Validate password
        if (password.isEmpty()) {
            showError(edtPassword, "Password is required")
            return false
        }
        if (password.length < 6) {
            showError(edtPassword, "Password must be at least 6 characters")
            return false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            showError(edtConfirmPassword, "Please confirm your password")
            return false
        }
        if (password != confirmPassword) {
            showError(edtConfirmPassword, "Passwords do not match")
            return false
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showError(view: EditText, message: String) {
        view.error = message
        view.requestFocus()
    }

    private fun isEmailAlreadyRegistered(email: String): Boolean {
        val usersFile = File(filesDir, "users.json")
        if (!usersFile.exists()) {
            return false
        }

        try {
            val jsonString = usersFile.readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val userObject = jsonArray.getJSONObject(i)
                if (userObject.getString("email") == email) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    private fun saveUserData(fullName: String, email: String, password: String) {
        // Save to SharedPreferences (for quick login checks)
        val editor = sharedPreferences.edit()
        editor.putString("last_registered_email", email)
        editor.apply()

        // Save to internal storage (JSON file for all users)
        saveUserToInternalStorage(fullName, email, password)
    }

    private fun saveUserToInternalStorage(fullName: String, email: String, password: String) {
        val usersFile = File(filesDir, "users.json")
        val userObject = JSONObject().apply {
            put("fullName", fullName)
            put("email", email)
            put("password", password)
            put("createdAt", System.currentTimeMillis())
        }

        try {
            val jsonArray = if (usersFile.exists()) {
                val jsonString = usersFile.readText()
                JSONArray(jsonString)
            } else {
                JSONArray()
            }

            jsonArray.put(userObject)

            FileWriter(usersFile).use { writer ->
                writer.write(jsonArray.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle the sign-in button click
    fun onSignInClick(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close the current activity
    }
}