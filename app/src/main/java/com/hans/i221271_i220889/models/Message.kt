package com.hans.i221271_i220889.models

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val canEdit: Boolean = true // Can edit within 5 minutes
)

data class Chat(
    val chatId: String = "",
    val participants: MutableList<String> = mutableListOf(),
    val lastMessage: Message? = null,
    val lastMessageTime: Long = System.currentTimeMillis()
)
