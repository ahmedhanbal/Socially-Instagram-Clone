package com.hans.i221271_i220889.offline

import android.content.Context
import com.google.gson.Gson
import com.hans.i221271_i220889.database.SociallyDatabase
import com.hans.i221271_i220889.database.entities.PendingAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages offline queue for actions that failed due to network issues
 */
class OfflineQueueManager(private val context: Context) {
    
    private val database = SociallyDatabase.getDatabase(context)
    private val pendingActionDao = database.pendingActionDao()
    private val gson = Gson()
    
    /**
     * Queue a message to be sent when online
     */
    suspend fun queueSendMessage(
        receiverId: Int,
        messageText: String?,
        mediaPath: String?,
        mediaType: String,
        isVanish: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val payload = gson.toJson(
            mapOf(
                "receiver_id" to receiverId,
                "message_text" to messageText,
                "media_path" to mediaPath,
                "media_type" to mediaType,
                "is_vanish" to isVanish
            )
        )
        
        val action = PendingAction(
            actionType = "SEND_MESSAGE",
            payload = payload
        )
        
        pendingActionDao.insertAction(action)
    }
    
    /**
     * Queue a post creation
     */
    suspend fun queueCreatePost(
        caption: String?,
        mediaPath: String?,
        mediaType: String?
    ): Long = withContext(Dispatchers.IO) {
        val payload = gson.toJson(
            mapOf(
                "caption" to caption,
                "media_path" to mediaPath,
                "media_type" to mediaType
            )
        )
        
        val action = PendingAction(
            actionType = "CREATE_POST",
            payload = payload
        )
        
        pendingActionDao.insertAction(action)
    }
    
    /**
     * Queue a story creation
     */
    suspend fun queueCreateStory(
        mediaPath: String,
        mediaType: String
    ): Long = withContext(Dispatchers.IO) {
        val payload = gson.toJson(
            mapOf(
                "media_path" to mediaPath,
                "media_type" to mediaType
            )
        )
        
        val action = PendingAction(
            actionType = "CREATE_STORY",
            payload = payload
        )
        
        pendingActionDao.insertAction(action)
    }
    
    /**
     * Queue a like action
     */
    suspend fun queueLikePost(postId: Int): Long = withContext(Dispatchers.IO) {
        val payload = gson.toJson(mapOf("post_id" to postId))
        
        val action = PendingAction(
            actionType = "LIKE_POST",
            payload = payload
        )
        
        pendingActionDao.insertAction(action)
    }
    
    /**
     * Queue a comment
     */
    suspend fun queueAddComment(
        postId: Int,
        commentText: String
    ): Long = withContext(Dispatchers.IO) {
        val payload = gson.toJson(
            mapOf(
                "post_id" to postId,
                "comment_text" to commentText
            )
        )
        
        val action = PendingAction(
            actionType = "ADD_COMMENT",
            payload = payload
        )
        
        pendingActionDao.insertAction(action)
    }
    
    /**
     * Queue message edit
     */
    suspend fun queueEditMessage(
        messageId: Int,
        newText: String
    ): Long = withContext(Dispatchers.IO) {
        val payload = gson.toJson(
            mapOf(
                "message_id" to messageId,
                "message_text" to newText
            )
        )
        
        val action = PendingAction(
            actionType = "EDIT_MESSAGE",
            payload = payload
        )
        
        pendingActionDao.insertAction(action)
    }
    
    /**
     * Queue message deletion
     */
    suspend fun queueDeleteMessage(messageId: Int): Long = withContext(Dispatchers.IO) {
        val payload = gson.toJson(mapOf("message_id" to messageId))
        
        val action = PendingAction(
            actionType = "DELETE_MESSAGE",
            payload = payload
        )
        
        pendingActionDao.insertAction(action)
    }
    
    /**
     * Get all pending actions
     */
    suspend fun getAllPendingActions(): List<PendingAction> = withContext(Dispatchers.IO) {
        pendingActionDao.getAllPendingActions()
    }
    
    /**
     * Mark action as completed
     */
    suspend fun markActionCompleted(actionId: Long) = withContext(Dispatchers.IO) {
        pendingActionDao.updateActionStatus(actionId, "COMPLETED")
    }
    
    /**
     * Mark action as failed
     */
    suspend fun markActionFailed(actionId: Long) = withContext(Dispatchers.IO) {
        pendingActionDao.updateActionStatus(actionId, "FAILED")
    }
    
    /**
     * Increment retry count
     */
    suspend fun incrementRetryCount(actionId: Long) = withContext(Dispatchers.IO) {
        pendingActionDao.incrementRetryCount(actionId)
    }
    
    /**
     * Clean up completed and permanently failed actions
     */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        pendingActionDao.deleteCompletedActions()
        pendingActionDao.deleteFailedActions()
    }
}

