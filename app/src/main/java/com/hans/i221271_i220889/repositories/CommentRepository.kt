package com.hans.i221271_i220889.repositories

import android.content.Context
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.CommentData
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Get comments for a post
     */
    suspend fun getComments(postId: Int): Result<List<CommentData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getComments(
                token = sessionManager.getAuthHeader(),
                postId = postId
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load comments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a comment to a post
     */
    suspend fun addComment(postId: Int, commentText: String): Result<CommentData> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.AddCommentRequest(
                postId = postId,
                commentText = commentText
            )
            val response = ApiClient.apiService.addComment(
                token = sessionManager.getAuthHeader(),
                body = request
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to add comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

