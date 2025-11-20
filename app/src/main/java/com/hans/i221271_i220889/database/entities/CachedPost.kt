package com.hans.i221271_i220889.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_posts")
data class CachedPost(
    @PrimaryKey
    val id: Int,
    val userId: Int,
    val username: String,
    val profilePicture: String?,
    val caption: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

