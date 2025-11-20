package com.hans.i221271_i220889.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session and authentication token storage
 */
class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "SociallySession"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PROFILE_PICTURE = "profile_picture"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_FIRST_TIME = "is_first_time"
    }
    
    /**
     * Save user session after successful login/signup
     */
    fun saveSession(authData: AuthData) {
        prefs.edit().apply {
            putString(KEY_TOKEN, authData.token)
            putInt(KEY_USER_ID, authData.userId)
            putString(KEY_USERNAME, authData.username)
            putString(KEY_EMAIL, authData.email)
            putString(KEY_FULL_NAME, authData.fullName)
            putString(KEY_PROFILE_PICTURE, authData.profilePicture)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_FIRST_TIME, false)
            apply()
        }
    }
    
    /**
     * Get auth token for API requests
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    /**
     * Get auth token with Bearer prefix for Authorization header
     */
    fun getAuthHeader(): String {
        return "Bearer ${getToken()}"
    }
    
    /**
     * Get current user ID
     */
    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }
    
    /**
     * Get current username
     */
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }
    
    /**
     * Get current user email
     */
    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }
    
    /**
     * Get current user full name
     */
    fun getFullName(): String? {
        return prefs.getString(KEY_FULL_NAME, null)
    }
    
    /**
     * Get current user profile picture
     */
    fun getProfilePicture(): String? {
        return prefs.getString(KEY_PROFILE_PICTURE, null)
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getToken() != null
    }
    
    /**
     * Check if this is first time user (needs profile setup)
     */
    fun isFirstTime(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_TIME, true)
    }
    
    /**
     * Mark profile setup as complete
     */
    fun setProfileSetupComplete() {
        prefs.edit().putBoolean(KEY_IS_FIRST_TIME, false).apply()
    }
    
    /**
     * Update profile picture in session
     */
    fun updateProfilePicture(pictureUrl: String?) {
        prefs.edit().putString(KEY_PROFILE_PICTURE, pictureUrl).apply()
    }
    
    /**
     * Update full name in session
     */
    fun updateFullName(fullName: String?) {
        prefs.edit().putString(KEY_FULL_NAME, fullName).apply()
    }
    
    /**
     * Clear session on logout
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

