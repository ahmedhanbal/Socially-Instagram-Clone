package com.hans.i221271_i220889.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.hans.i221271_i220889.LoginActivity
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AuthHelper {
    
    /**
     * Logout user - clear session and navigate to login
     */
    fun logout(context: Context, showToast: Boolean = true) {
        val sessionManager = SessionManager(context)
        
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin(context)
            return
        }
        
        // Call logout API in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = sessionManager.getAuthHeader()
                ApiClient.apiService.logout(token)
                
                withContext(Dispatchers.Main) {
                    // Clear local session
                    sessionManager.clearSession()
                    
                    if (showToast) {
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    }
                    
                    navigateToLogin(context)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Even if API call fails, clear local session
                    sessionManager.clearSession()
                    
                    if (showToast) {
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    }
                    
                    navigateToLogin(context)
                }
            }
        }
    }
    
    /**
     * Navigate to login screen with flags to clear task
     */
    private fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
    
    /**
     * Check if user is authenticated and has valid session
     */
    fun isAuthenticated(context: Context): Boolean {
        val sessionManager = SessionManager(context)
        return sessionManager.isLoggedIn()
    }
    
    /**
     * Get current user ID or -1 if not logged in
     */
    fun getCurrentUserId(context: Context): Int {
        val sessionManager = SessionManager(context)
        return sessionManager.getUserId()
    }
    
    /**
     * Get auth header for API requests
     */
    fun getAuthHeader(context: Context): String {
        val sessionManager = SessionManager(context)
        return sessionManager.getAuthHeader()
    }
}

