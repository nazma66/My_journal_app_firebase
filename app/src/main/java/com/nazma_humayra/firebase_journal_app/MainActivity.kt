package com.nazma_humayra.firebase_journal_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.nazma_humayra.firebase_journal_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Load the saved email from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val lastLoggedInEmail = sharedPreferences.getString("lastLoggedInEmail", "")

        // Set up the AutoCompleteTextView with the saved email
        val emailSuggestions = arrayOf(lastLoggedInEmail).filter { it?.isNotEmpty() == true }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, emailSuggestions)
        binding.email.setAdapter(adapter)

        // Set the adapter threshold to 1 so that it shows suggestions as soon as the user starts typing
        binding.email.threshold = 1

        // Rest of your code...
        binding.text.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.forgotPassword.setOnClickListener {
            showResetPasswordUI()
        }

        binding.saveNewPasswordButton.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val newPassword = binding.newPassword.text.toString().trim()
            if (email.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Email and new password must not be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resetPassword(email)
        }

        binding.emailSignInButton.setOnClickListener {
            LoginWithEmailPassword(
                binding.email.text.toString().trim(),
                binding.password.text.toString().trim()
            )
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }


    private fun showResetPasswordUI() {
        binding.newPassword.visibility = View.VISIBLE
        binding.saveNewPasswordButton.visibility = View.VISIBLE
        binding.emailSignInButton.visibility = View.GONE
        binding.forgotPassword.visibility = View.GONE
        binding.text.visibility = View.GONE
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent. Please check your inbox.", Toast.LENGTH_SHORT).show()
                    // Reset UI to login view
                    binding.newPassword.visibility = View.GONE
                    binding.saveNewPasswordButton.visibility = View.GONE
                    binding.emailSignInButton.visibility = View.VISIBLE
                    binding.forgotPassword.visibility = View.VISIBLE
                    binding.text.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Failed to send reset email. Please check the email address.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun LoginWithEmailPassword(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password must not be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save the email to shared preferences
                    val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    sharedPreferences.edit().putString("lastLoggedInEmail", email).apply()

                    // Sign in Success
                    goToJournalList()
                } else {
                    Toast.makeText(this, "Authentication Failed: Wrong password or email.", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            goToJournalList()
        }
    }

    private fun goToJournalList() {
        val intent = Intent(this, JournalList::class.java)
        startActivity(intent)
        finish() // Close the current activity to prevent going back to it
    }
}
