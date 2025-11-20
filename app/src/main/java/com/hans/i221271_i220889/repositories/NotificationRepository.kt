package com.hans.i221271_i220889.repositories

import android.content.Context
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.NotificationData
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Get notifications for the current user
     */
    suspend fun getNotifications(page: Int = 1, limit: Int = 50): Result<List<NotificationData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listNotifications(
                token = sessionManager.getAuthHeader(),
                page = page,
                limit = limit
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load notifications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.MarkNotificationReadRequest(notificationId = notificationId)
            val response = ApiClient.apiService.markNotificationRead(
                token = sessionManager.getAuthHeader(),
                body = request
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to mark notification as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a push notification event
     */
    suspend fun pushNotificationEvent(
        userId: Int,
        type: String,
        title: String,
        message: String,
        relatedUserId: Int? = null,
        relatedItemId: Int? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.pushNotificationEvent(
                token = sessionManager.getAuthHeader(),
                userId = userId,
                type = type,
                title = title,
                message = message,
                relatedUserId = relatedUserId,
                relatedItemId = relatedItemId
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to send notification"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

