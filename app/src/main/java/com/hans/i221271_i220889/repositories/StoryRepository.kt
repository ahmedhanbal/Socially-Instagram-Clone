package com.hans.i221271_i220889.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.network.StoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class StoryRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Create a new story
     */
    suspend fun createStory(
        mediaUri: Uri,
        mediaType: String // "image" or "video"
    ): Result<StoryData> = withContext(Dispatchers.IO) {
        try {
            // Convert URI to file
            val inputStream = context.contentResolver.openInputStream(mediaUri)
                ?: return@withContext Result.failure(Exception("Cannot open media"))
            
            val extension = if (mediaType == "video") ".mp4" else ".jpg"
            val tempFile = File.createTempFile("story_", extension, context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            // Create multipart request
            val mimeType = if (mediaType == "video") "video/*" else "image/*"
            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val mediaPart = MultipartBody.Part.createFormData("media", tempFile.name, requestBody)
            val typePart = mediaType.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = ApiClient.apiService.createStory(
                token = sessionManager.getAuthHeader(),
                image = mediaPart,
                mediaType = typePart
            )
            
            // Clean up temp file
            tempFile.delete()
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to create story"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all active stories (not expired)
     */
    suspend fun getAllStories(): Result<List<StoryData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listStories(
                token = sessionManager.getAuthHeader(),
                userId = null // Get all stories
            )
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.isSuccess() == true) {
                    val stories = apiResponse.data?.stories ?: emptyList()
                    Result.success(stories)
                } else {
                    Result.failure(Exception(apiResponse?.message ?: "Failed to load stories"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error loading stories", e)
            Result.failure(Exception("Error loading stories: ${e.message}"))
        }
    }
    
    /**
     * Get stories for a specific user
     */
    suspend fun getUserStories(userId: Int): Result<List<StoryData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listStories(
                token = sessionManager.getAuthHeader(),
                userId = userId
            )
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.isSuccess() == true) {
                    val stories = apiResponse.data?.stories ?: emptyList()
                    Result.success(stories)
                } else {
                    Result.failure(Exception(apiResponse?.message ?: "Failed to load user stories"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error loading user stories", e)
            Result.failure(Exception("Error loading user stories: ${e.message}"))
        }
    }
}

