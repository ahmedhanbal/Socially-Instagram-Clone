package com.hans.i221271_i220889.repositories

import android.content.Context
import android.net.Uri
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.ApiResponse
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.network.UserProfileData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Get user profile by ID
     */
    suspend fun getProfile(userId: Int): Result<UserProfileData> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getProfile(
                userId = userId,
                token = sessionManager.getAuthHeader()
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update profile information
     */
    suspend fun updateProfile(
        fullName: String?,
        bio: String?,
        isPrivate: Boolean?
    ): Result<UserProfileData> = withContext(Dispatchers.IO) {
        try {
            val request = com.hans.i221271_i220889.network.UpdateProfileRequest(
                fullName = fullName,
                bio = bio,
                isPrivate = isPrivate
            )
            val response = ApiClient.apiService.updateProfile(
                token = sessionManager.getAuthHeader(),
                body = request
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    // Update session with new data
                    fullName?.let { name -> sessionManager.updateFullName(name) }
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to update profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload profile picture or cover photo
     */
    suspend fun uploadPicture(
        imageUri: Uri,
        type: String // "profile_picture" or "cover_photo"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Convert URI to file
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Cannot open image"))
            
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            // Create multipart request
            val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestBody)
            val typePart = type.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = ApiClient.apiService.uploadProfilePicture(
                token = sessionManager.getAuthHeader(),
                type = typePart,
                image = imagePart
            )
            
            // Clean up temp file
            tempFile.delete()
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                val imageUrl = response.body()?.data?.get("image_path")
                if (imageUrl != null) {
                    // Update session if profile picture
                    if (type == "profile_picture") {
                        sessionManager.updateProfilePicture(imageUrl)
                    }
                    Result.success(imageUrl)
                } else {
                    Result.failure(Exception("No image URL returned"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to upload image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current logged-in user's profile
     */
    suspend fun getOwnProfile(): Result<UserProfileData> {
        val userId = sessionManager.getUserId()
        if (userId == -1) {
            return Result.failure(Exception("Not logged in"))
        }
        return getProfile(userId)
    }
}

