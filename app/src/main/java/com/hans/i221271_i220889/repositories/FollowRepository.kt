package com.hans.i221271_i220889.repositories

import android.content.Context
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.FollowData
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FollowRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Send a follow request to a user
     */
    suspend fun sendFollowRequest(followingId: Int): Result<FollowData> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.sendFollowRequest(
                token = sessionManager.getAuthHeader(),
                followingId = followingId
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to send follow request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Respond to a follow request (accept or reject)
     */
    suspend fun respondToFollowRequest(followId: Int, accept: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.respondFollowRequest(
                token = sessionManager.getAuthHeader(),
                followId = followId,
                action = if (accept) "accept" else "reject"
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to respond to follow request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get followers list for a user
     */
    suspend fun getFollowers(userId: Int): Result<List<FollowData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listRelations(
                token = sessionManager.getAuthHeader(),
                userId = userId,
                type = "followers"
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load followers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get following list for a user
     */
    suspend fun getFollowing(userId: Int): Result<List<FollowData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listRelations(
                token = sessionManager.getAuthHeader(),
                userId = userId,
                type = "following"
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load following"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pending follow requests for current user
     */
    suspend fun getPendingRequests(): Result<List<FollowData>> = withContext(Dispatchers.IO) {
        try {
            val userId = sessionManager.getUserId()
            if (userId == -1) {
                return@withContext Result.failure(Exception("Not logged in"))
            }
            
            val response = ApiClient.apiService.listRelations(
                token = sessionManager.getAuthHeader(),
                userId = userId,
                type = "requests"
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load follow requests"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unfollow a user
     */
    suspend fun unfollow(followingId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.unfollow(
                token = sessionManager.getAuthHeader(),
                followingId = followingId
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to unfollow"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

