package com.hans.i221271_i220889.models

import java.io.Serializable

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "", // Can store Base64 or URL
    val profileImageBase64: String = "", // Base64 encoded profile image for Firebase free plan
    val isOnline: Boolean = false
    // Note: followers, following, and stories are stored separately in Firebase:
    // - followers: /followers/{userId}/{followerId}
    // - following: /following/{userId}/{followingId}
    // - stories: /stories/{storyId} (with userId field)
) : Serializable
