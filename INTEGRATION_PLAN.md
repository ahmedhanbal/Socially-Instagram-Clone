# Socially - Backend Integration Plan

## Overview
Migrating from Firebase to PHP/MySQL backend while maintaining all existing functionality.

## Database Setup
1. Import `schema.sql` into XAMPP MySQL
2. Database: `socially`, User: `root`, Password: (empty)
3. Create uploads directories structure

## Integration Strategy - Feature by Feature

### Phase 1: Foundation & Authentication ✓
**Files to Modify:**
- Create `ApiService.kt` - Retrofit interface
- Create `ApiClient.kt` - Retrofit singleton
- Create `SessionManager.kt` - Local session storage (SharedPreferences)
- Modify `signup.kt` - Call signup API instead of Firebase
- Modify `LoginActivity.kt` - Call login API instead of Firebase
- Modify `FirebaseAuthManager.kt` - Add logout API call

**API Endpoints:**
- POST `/routes/auth/signup.php`
- POST `/routes/auth/login.php`
- POST `/routes/auth/logout.php`

### Phase 2: Profile Management
**Files to Modify:**
- Modify `OwnProfile.kt` - Load profile from API
- Modify `UserProfile.kt` - Load user profile from API
- Create `ProfileRepository.kt` - Handle profile API calls
- Modify profile picture/cover photo upload logic

**API Endpoints:**
- GET `/routes/profile/get_profile.php?user_id={id}`
- POST `/routes/profile/update_profile.php`
- POST `/routes/profile/upload_picture.php`

### Phase 3: Stories Feature
**Files to Modify:**
- Modify `storyUpload.kt` - Upload story via API
- Modify `StoryAdapter.kt` - Load stories from API
- Modify `HomeScreen.kt` - Fetch stories from API
- Add story expiration cleanup job

**API Endpoints:**
- POST `/routes/stories/create_story.php`
- GET `/routes/stories/list_stories.php`
- GET `/routes/stories/delete_expired.php` (cron job)

### Phase 4: Posts, Likes & Comments
**Files to Modify:**
- Modify `CreatePostActivity.kt` - Create post via API
- Modify `PostAdapter.kt` - Load posts from API
- Modify `PostRepository.kt` - Replace Firebase with API calls
- Modify `CommentsActivity.kt` - Load/add comments via API

**API Endpoints:**
- POST `/routes/posts/create_post.php`
- GET `/routes/posts/list_feed.php`
- POST `/routes/posts/toggle_like.php`
- GET `/routes/posts/comments.php?post_id={id}`
- POST `/routes/posts/comments.php`

### Phase 5: Messaging System
**Files to Modify:**
- Modify `Chat.kt` - Send/receive messages via API
- Modify `MessageAdapter.kt` - Handle message display
- Modify `ChatRepository.kt` - Replace Firebase with API
- Add message edit/delete within 5 minutes logic
- Implement vanish mode logic

**API Endpoints:**
- POST `/routes/messages/send_message.php`
- GET `/routes/messages/list_messages.php?user_id={id}&other_user_id={id}`
- PUT `/routes/messages/edit_message.php`
- DELETE `/routes/messages/delete_message.php`
- POST `/routes/messages/mark_seen.php`

### Phase 6: Follow System
**Files to Modify:**
- Modify `FollowManager.kt` - Replace Firebase with API
- Modify `FollowRequestsActivity.kt` - Handle requests via API
- Modify `FollowersFollowingActivity.kt` - Load lists via API

**API Endpoints:**
- POST `/routes/follows/send_request.php`
- POST `/routes/follows/respond_request.php`
- GET `/routes/follows/list_relations.php?user_id={id}&type={followers|following|requests}`
- DELETE `/routes/follows/unfollow.php`

### Phase 7: Notifications
**Files to Modify:**
- Modify `Notifications.kt` - Load notifications from API
- Modify `MyFirebaseMessagingService.kt` - Store FCM token via API
- Create notification triggers on backend events

**API Endpoints:**
- GET `/routes/notifications/list_notifications.php?user_id={id}`
- POST `/routes/notifications/push_event.php`
- POST `/routes/notifications/mark_read.php`

### Phase 8: Search & Filters
**Files to Modify:**
- Modify `Search.kt` - Search users via API
- Add filter options (followers/following)

**API Endpoints:**
- GET `/routes/search/find_users.php?query={text}&filter={all|followers|following}`

### Phase 9: Online/Offline Status
**Files to Modify:**
- Modify `OnlineStatusManager.kt` - Update status via API
- Modify `PresenceManager.kt` - Poll status from API
- Add status indicators in chat/profile screens

**API Endpoints:**
- POST `/routes/status/update_status.php`
- GET `/routes/status/get_status.php?user_ids={comma-separated}`

### Phase 10: Security Features
**Files to Modify:**
- Modify `ScreenshotDetector.kt` - Report to API
- Add notification trigger for screenshots

**API Endpoints:**
- POST `/routes/security/report_screenshot.php`

### Phase 11: SQLite Offline Support
**Files to Create:**
- `LocalDatabase.kt` - Room database
- `OfflineQueueManager.kt` - Queue failed requests
- `SyncService.kt` - Background sync worker
- Entity classes for local caching

**Tables Needed:**
- cached_posts
- cached_messages
- cached_stories
- pending_actions (upload queue)

## Commit Strategy
- 2:1 ratio - ahmedhanbal : naveedahmed5
- Commit after each major component/feature integration
- Clear commit messages describing changes

## Testing Checklist per Feature
- [ ] API returns correct JSON format
- [ ] Error handling for network failures
- [ ] Loading states shown properly
- [ ] Data persists correctly
- [ ] Offline mode works (later phases)

## Dependencies to Add
```kotlin
// Retrofit & OkHttp
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// Room Database (for offline)
implementation("androidx.room:room-runtime:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// WorkManager (for sync)
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## Backend Configuration
- Base URL: `http://localhost/socially_api/` (development)
- Production: Update to actual server IP/domain
- Enable CORS for development
- Set up proper file permissions for uploads/ directories

