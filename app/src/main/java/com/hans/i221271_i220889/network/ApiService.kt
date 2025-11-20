package com.hans.i221271_i220889.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // ==================== AUTH ====================
    @POST("routes/auth/signup.php")
    suspend fun signup(
        @Body body: SignupRequest
    ): Response<ApiResponse<AuthData>>
    
    @POST("routes/auth/login.php")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<ApiResponse<AuthData>>
    
    @POST("routes/auth/logout.php")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Any>>
    
    // ==================== PROFILE ====================
    @GET("routes/profile/get_profile.php")
    suspend fun getProfile(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int
    ): Response<ApiResponse<UserProfileData>>
    
    @POST("routes/profile/update_profile.php")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: UpdateProfileRequest
    ): Response<ApiResponse<UserProfileData>>
    
    @POST("routes/profile/update_fcm_token.php")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body body: UpdateFcmTokenRequest
    ): Response<ApiResponse<Any>>
    
    @Multipart
    @POST("routes/profile/update_profile.php")
    suspend fun uploadProfilePicture(
        @Header("Authorization") token: String,
        @Part("type") type: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<Map<String, String>>>
    
    // ==================== STORIES ====================
    @Multipart
    @POST("routes/stories/create_story.php")
    suspend fun createStory(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("media_type") mediaType: RequestBody
    ): Response<ApiResponse<StoryData>>
    
    @GET("routes/stories/list_stories.php")
    suspend fun listStories(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int? = null
    ): Response<ApiResponse<StoryListResponse>>
    
    // ==================== POSTS ====================
    @Multipart
    @POST("routes/posts/create_post.php")
    suspend fun createPost(
        @Header("Authorization") token: String,
        @Part("caption") caption: RequestBody?,
        @Part image: MultipartBody.Part?,
        @Part("media_type") mediaType: RequestBody?
    ): Response<ApiResponse<PostData>>
    
    @GET("routes/posts/list_feed.php")
    suspend fun listFeed(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<FeedResponse>>
    
    @GET("routes/posts/list_feed.php")
    suspend fun getUserPosts(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<FeedResponse>>
    
    @POST("routes/posts/toggle_like.php")
    suspend fun toggleLike(
        @Header("Authorization") token: String,
        @Body body: ToggleLikeRequest
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("routes/posts/comments.php")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Query("post_id") postId: Int
    ): Response<ApiResponse<List<CommentData>>>
    
    @POST("routes/posts/comments.php")
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Body body: AddCommentRequest
    ): Response<ApiResponse<CommentData>>
    
    // ==================== MESSAGES ====================
    @Multipart
    @POST("routes/messages/send_message.php")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Part("receiver_id") receiverId: RequestBody,
        @Part("message_text") messageText: RequestBody?,
        @Part media: MultipartBody.Part?,
        @Part("media_type") mediaType: RequestBody?,
        @Part("is_vanish") isVanish: RequestBody?
    ): Response<ApiResponse<MessageData>>
    
    @GET("routes/messages/list_messages.php")
    suspend fun listMessages(
        @Header("Authorization") token: String,
        @Query("other_user_id") otherUserId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<MessageData>>>
    
    @POST("routes/messages/edit_message.php")
    suspend fun editMessage(
        @Header("Authorization") token: String,
        @Body body: EditMessageRequest
    ): Response<ApiResponse<MessageData>>
    
    @POST("routes/messages/delete_message.php")
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Body body: DeleteMessageRequest
    ): Response<ApiResponse<Any>>
    
    @POST("routes/messages/mark_seen.php")
    suspend fun markMessagesSeen(
        @Header("Authorization") token: String,
        @Body body: MarkSeenRequest
    ): Response<ApiResponse<Any>>
    
    // ==================== FOLLOWS ====================
    @POST("routes/follows/send_request.php")
    suspend fun sendFollowRequest(
        @Header("Authorization") token: String,
        @Body body: SendFollowRequest
    ): Response<ApiResponse<FollowData>>
    
    @POST("routes/follows/respond_request.php")
    suspend fun respondFollowRequest(
        @Header("Authorization") token: String,
        @Body body: RespondFollowRequest
    ): Response<ApiResponse<Any>>
    
    @GET("routes/follows/list_relations.php")
    suspend fun listRelations(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int,
        @Query("type") type: String // "followers", "following", "requests"
    ): Response<ApiResponse<List<FollowData>>>
    
    @HTTP(method = "DELETE", path = "routes/follows/unfollow.php", hasBody = true)
    suspend fun unfollow(
        @Header("Authorization") token: String,
        @Body body: UnfollowRequest
    ): Response<ApiResponse<Any>>
    
    // ==================== NOTIFICATIONS ====================
    @GET("routes/notifications/list_notifications.php")
    suspend fun listNotifications(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<NotificationData>>>
    
    @POST("routes/notifications/mark_read.php")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Body body: MarkNotificationReadRequest
    ): Response<ApiResponse<Any>>
    
    @POST("routes/notifications/push_event.php")
    suspend fun pushNotificationEvent(
        @Header("Authorization") token: String,
        @Body body: PushNotificationRequest
    ): Response<ApiResponse<Any>>
    
    // ==================== SEARCH ====================
    @GET("routes/search/find_users.php")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("filter") filter: String? = null // "followers", "following", or null for all
    ): Response<ApiResponse<List<UserProfileData>>>
    
    // ==================== STATUS ====================
    @POST("routes/status/update_status.php")
    suspend fun updateStatus(
        @Header("Authorization") token: String,
        @Body body: UpdateStatusRequest
    ): Response<ApiResponse<Any>>
    
    @GET("routes/status/get_status.php")
    suspend fun getStatus(
        @Header("Authorization") token: String,
        @Query("user_ids") userIds: String // comma-separated IDs
    ): Response<ApiResponse<List<UserStatusData>>>
    
    // ==================== SECURITY ====================
    @POST("routes/security/report_screenshot.php")
    suspend fun reportScreenshot(
        @Header("Authorization") token: String,
        @Body body: ReportScreenshotRequest
    ): Response<ApiResponse<Any>>
}

