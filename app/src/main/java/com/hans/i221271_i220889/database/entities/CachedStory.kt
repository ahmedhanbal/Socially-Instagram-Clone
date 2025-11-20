package com.hans.i221271_i220889.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_stories")
data class CachedStory(
    @PrimaryKey
    val id: Int,
    val userId: Int,
    val username: String,
    val profilePicture: String?,
    val mediaUrl: String,
    val mediaType: String,
    val createdAt: String,
    val expiresAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

