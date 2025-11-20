package com.hans.i221271_i220889.models

import java.io.Serializable

/**
 * User model for the Socially app
 * Used for displaying user information in various screens
 */
data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    val bio: String = "",
    val profilePicture: String = "", // URL or path to profile picture
    val profileImageUrl: String = "", // Backward compatibility - Can store Base64 or URL
    val profileImageBase64: String = "", // Backward compatibility - Base64 encoded profile image
    val coverPhoto: String = "",
    val isPrivate: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isOnline: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0
) : Serializable
