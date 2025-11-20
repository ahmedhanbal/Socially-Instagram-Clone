package com.hans.i221271_i220889.utils

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable

data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val type: String = "text", // text | image | post
    val content: String = "",   // text or imageUrl or postId
    val timestamp: Long = System.currentTimeMillis(),
    val editableUntil: Long = timestamp + 5 * 60 * 1000
) : Serializable

class ChatRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun sendText(chatId: String, text: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        val messageId = db.child("messages").child(chatId).push().key ?: return onComplete(false)
        val payload = ChatMessage(
            messageId = messageId,
            chatId = chatId,
            senderId = uid,
            type = "text",
            content = text
        )
        db.child("messages").child(chatId).child(messageId).setValue(payload)
            .addOnSuccessListener {
                // Send FCM notification to recipient
                sendMessageNotification(chatId, uid, text, "text")
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun sendImage(context: Context, chatId: String, imageUri: Uri, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        
        // Convert image to Base64
        val base64Image = Base64Image.uriToBase64(context, imageUri)
        if (base64Image == null) {
            onComplete(false)
            return
        }
        
        val messageId = db.child("messages").child(chatId).push().key ?: return onComplete(false)
        val payload = ChatMessage(
            messageId = messageId,
            chatId = chatId,
            senderId = uid,
            type = "image",
            content = base64Image // Store Base64 string instead of URL
        )
        db.child("messages").child(chatId).child(messageId).setValue(payload)
            .addOnSuccessListener {
                // Send FCM notification to recipient
                sendMessageNotification(chatId, uid, "Sent an image", "image")
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun sendPost(chatId: String, postId: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        val messageId = db.child("messages").child(chatId).push().key ?: return onComplete(false)
        val payload = ChatMessage(
            messageId = messageId,
            chatId = chatId,
            senderId = uid,
            type = "post",
            content = postId
        )
        db.child("messages").child(chatId).child(messageId).setValue(payload)
            .addOnSuccessListener {
                // Send FCM notification to recipient
                sendMessageNotification(chatId, uid, "Shared a post", "post")
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun editMessage(chatId: String, messageId: String, newText: String, onComplete: (Boolean) -> Unit) {
        db.child("messages").child(chatId).child(messageId).get().addOnSuccessListener { snap ->
            val msg = snap.getValue(ChatMessage::class.java) ?: return@addOnSuccessListener
            if (System.currentTimeMillis() <= msg.editableUntil && msg.type == "text") {
                db.child("messages").child(chatId).child(messageId).child("content").setValue(newText)
                    .addOnCompleteListener { onComplete(it.isSuccessful) }
            } else {
                onComplete(false)
            }
        }.addOnFailureListener { onComplete(false) }
    }

    fun deleteMessage(chatId: String, messageId: String, onComplete: (Boolean) -> Unit) {
        db.child("messages").child(chatId).child(messageId).get().addOnSuccessListener { snap ->
            val msg = snap.getValue(ChatMessage::class.java) ?: return@addOnSuccessListener
            if (System.currentTimeMillis() <= msg.editableUntil) {
                db.child("messages").child(chatId).child(messageId).removeValue()
                    .addOnCompleteListener { onComplete(it.isSuccessful) }
            } else {
                onComplete(false)
            }
        }.addOnFailureListener { onComplete(false) }
    }
    
    /**
     * Send FCM notification to the recipient when a message is sent
     */
    private fun sendMessageNotification(chatId: String, senderId: String, messageContent: String, messageType: String) {
        try {
            // Extract recipient ID from chatId (format: userId1_userId2)
            val userIds = chatId.split("_")
            val recipientId = if (userIds[0] == senderId) userIds[1] else userIds[0]
            
            // Get sender's username and profile image
            db.child("users").child(senderId).get()
                .addOnSuccessListener { userSnapshot ->
                    val user = userSnapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
                    val senderUsername = user?.username ?: "Someone"
                    val senderProfileImage = user?.profileImageBase64 ?: ""
                    
                    // Save notification to database
                    val notificationId = db.child("notifications").child(recipientId).push().key ?: return@addOnSuccessListener
                    val notificationData = mapOf(
                        "notificationId" to notificationId,
                        "type" to "message",
                        "fromUserId" to senderId,
                        "fromUsername" to senderUsername,
                        "fromUserProfileImage" to senderProfileImage,
                        "title" to senderUsername,
                        "body" to if (messageType == "text") messageContent else "Sent a $messageType",
                        "timestamp" to System.currentTimeMillis(),
                        "isRead" to false
                    )
                    
                    db.child("notifications").child(recipientId).child(notificationId).setValue(notificationData)
                    
                    // Send FCM push notification
                    com.hans.i221271_i220889.services.MyFirebaseMessagingService.sendNotificationToUser(
                        recipientId,
                        senderUsername,
                        if (messageType == "text") messageContent else "Sent a $messageType",
                        "new_message",
                        senderId,
                        senderUsername
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error sending notification: ${e.message}", e)
        }
    }
}


