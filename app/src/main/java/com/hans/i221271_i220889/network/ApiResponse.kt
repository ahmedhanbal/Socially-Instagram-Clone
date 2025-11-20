package com.hans.i221271_i220889.network

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper matching PHP backend format
 */

data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T?,
    
    @SerializedName("timestamp")
    val timestamp: Long
) {
    fun isSuccess(): Boolean = status == "success"
}

// ==================== REQUEST BODIES ====================
data class SignupRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("full_name")
    val fullName: String? = null,
    
    // Optional base64-encoded avatar image for signup
    @SerializedName("avatar_base64")
    val avatarBase64: String? = null
)

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("fcm_token")
    val fcmToken: String? = null
)

data class UpdateProfileRequest(
    @SerializedName("full_name")
    val fullName: String? = null,
    
    @SerializedName("bio")
    val bio: String? = null,
    
    @SerializedName("is_private")
    val isPrivate: Boolean? = null
)

data class ToggleLikeRequest(
    @SerializedName("post_id")
    val postId: Int
)

data class AddCommentRequest(
    @SerializedName("post_id")
    val postId: Int,
    
    @SerializedName("comment_text")
    val commentText: String
)

data class EditMessageRequest(
    @SerializedName("message_id")
    val messageId: Int,
    
    @SerializedName("message_text")
    val messageText: String
)

data class DeleteMessageRequest(
    @SerializedName("message_id")
    val messageId: Int
)

data class MarkSeenRequest(
    @SerializedName("other_user_id")
    val otherUserId: Int
)

data class SendFollowRequest(
    @SerializedName("target_user_id")
    val targetUserId: Int
)

data class RespondFollowRequest(
    @SerializedName("follower_id")
    val followerId: Int,
    
    @SerializedName("action")
    val action: String
)

data class UnfollowRequest(
    @SerializedName("following_id")
    val followingId: Int
)

data class MarkNotificationReadRequest(
    @SerializedName("notification_id")
    val notificationId: Int
)

data class PushNotificationRequest(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("related_user_id")
    val relatedUserId: Int? = null,
    
    @SerializedName("related_item_id")
    val relatedItemId: Int? = null
)

data class UpdateStatusRequest(
    @SerializedName("is_online")
    val isOnline: Boolean
)

data class UpdateFcmTokenRequest(
    @SerializedName("fcm_token")
    val fcmToken: String
)

data class ReportScreenshotRequest(
    @SerializedName("reported_user_id")
    val reportedUserId: Int,
    
    @SerializedName("chat_context")
    val chatContext: String? = null
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
    
    @SerializedName("like_count")
    val likesCount: Int = 0,
    
    @SerializedName("liked_by_me")
    val isLiked: Boolean = false,
    
    @SerializedName("comment_count")
    val commentsCount: Int = 0,
    
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class FeedResponse(
    @SerializedName("page")
    val page: Int,

    @SerializedName("offset")
    val offset: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("posts")
    val posts: List<PostData> = emptyList()
)

data class StoryListResponse(
    @SerializedName("viewer_id")
    val viewerId: Int,

    @SerializedName("stories")
    val stories: List<StoryData> = emptyList()
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

