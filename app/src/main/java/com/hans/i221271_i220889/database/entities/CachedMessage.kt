package com.hans.i221271_i220889.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val messageText: String?,
    val mediaUrl: String?,
    val mediaType: String,
    val isVanish: Boolean,
    val isSeen: Boolean,
    val isDeleted: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    val seenAt: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

