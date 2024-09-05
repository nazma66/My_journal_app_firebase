package com.nazma_humayra.firebase_journal_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.nazma_humayra.firebase_journal_app.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Set the content view after setting up the window
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        auth = Firebase.auth

        binding.accSignUpButton.setOnClickListener {
            createUser()
        }

        binding.tvLogin.setOnClickListener {
            Log.d(TAG, "Log In text clicked. Starting MainActivity.")
            val intent = Intent(this@SignUpActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createUser() {
        val email = binding.emailCreate.text.toString().trim()
        val password = binding.passwordCreate.text.toString().trim()
        val username = binding.userNameCreateET.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "All fields must be filled.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (userId != null) {
                        val userMap = mapOf("username" to username)
                        FirebaseFirestore.getInstance().collection("Users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Log.d(TAG, "User profile created.")
                                updateUI(user)
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error saving user profile.", e)
                                updateUI(null)
                            }
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
            // Clear input fields
            binding.emailCreate.text.clear()
            binding.passwordCreate.text.clear()
            binding.userNameCreateET.text.clear()

            // Navigate to Login page
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finish SignUpActivity so that the user can't go back to it
        } else {
            Toast.makeText(this, "Sign up failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUsername(userId: String, callback: (String?) -> Unit) {
        FirebaseFirestore.getInstance().collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username")
                callback(username)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to retrieve username.", e)
                callback(null)
            }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun reload() {
        // Optionally, reload user information or handle user session
    }

    companion object {
        private const val TAG = "SignUpActivity"
    }
}
