package com.hans.i221271_i220889.repositories

import android.content.Context
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FcmRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Update FCM token on the server
     */
    suspend fun updateFcmToken(fcmToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.UpdateFcmTokenRequest(fcmToken = fcmToken)
            val response = ApiClient.apiService.updateFcmToken(
                token = sessionManager.getAuthHeader(),
                body = request
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to update FCM token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

