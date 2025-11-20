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
            val request = com.hans.i221271_i220889.network.SendFollowRequest(targetUserId = followingId)
            val response = ApiClient.apiService.sendFollowRequest(
                token = sessionManager.getAuthHeader(),
                body = request
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
     * @param followerId The user ID of the person who sent the follow request
     */
    suspend fun respondToFollowRequest(followerId: Int, accept: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.RespondFollowRequest(
                followerId = followerId,
                action = if (accept) "accept" else "reject"
            )
            val response = ApiClient.apiService.respondFollowRequest(
                token = sessionManager.getAuthHeader(),
                body = request
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
            val request = com.hans.i221271_i220889.network.UnfollowRequest(followingId = followingId)
            val response = ApiClient.apiService.unfollow(
                token = sessionManager.getAuthHeader(),
                body = request
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
    
    /**
     * Check if there's a pending request from current user to target user
     * This checks all outgoing requests from current user
     */
    suspend fun hasPendingRequestTo(targetUserId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = sessionManager.getUserId()
            if (currentUserId == -1) {
                return@withContext Result.failure(Exception("Not logged in"))
            }
            
            // Get all outgoing requests from current user
            // We need to check if there's a pending request to the target user
            // Since list_relations doesn't support outgoing requests, we'll check by getting
            // all requests sent TO the target user and see if current user is in that list
            val response = ApiClient.apiService.listRelations(
                token = sessionManager.getAuthHeader(),
                userId = targetUserId,
                type = "requests"
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                val requests = response.body()?.data ?: emptyList()
                // Check if current user has a pending request to target user
                val hasPending = requests.any { it.followerId == currentUserId && it.status == "pending" }
                Result.success(hasPending)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to check pending request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

