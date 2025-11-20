package com.hans.i221271_i220889.models

import java.io.Serializable

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "", // Base64 encoded profile image
    val imageUrl: String = "", // Base64 encoded story image
    val videoUrl: String = "", // Base64 encoded video (future support)
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours
    val viewers: MutableList<String> = mutableListOf(),
    val isViewed: Boolean = false
) : Serializable
