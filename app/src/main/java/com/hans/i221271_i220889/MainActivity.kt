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

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            // Create a simple splash screen programmatically
            createSimpleSplashScreen()

            // Splash screen with 1-second delay - check login status
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
            }, 1000) // 1000 ms = 1 second
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            finish()
        }
    }
    
    private fun checkUserAndNavigate() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // User is logged in - check if profile is complete
            android.util.Log.d("MainActivity", "User is logged in, checking profile...")
            
            // Check if user has profile data in Firebase
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUser.uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
                        // If user exists and has username, profile is complete - go to HomeScreen
                        if (user != null && user.username.isNotEmpty()) {
                            android.util.Log.d("MainActivity", "Profile complete - going to HomeScreen")
                            val intent = Intent(this, HomeScreen::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        } else {
                            // Profile incomplete - go to signup (profile setup)
                            android.util.Log.d("MainActivity", "Profile incomplete - going to signup")
                            val intent = Intent(this, signup::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // User doesn't exist in database - go to signup (profile setup)
                        android.util.Log.d("MainActivity", "User not in database - going to signup")
                        val intent = Intent(this, signup::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener {
                    // Error checking profile - go to login
                    android.util.Log.e("MainActivity", "Error checking profile: ${it.message}")
                    val intent = Intent(this, LoginActivity::class.java)
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
