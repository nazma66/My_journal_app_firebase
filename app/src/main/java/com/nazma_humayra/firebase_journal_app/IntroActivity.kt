package com.nazma_humayra.firebase_journal_app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import androidx.core.view.WindowCompat

class IntroActivity : AppCompatActivity() {

    private val TAG = "IntroActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        // Adjust the system UI to fit the layout with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        // Ensure the status bar text color is white (light)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Clear the SYSTEM_UI_FLAG_LIGHT_STATUS_BAR flag to make the status bar text light
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // Find the views from the layout
        val btnGetStarted = findViewById<MaterialButton>(R.id.btnGetStarted)
        val tvLogin = findViewById<MaterialButton>(R.id.tvLogin)

        // Set onClickListener for the Get Started button
        btnGetStarted.setOnClickListener {
            Log.d(TAG, "Get Started button clicked. Starting SignUpActivity.")
            val intent = Intent(this@IntroActivity, SignUpActivity::class.java)
            startActivity(intent)
            finish() // Finish the current activity
        }

        // Set onClickListener for the Login text
        tvLogin.setOnClickListener {
            Log.d(TAG, "Log In text clicked. Starting MainActivity.")
            val intent = Intent(this@IntroActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
