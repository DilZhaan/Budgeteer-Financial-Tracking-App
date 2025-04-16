package com.dilz.budgeteer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import java.io.File

class LoginActivity : AppCompatActivity() {
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var tvForgotPwd: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if user is already logged in
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("is_logged_in", false)) {
            // User is already logged in, go directly to home screen
            navigateToHome()
            return
        }

        // Initialize UI components
        edtEmail = findViewById(R.id.ectEmail)
        edtPassword = findViewById(R.id.edtPwd)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvForgotPwd = findViewById(R.id.tvFogotPwd)

        // Set up click listeners
        btnSignIn.setOnClickListener {
            signIn()
        }

        tvForgotPwd.setOnClickListener {
            // You can implement forgot password functionality here
            Toast.makeText(this, "Forgot Password functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        // Check if user just registered and prefill email
        val lastRegisteredEmail = sharedPreferences.getString("last_registered_email", "")
        if (!lastRegisteredEmail.isNullOrEmpty()) {
            edtEmail.setText(lastRegisteredEmail)
            edtPassword.requestFocus()
        }
    }

    private fun signIn() {
        // Get input values
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString()

        // Validate inputs
        if (!validateInputs(email, password)) {
            return
        }

        // Check credentials
        if (authenticateUser(email, password)) {
            // Save current user session
            saveUserSession(email)

            // Show success message
            Snackbar.make(findViewById(R.id.main), "Login successful!", Snackbar.LENGTH_SHORT).show()

            // Navigate to main screen
            navigateToHome()
        } else {
            // Show error message
            Snackbar.make(findViewById(R.id.main), "Invalid email or password", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        // Clear previous errors
        edtEmail.error = null
        edtPassword.error = null

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

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showError(view: EditText, message: String) {
        view.error = message
        view.requestFocus()
    }

    private fun authenticateUser(email: String, password: String): Boolean {
        val usersFile = File(filesDir, "users.json")
        if (!usersFile.exists()) {
            return false
        }

        try {
            val jsonString = usersFile.readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val userObject = jsonArray.getJSONObject(i)
                if (userObject.getString("email") == email &&
                    userObject.getString("password") == password) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    private fun saveUserSession(email: String) {
        // Save current user in SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("current_user_email", email)
        editor.putBoolean("is_logged_in", true)
        editor.putLong("login_time", System.currentTimeMillis())
        editor.apply()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Handle the sign-up button click
    fun onSignUpClick(view: View) {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    // This is no longer needed as we've added an onClick listener to the button
    fun onSignInClick(view: View) {
        signIn()
    }
}