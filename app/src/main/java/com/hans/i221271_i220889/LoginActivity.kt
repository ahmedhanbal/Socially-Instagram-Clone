package com.hans.i221271_i220889

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.hans.i221271_i220889.utils.FirebaseAuthManager

class LoginActivity : AppCompatActivity() {
    private lateinit var authManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        // Handle status bar and navigation bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            authManager = FirebaseAuthManager()
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        setupLoginButton()
        setupSignupButton()
        setupForgotPasswordButton()
    }
    
    private fun setupLoginButton() {
        val emailTextBox = findViewById<TextInputEditText>(R.id.emailTextBox)
        val passwordTextBox = findViewById<TextInputEditText>(R.id.passwordTextBox)
        val loginButton = findViewById<Button>(R.id.btnLogin2)
        
        loginButton?.setOnClickListener {
            val email = emailTextBox?.text.toString().trim()
            val password = passwordTextBox?.text.toString().trim()
            
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Try to login with Firebase
            try {
                authManager.signIn(email, password, this) { success, message ->
                    if (success) {
                        // Login successful, go to home screen
                        val intent = Intent(this, HomeScreen::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Login failed, show error message
                        Toast.makeText(this, message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // If Firebase fails, just navigate to home screen for now
                Toast.makeText(this, "Firebase error, proceeding without auth", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeScreen::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
    
    private fun setupSignupButton() {
        val signupButton = findViewById<Button>(R.id.signupBtn)
        signupButton?.setOnClickListener {
            val intent = Intent(this, signup::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupForgotPasswordButton() {
        val forgotPasswordButton = findViewById<Button>(R.id.forgotPassword)
        forgotPasswordButton?.setOnClickListener {
            Toast.makeText(this, "Forgot password functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
