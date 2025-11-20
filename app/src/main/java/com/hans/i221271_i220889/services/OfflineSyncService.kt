package com.hans.i221271_i220889.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hans.i221271_i220889.database.entities.PendingAction
import com.hans.i221271_i220889.offline.NetworkHelper
import com.hans.i221271_i220889.offline.OfflineQueueManager
import com.hans.i221271_i220889.repositories.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Service to sync pending offline actions when device comes online
 */
class OfflineSyncService : Service() {
    
    companion object {
        private const val TAG = "OfflineSyncService"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    
    private lateinit var queueManager: OfflineQueueManager
    private lateinit var messageRepository: MessageRepository
    private lateinit var postRepository: PostRepositoryApi
    private lateinit var storyRepository: StoryRepository
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OfflineSyncService created")
        
        queueManager = OfflineQueueManager(this)
        messageRepository = MessageRepository(this)
        postRepository = PostRepositoryApi(this)
        storyRepository = StoryRepository(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OfflineSyncService started")
        
        // Check if online
        if (!NetworkHelper.isOnline(this)) {
            Log.d(TAG, "Device is offline. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Process queue in background
        serviceScope.launch {
            try {
                processPendingActions()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing pending actions: ${e.message}", e)
            } finally {
                // Clean up and stop service
                queueManager.cleanup()
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "OfflineSyncService destroyed")
    }
    
    private suspend fun processPendingActions() {
        val pendingActions = queueManager.getAllPendingActions()
        Log.d(TAG, "Processing ${pendingActions.size} pending actions")
        
        for (action in pendingActions) {
            // Check if we're still online
            if (!NetworkHelper.isOnline(this)) {
                Log.d(TAG, "Lost connection. Stopping sync.")
                break
            }
            
            // Check retry limit
            if (action.retryCount >= action.maxRetries) {
                Log.w(TAG, "Action ${action.id} exceeded max retries. Marking as failed.")
                queueManager.markActionFailed(action.id)
                continue
            }
            
            // Process action based on type
            val success = when (action.actionType) {
                "SEND_MESSAGE" -> processSendMessage(action)
                "CREATE_POST" -> processCreatePost(action)
                "CREATE_STORY" -> processCreateStory(action)
                "LIKE_POST" -> processLikePost(action)
                "ADD_COMMENT" -> processAddComment(action)
                "EDIT_MESSAGE" -> processEditMessage(action)
                "DELETE_MESSAGE" -> processDeleteMessage(action)
                else -> {
                    Log.w(TAG, "Unknown action type: ${action.actionType}")
                    false
                }
            }
            
            if (success) {
                Log.d(TAG, "Action ${action.id} (${action.actionType}) completed successfully")
                queueManager.markActionCompleted(action.id)
            } else {
                Log.w(TAG, "Action ${action.id} (${action.actionType}) failed. Retry ${action.retryCount + 1}/${action.maxRetries}")
                queueManager.incrementRetryCount(action.id)
            }
        }
        
        Log.d(TAG, "Finished processing pending actions")
    }
    
    private suspend fun processSendMessage(action: PendingAction): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val payload: Map<String, Any> = gson.fromJson(action.payload, mapType)
            
            val receiverId = (payload["receiver_id"] as Double).toInt()
            val messageText = payload["message_text"] as? String
            val mediaPath = payload["media_path"] as? String
            val mediaType = payload["media_type"] as String
            val isVanish = payload["is_vanish"] as Boolean
            
            if (mediaPath != null && File(mediaPath).exists()) {
                val uri = Uri.fromFile(File(mediaPath))
                messageRepository.sendMediaMessage(receiverId, uri, mediaType, messageText, isVanish).isSuccess
            } else {
                messageRepository.sendTextMessage(receiverId, messageText ?: "", isVanish).isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            false
        }
    }
    
    private suspend fun processCreatePost(action: PendingAction): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val payload: Map<String, Any?> = gson.fromJson(action.payload, mapType)
            
            val caption = payload["caption"] as? String
            val mediaPath = payload["media_path"] as? String
            val mediaType = payload["media_type"] as? String
            
            val mediaUri = if (mediaPath != null && File(mediaPath).exists()) {
                Uri.fromFile(File(mediaPath))
            } else null
            
            postRepository.createPost(caption, mediaUri, mediaType).isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post: ${e.message}", e)
            false
        }
    }
    
    private suspend fun processCreateStory(action: PendingAction): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            val payload: Map<String, String> = gson.fromJson(action.payload, mapType)
            
            val mediaPath = payload["media_path"] ?: return false
            val mediaType = payload["media_type"] ?: return false
            
            if (!File(mediaPath).exists()) {
                Log.w(TAG, "Story media file not found: $mediaPath")
                return false
            }
            
            val uri = Uri.fromFile(File(mediaPath))
            storyRepository.createStory(uri, mediaType).isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error creating story: ${e.message}", e)
            false
        }
    }
    
    private suspend fun processLikePost(action: PendingAction): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val payload: Map<String, Any> = gson.fromJson(action.payload, mapType)
            
            val postId = (payload["post_id"] as Double).toInt()
            postRepository.toggleLike(postId).isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error liking post: ${e.message}", e)
            false
        }
    }
    
    private suspend fun processAddComment(action: PendingAction): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val payload: Map<String, Any> = gson.fromJson(action.payload, mapType)
            
            val postId = (payload["post_id"] as Double).toInt()
            val commentText = payload["comment_text"] as String
            
            postRepository.addComment(postId, commentText).isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment: ${e.message}", e)
            false
        }
    }
    
    private suspend fun processEditMessage(action: PendingAction): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val payload: Map<String, Any> = gson.fromJson(action.payload, mapType)
            
            val messageId = (payload["message_id"] as Double).toInt()
            val messageText = payload["message_text"] as String
            
            messageRepository.editMessage(messageId, messageText).isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message: ${e.message}", e)
            false
        }
    }
    
    private suspend fun processDeleteMessage(action: PendingAction): Boolean {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val payload: Map<String, Any> = gson.fromJson(action.payload, mapType)
            
            val messageId = (payload["message_id"] as Double).toInt()
            messageRepository.deleteMessage(messageId).isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: ${e.message}", e)
            false
        }
    }
}

