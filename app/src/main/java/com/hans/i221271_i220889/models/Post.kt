package com.hans.i221271_i220889.models

import java.io.Serializable

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "", // Profile picture URL from backend (can be path or base64)
    val userProfileImageBase64: String = "", // Backward compatibility
    val imageUrl: String = "", // Post image URL from backend
    val imageBase64: String = "", // Backward compatibility for base64 images
    val videoBase64: String = "", // Backward compatibility for base64 videos
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: MutableList<String> = mutableListOf(), // Backward compatibility
    val likesCount: Int = 0, // Like count from backend
    val commentsCount: Int = 0, // Comment count from backend
    val likeCount: Int = 0, // Backward compatibility
    val commentCount: Int = 0, // Backward compatibility
    val isLikedByCurrentUser: Boolean = false // Whether current user has liked this post
) : Serializable

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val username: String = "",
    val profilePicture: String = "", // Profile picture URL from backend
    val userProfileImage: String = "", // Backward compatibility
    val text: String = "",
    val commentText: String = "", // From backend API
    val timestamp: String = System.currentTimeMillis().toString(),
    val likes: Int = 0
) : Serializable
