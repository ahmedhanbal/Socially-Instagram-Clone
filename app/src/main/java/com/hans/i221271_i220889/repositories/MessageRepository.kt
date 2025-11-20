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
import android.provider.OpenableColumns
import android.database.Cursor

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
                isVanish = (if (isVanish) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess() == true) {
                    body.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No data returned"))
                } else {
                    val errorMsg = body?.message ?: response.errorBody()?.string() ?: "Failed to send message"
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Failed to send message: $errorBody"))
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            // Handle JSON parsing errors
            val errorMsg = "Invalid response format: ${e.message}"
            android.util.Log.e("MessageRepository", errorMsg, e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            android.util.Log.e("MessageRepository", "Error sending text message: ${e.message}", e)
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
            
            // Get original filename if available
            val originalFileName = getFileName(mediaUri) ?: "file"
            val extension = originalFileName.substringAfterLast('.', "")
            
            val tempFile = File.createTempFile("message_", if (extension.isNotEmpty()) ".$extension" else ".dat", context.cacheDir)
            
            // Check file size (max 2MB)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            val fileSize = tempFile.length()
            val maxSize = 2 * 1024 * 1024L // 2MB
            if (fileSize > maxSize) {
                tempFile.delete()
                return@withContext Result.failure(Exception("File size exceeds 2MB limit"))
            }
            
            // Determine MIME type
            val mimeType = when (mediaType) {
                "video" -> "video/mp4"
                "image" -> context.contentResolver.getType(mediaUri) ?: "image/*"
                "file" -> context.contentResolver.getType(mediaUri) ?: "application/octet-stream"
                else -> "application/octet-stream"
            }
            
            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val mediaPart = MultipartBody.Part.createFormData("media", originalFileName, requestBody)
            
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
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess() == true) {
                    body.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No data returned"))
                } else {
                    val errorMsg = body?.message ?: response.errorBody()?.string() ?: "Failed to send message"
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Failed to send message: $errorBody"))
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            // Handle JSON parsing errors
            val errorMsg = "Invalid response format: ${e.message}"
            android.util.Log.e("MessageRepository", errorMsg, e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            android.util.Log.e("MessageRepository", "Error sending media: ${e.message}", e)
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
            val request = com.hans.i221271_i220889.network.EditMessageRequest(
                messageId = messageId,
                messageText = newText
            )
            val response = ApiClient.apiService.editMessage(
                token = sessionManager.getAuthHeader(),
                body = request
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
            val request = com.hans.i221271_i220889.network.DeleteMessageRequest(messageId = messageId)
            val response = ApiClient.apiService.deleteMessage(
                token = sessionManager.getAuthHeader(),
                body = request
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
            val request = com.hans.i221271_i220889.network.MarkSeenRequest(otherUserId = otherUserId)
            val response = ApiClient.apiService.markMessagesSeen(
                token = sessionManager.getAuthHeader(),
                body = request
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
    
    /**
     * Get filename from URI
     */
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }
}

