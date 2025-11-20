package com.hans.i221271_i220889.utils

import java.io.Serializable

/**
 * Data class representing a chat message
 */
data class ChatMessage(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val type: String, // "text", "image", "video", "file", "post"
    val content: String, // message text or base64 image
    val mediaUrl: String? = null, // URL to media file
    val mediaType: String? = null, // "image", "video", "file"
    val timestamp: Long,
    val editableUntil: Long = timestamp + (15 * 60 * 1000), // 15 minutes to edit
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) : Serializable

