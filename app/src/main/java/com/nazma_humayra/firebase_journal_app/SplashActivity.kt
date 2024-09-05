package com.nazma_humayra.firebase_journal_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Delay for 2 seconds (2000 milliseconds)
        Handler(Looper.getMainLooper()).postDelayed({
            // Start the IntroActivity after the delay
            val intent = Intent(this@SplashActivity, IntroActivity::class.java)
            startActivity(intent)
            finish() // Close the SplashActivity so the user can't go back to it
        }, 2000)
    }
}
