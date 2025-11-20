package com.hans.i221271_i220889.repositories

import android.content.Context
import android.net.Uri
import com.hans.i221271_i220889.network.ApiClient
import com.hans.i221271_i220889.network.MessageData
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class MessageRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    
    /**
     * Send a text message
     */
    suspend fun sendTextMessage(
        receiverId: Int,
        messageText: String,
        isVanish: Boolean = false
    ): Result<MessageData> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.sendMessage(
                token = sessionManager.getAuthHeader(),
                receiverId = receiverId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                messageText = messageText.toRequestBody("text/plain".toMediaTypeOrNull()),
                media = null,
                mediaType = "text".toRequestBody("text/plain".toMediaTypeOrNull()),
                isVanish = if (isVanish) "1" else "0".toRequestBody("text/plain".toMediaTypeOrNull())
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a media message (image/video/file)
     */
    suspend fun sendMediaMessage(
        receiverId: Int,
        mediaUri: Uri,
        mediaType: String, // "image", "video", "file"
        messageText: String? = null,
        isVanish: Boolean = false
    ): Result<MessageData> = withContext(Dispatchers.IO) {
        try {
            // Convert URI to file
            val inputStream = context.contentResolver.openInputStream(mediaUri)
                ?: return@withContext Result.failure(Exception("Cannot open media"))
            
            val extension = when (mediaType) {
                "video" -> ".mp4"
                "image" -> ".jpg"
                else -> ".dat"
            }
            val tempFile = File.createTempFile("message_", extension, context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            // Create multipart request
            val mimeType = when (mediaType) {
                "video" -> "video/*"
                "image" -> "image/*"
                else -> "application/octet-stream"
            }
            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val mediaPart = MultipartBody.Part.createFormData("media", tempFile.name, requestBody)
            
            val response = ApiClient.apiService.sendMessage(
                token = sessionManager.getAuthHeader(),
                receiverId = receiverId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                messageText = messageText?.toRequestBody("text/plain".toMediaTypeOrNull()),
                media = mediaPart,
                mediaType = mediaType.toRequestBody("text/plain".toMediaTypeOrNull()),
                isVanish = (if (isVanish) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())
            )
            
            // Clean up temp file
            tempFile.delete()
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get messages in a conversation
     */
    suspend fun getMessages(otherUserId: Int, page: Int = 1): Result<List<MessageData>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.listMessages(
                token = sessionManager.getAuthHeader(),
                otherUserId = otherUserId,
                page = page
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.success(emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load messages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Edit a message (within 5 minutes)
     */
    suspend fun editMessage(messageId: Int, newText: String): Result<MessageData> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.editMessage(
                token = sessionManager.getAuthHeader(),
                messageId = messageId,
                messageText = newText
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No data returned"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to edit message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.deleteMessage(
                token = sessionManager.getAuthHeader(),
                messageId = messageId
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to delete message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark messages as seen
     */
    suspend fun markMessagesSeen(otherUserId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.markMessagesSeen(
                token = sessionManager.getAuthHeader(),
                otherUserId = otherUserId
            )
            
            if (response.isSuccessful && response.body()?.isSuccess() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to mark messages as seen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

