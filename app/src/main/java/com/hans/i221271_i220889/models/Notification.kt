package com.hans.i221271_i220889.models

import java.io.Serializable

data class Notification(
    val notificationId: String = "",
    val type: String = "", // "call", "message", "follow_request", "like"
    val fromUserId: String = "",
    val fromUsername: String = "",
    val profilePicture: String = "", // Profile picture URL from backend
    val title: String = "",
    val message: String = "", // Message text
    val body: String = "", // Backward compatibility
    val timestamp: String = System.currentTimeMillis().toString(),
    val postId: String = "", // For like notifications
    val channelName: String = "", // For call notifications
    val callType: String = "", // "voice" or "video" for call notifications
    var isRead: Boolean = false
) : Serializable

