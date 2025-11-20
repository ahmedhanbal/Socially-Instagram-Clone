package com.hans.i221271_i220889.repositories

import android.content.Context
import android.net.Uri
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.CommentData
import com.hans.i221271_i220889.network.PostData
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class PostRepositoryApi(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Create a new post
     */
    suspend fun createPost(
        caption: String?,
        mediaUri: Uri?,
        mediaType: String? // "image" or "video"
    ): Result<PostData> = withContext(Dispatchers.IO) {
        try {
            var tempFile: File? = null
            val mediaPart: MultipartBody.Part? = if (mediaUri != null && mediaType != null) {
                // Convert URI to file
                val inputStream = context.contentResolver.openInputStream(mediaUri)
                    ?: return@withContext Result.failure(Exception("Cannot open media"))
                
                val extension = if (mediaType == "video") ".mp4" else ".jpg"
                tempFile = File.createTempFile("post_", extension, context.cacheDir)
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                
                val mimeType = if (mediaType == "video") "video/*" else "image/*"
                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("media", tempFile.name, requestBody)
            } else {
                null
            }
            
            val captionPart = caption?.toRequestBody("text/plain".toMediaTypeOrNull())
            val typePart = mediaType?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = ApiClient.apiService.createPost(
                token = sessionManager.getAuthHeader(),
                caption = captionPart,
                image = mediaPart,
                mediaType = typePart
            )
            
            // Clean up temp file
            tempFile?.delete()
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to create post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get feed posts
     */
    suspend fun getFeed(page: Int = 1, limit: Int = 20): Result<List<PostData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listFeed(
                token = sessionManager.getAuthHeader(),
                page = page,
                limit = limit
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load feed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get posts for a specific user
     */
    suspend fun getUserPosts(userId: Int, page: Int = 1, limit: Int = 20): Result<List<PostData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getUserPosts(
                token = sessionManager.getAuthHeader(),
                userId = userId,
                page = page,
                limit = limit
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load user posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Toggle like on a post
     */
    suspend fun toggleLike(postId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.ToggleLikeRequest(postId = postId)
            val response = ApiClient.apiService.toggleLike(
                token = sessionManager.getAuthHeader(),
                body = request
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                val status = response.body()?.data?.get("status") as? String
                val isLiked = status == "liked"
                Result.success(isLiked)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to toggle like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
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
            val response = ApiClient.apiService.addComment(
                token = sessionManager.getAuthHeader(),
                postId = postId,
                commentText = commentText
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

