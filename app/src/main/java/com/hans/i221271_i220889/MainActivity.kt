package com.hans.i221271_i220889

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import com.hans.i221271_i220889.network.SessionManager

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Use XML-based splash screen layout
            setContentView(R.layout.activity_main)

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
}