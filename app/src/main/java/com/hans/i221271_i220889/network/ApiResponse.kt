package com.hans.i221271_i220889.network

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper matching PHP backend format
 */
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("errors")
    val errors: Map<String, String>? = null
) {
    fun isSuccess(): Boolean = status == "success"
    fun isError(): Boolean = status == "error"
}

// Request bodies
data class SignupRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("full_name")
    val fullName: String? = null
)

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("fcm_token")
    val fcmToken: String? = null
)

// Auth related responses
data class AuthData(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("user")
    val user: UserData
)

data class UserData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("full_name")
    val fullName: String?,
    
    @SerializedName("profile_picture")
    val profilePicture: String?
)

// User profile data
data class UserProfileData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("full_name")
    val fullName: String?,
    
    @SerializedName("bio")
    val bio: String?,
    
    @SerializedName("profile_picture")
    val profilePicture: String?,
    
    @SerializedName("cover_photo")
    val coverPhoto: String?,
    
    @SerializedName("is_private")
    val isPrivate: Boolean,
    
    @SerializedName("followers_count")
    val followersCount: Int = 0,
    
    @SerializedName("following_count")
    val followingCount: Int = 0,
    
    @SerializedName("posts_count")
    val postsCount: Int = 0
)

// Story data
data class StoryData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("profile_picture")
    val profilePicture: String?,
    
    @SerializedName("media_url")
    val mediaUrl: String,
    
    @SerializedName("media_type")
    val mediaType: String,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("expires_at")
    val expiresAt: String
)

// Post data
data class PostData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("profile_picture")
    val profilePicture: String?,
    
    @SerializedName("caption")
    val caption: String?,
    
    @SerializedName("media_url")
    val mediaUrl: String?,
    
    @SerializedName("media_type")
    val mediaType: String?,
    
    @SerializedName("likes_count")
    val likesCount: Int,
    
    @SerializedName("comments_count")
    val commentsCount: Int,
    
    @SerializedName("is_liked")
    val isLiked: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String
)

// Comment data
data class CommentData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("post_id")
    val postId: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("profile_picture")
    val profilePicture: String?,
    
    @SerializedName("comment_text")
    val commentText: String,
    
    @SerializedName("created_at")
    val createdAt: String
)

// Message data
data class MessageData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("sender_id")
    val senderId: Int,
    
    @SerializedName("receiver_id")
    val receiverId: Int,
    
    @SerializedName("message_text")
    val messageText: String?,
    
    @SerializedName("media_url")
    val mediaUrl: String?,
    
    @SerializedName("media_type")
    val mediaType: String,
    
    @SerializedName("is_vanish")
    val isVanish: Boolean,
    
    @SerializedName("is_seen")
    val isSeen: Boolean,
    
    @SerializedName("is_deleted")
    val isDeleted: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String?,
    
    @SerializedName("seen_at")
    val seenAt: String?
)

// Follow data
data class FollowData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("follower_id")
    val followerId: Int,
    
    @SerializedName("following_id")
    val followingId: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("full_name")
    val fullName: String?,
    
    @SerializedName("profile_picture")
    val profilePicture: String?,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("created_at")
    val createdAt: String
)

// Notification data
data class NotificationData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("related_user_id")
    val relatedUserId: Int?,
    
    @SerializedName("related_user_name")
    val relatedUserName: String?,
    
    @SerializedName("related_user_picture")
    val relatedUserPicture: String?,
    
    @SerializedName("related_item_id")
    val relatedItemId: Int?,
    
    @SerializedName("is_read")
    val isRead: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String
)

// User status data
data class UserStatusData(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("is_online")
    val isOnline: Boolean,
    
    @SerializedName("last_seen")
    val lastSeen: String?
)

