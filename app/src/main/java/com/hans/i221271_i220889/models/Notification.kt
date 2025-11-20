package com.hans.i221271_i220889.models

import java.io.Serializable

data class Notification(
    val notificationId: String = "",
    val type: String = "", // "call", "message", "follow_request", "like"
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromUserProfileImage: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val postId: String = "", // For like notifications
    val channelName: String = "", // For call notifications
    val callType: String = "", // "voice" or "video" for call notifications
    val isRead: Boolean = false
) : Serializable

