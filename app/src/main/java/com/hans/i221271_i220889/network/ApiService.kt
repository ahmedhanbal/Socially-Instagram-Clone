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
        @Query("user_id") userId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserProfileData>>
    
    @POST("routes/profile/update_profile.php")
    @FormUrlEncoded
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Field("full_name") fullName: String?,
        @Field("bio") bio: String?,
        @Field("is_private") isPrivate: Boolean?
    ): Response<ApiResponse<UserProfileData>>
    
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
    ): Response<ApiResponse<List<StoryData>>>
    
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
    ): Response<ApiResponse<List<PostData>>>
    
    @GET("routes/posts/list_feed.php")
    suspend fun getUserPosts(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<PostData>>>
    
    @POST("routes/posts/toggle_like.php")
    @FormUrlEncoded
    suspend fun toggleLike(
        @Header("Authorization") token: String,
        @Field("post_id") postId: Int
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("routes/posts/comments.php")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Query("post_id") postId: Int
    ): Response<ApiResponse<List<CommentData>>>
    
    @POST("routes/posts/comments.php")
    @FormUrlEncoded
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Field("post_id") postId: Int,
        @Field("comment_text") commentText: String
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
    
    @PUT("routes/messages/edit_message.php")
    @FormUrlEncoded
    suspend fun editMessage(
        @Header("Authorization") token: String,
        @Field("message_id") messageId: Int,
        @Field("message_text") messageText: String
    ): Response<ApiResponse<MessageData>>
    
    @HTTP(method = "DELETE", path = "routes/messages/delete_message.php", hasBody = true)
    @FormUrlEncoded
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Field("message_id") messageId: Int
    ): Response<ApiResponse<Any>>
    
    @POST("routes/messages/mark_seen.php")
    @FormUrlEncoded
    suspend fun markMessagesSeen(
        @Header("Authorization") token: String,
        @Field("other_user_id") otherUserId: Int
    ): Response<ApiResponse<Any>>
    
    // ==================== FOLLOWS ====================
    @POST("routes/follows/send_request.php")
    @FormUrlEncoded
    suspend fun sendFollowRequest(
        @Header("Authorization") token: String,
        @Field("following_id") followingId: Int
    ): Response<ApiResponse<FollowData>>
    
    @POST("routes/follows/respond_request.php")
    @FormUrlEncoded
    suspend fun respondFollowRequest(
        @Header("Authorization") token: String,
        @Field("follow_id") followId: Int,
        @Field("action") action: String // "accept" or "reject"
    ): Response<ApiResponse<Any>>
    
    @GET("routes/follows/list_relations.php")
    suspend fun listRelations(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int,
        @Query("type") type: String // "followers", "following", "requests"
    ): Response<ApiResponse<List<FollowData>>>
    
    @HTTP(method = "DELETE", path = "routes/follows/unfollow.php", hasBody = true)
    @FormUrlEncoded
    suspend fun unfollow(
        @Header("Authorization") token: String,
        @Field("following_id") followingId: Int
    ): Response<ApiResponse<Any>>
    
    // ==================== NOTIFICATIONS ====================
    @GET("routes/notifications/list_notifications.php")
    suspend fun listNotifications(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<NotificationData>>>
    
    @POST("routes/notifications/mark_read.php")
    @FormUrlEncoded
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Field("notification_id") notificationId: Int
    ): Response<ApiResponse<Any>>
    
    @POST("routes/notifications/push_event.php")
    @FormUrlEncoded
    suspend fun pushNotificationEvent(
        @Header("Authorization") token: String,
        @Field("user_id") userId: Int,
        @Field("type") type: String,
        @Field("title") title: String,
        @Field("message") message: String,
        @Field("related_user_id") relatedUserId: Int?,
        @Field("related_item_id") relatedItemId: Int?
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
    @FormUrlEncoded
    suspend fun updateStatus(
        @Header("Authorization") token: String,
        @Field("is_online") isOnline: Boolean
    ): Response<ApiResponse<Any>>
    
    @GET("routes/status/get_status.php")
    suspend fun getStatus(
        @Header("Authorization") token: String,
        @Query("user_ids") userIds: String // comma-separated IDs
    ): Response<ApiResponse<List<UserStatusData>>>
    
    // ==================== SECURITY ====================
    @POST("routes/security/report_screenshot.php")
    @FormUrlEncoded
    suspend fun reportScreenshot(
        @Header("Authorization") token: String,
        @Field("reported_user_id") reportedUserId: Int,
        @Field("chat_context") chatContext: String?
    ): Response<ApiResponse<Any>>
}

