package com.hans.i221271_i220889

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hans.i221271_i220889.network.SessionManager

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            // Create a simple splash screen programmatically
            createSimpleSplashScreen()

            // Splash screen with 5-second delay as per requirements - check login status
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    checkUserAndNavigate()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error navigating: ${e.message}", e)
                    e.printStackTrace()
                    // Fallback to login if error occurs
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            }, 5000) // 5000 ms = 5 seconds
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            finish()
        }
    }
    
    private fun checkUserAndNavigate() {
        val sessionManager = SessionManager(this)
        
        if (sessionManager.isLoggedIn()) {
            // User is logged in - check if profile setup is complete
            android.util.Log.d("MainActivity", "User is logged in")
            
            if (sessionManager.isFirstTime()) {
                // First time user - needs profile setup (though signup already does this now)
                android.util.Log.d("MainActivity", "First time user - profile setup needed")
                val intent = Intent(this, signup::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                // Profile complete - go to home screen
                android.util.Log.d("MainActivity", "Profile complete - going to HomeScreen")
                val intent = Intent(this, HomeScreen::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        } else {
            // User is not logged in - go to login
            android.util.Log.d("MainActivity", "User not logged in - going to LoginActivity")
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun createSimpleSplashScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val logo = TextView(this).apply {
            text = "Socially"
            textSize = 48f
            setTextColor(android.graphics.Color.parseColor("#8e3f42"))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "from SMD"
            textSize = 20f
            setTextColor(android.graphics.Color.GRAY)
            gravity = Gravity.CENTER
        }

        layout.addView(logo)
        layout.addView(subtitle)
        setContentView(layout)
    }
}
