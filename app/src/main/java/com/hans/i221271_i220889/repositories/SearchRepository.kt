package com.hans.i221271_i220889.repositories

import android.content.Context
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.NotificationData
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.network.UserProfileData
import com.hans.i221271_i220889.network.UserStatusData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Search users by username or name
     */
    suspend fun searchUsers(
        query: String,
        filter: String? = null // "followers", "following", or null for all
    ): Result<List<UserProfileData>> = withContext(Dispatchers.IO) {
        try {
            val sanitizedFilter = filter?.takeIf { it == "followers" || it == "following" }
            val response = ApiClient.apiService.searchUsers(
                token = sessionManager.getAuthHeader(),
                query = query,
                filter = sanitizedFilter
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get notifications for current user
     */
    suspend fun getNotifications(page: Int = 1): Result<List<NotificationData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listNotifications(
                token = sessionManager.getAuthHeader(),
                page = page
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
    suspend fun markNotificationRead(notificationId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
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
     * Update online status
     */
    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.UpdateStatusRequest(isOnline = isOnline)
            val response = ApiClient.apiService.updateStatus(
                token = sessionManager.getAuthHeader(),
                body = request
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to update status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get online status for multiple users
     */
    suspend fun getUsersStatus(userIds: List<Int>): Result<List<UserStatusData>> = withContext(Dispatchers.IO) {
        try {
            val userIdsString = userIds.joinToString(",")
            val response = ApiClient.apiService.getStatus(
                token = sessionManager.getAuthHeader(),
                userIds = userIdsString
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get user status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Report screenshot in chat
     */
    suspend fun reportScreenshot(reportedUserId: Int, chatContext: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.ReportScreenshotRequest(
                reportedUserId = reportedUserId,
                chatContext = chatContext
            )
            val response = ApiClient.apiService.reportScreenshot(
                token = sessionManager.getAuthHeader(),
                body = request
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to report screenshot"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

