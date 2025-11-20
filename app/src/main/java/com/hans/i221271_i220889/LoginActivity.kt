package com.hans.i221271_i220889

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.repositories.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var searchRepository: SearchRepository

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
        
        sessionManager = SessionManager(this)
        searchRepository = SearchRepository(this)
        
        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            // Update status to online
            lifecycleScope.launch {
                searchRepository.updateOnlineStatus(true)
            }
            navigateToHomeScreen()
            return
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
            
            // Login with new PHP API
            loginButton.isEnabled = false
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get FCM token
                    val fcmToken = try {
                        FirebaseMessaging.getInstance().token.await()
                    } catch (e: Exception) {
                        android.util.Log.e("LoginActivity", "Failed to get FCM token: ${e.message}")
                        null
                    }
                    
                    val request = com.hans.i221271_i220889.network.LoginRequest(
                        username = email, // Can be email or username
                        password = password,
                        fcmToken = fcmToken
                    )
                    val response = ApiClient.apiService.login(request)
                    
                    withContext(Dispatchers.Main) {
                        loginButton.isEnabled = true
                        if (response.isSuccessful && response.body()?.isSuccess() == true) {
                            val authData = response.body()?.data
                            if (authData != null) {
                                // Save session
                                sessionManager.saveSession(authData)
                                
                                // Update status to online after login
                                CoroutineScope(Dispatchers.IO).launch {
                                    searchRepository.updateOnlineStatus(true)
                                }
                                
                                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                navigateToHomeScreen()
                            } else {
                                Toast.makeText(this@LoginActivity, "Login failed: Invalid response", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMsg = response.body()?.message ?: "Login failed"
                            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        loginButton.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun navigateToHomeScreen() {
        val intent = Intent(this, HomeScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
            Toast.makeText(this, "remember your password dummbo", Toast.LENGTH_SHORT).show()
        }
    }
}
