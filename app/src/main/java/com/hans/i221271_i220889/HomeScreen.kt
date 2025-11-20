package com.hans.i221271_i220889

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.PostAdapter
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.repositories.PostRepositoryApi
import com.hans.i221271_i220889.repositories.StoryRepository
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.network.ApiConfig
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class HomeScreen : AppCompatActivity() {
    private lateinit var postRepository: PostRepositoryApi
    private lateinit var storyRepository: StoryRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()

    // Stories RecyclerView properties
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: com.hans.i221271_i220889.adapters.StoryAdapter
    private val stories = mutableListOf<com.hans.i221271_i220889.models.Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
        setContentView(R.layout.activity_home_screen)
        } catch (e: Exception) {
            // If layout fails, create a simple home screen programmatically
            createSimpleHomeScreen()
            return
        }
        
        // Initialize repositories
        sessionManager = SessionManager(this)
        postRepository = PostRepositoryApi(this)
        storyRepository = StoryRepository(this)
        
        // Setup RecyclerViews
        setupPostsRecyclerView()
        
        // Initialize stories RecyclerView early
        try {
            setupStoriesRecyclerView()
            
            // Post a runnable to check RecyclerView visibility after layout
            storiesRecyclerView.post {
                android.util.Log.d("HomeScreen", "RecyclerView post-layout check - width: ${storiesRecyclerView.width}, height: ${storiesRecyclerView.height}, visibility: ${storiesRecyclerView.visibility}, adapter: ${storiesRecyclerView.adapter}, itemCount: ${storiesRecyclerView.adapter?.itemCount ?: 0}")
                
                // Make sure RecyclerView is visible
                if (storiesRecyclerView.visibility != android.view.View.VISIBLE) {
                    android.util.Log.w("HomeScreen", "RecyclerView is not visible! Setting to VISIBLE")
                    storiesRecyclerView.visibility = android.view.View.VISIBLE
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error setting up stories RecyclerView: ${e.message}", e)
        }

        // Set padding for the main layout based on system bars (status bar, navigation bar)
        try {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
            }
        } catch (e: Exception) {
            // If findViewById fails, continue without window insets
        }

        // Get profile image URI from intent (if provided) for backward compatibility
        val imageUriString = intent.getStringExtra("PROFILE_IMAGE_URI")
        
        // Load profile image from Firebase for bottom nav bar
        try {
            val profileImageInFeed = findViewById<ImageButton>(R.id.tab_5)
            loadProfileImageForBottomNav(profileImageInFeed)
            
            // Also try to load from intent if provided (for backward compatibility)
            if (imageUriString != null) {
                val imageUri = Uri.parse(imageUriString)
                profileImageInFeed.setImageURI(imageUri)
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error loading profile image: ${e.message}", e)
        }

        // Load stories from API
        loadStoriesFromApi()
        val uploadStory = findViewById<ImageButton>(R.id.uploadStoryButton)
        uploadStory?.setOnClickListener {
            val intent = Intent(this, storyUpload::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }

        // Set up the Search button to open the search screen
        val searchBtn = findViewById<ImageButton>(R.id.tab_2_search)
        searchBtn.setOnClickListener {
            val intentSearch = Intent(this, Search::class.java)
            imageUriString?.let {
                val imageUri = Uri.parse(it)
                intentSearch.putExtra("PROFILE_IMAGE_URI", imageUri.toString()) // Pass URI as String
            }
            startActivity(intentSearch)
            overridePendingTransition(0, 0)
        }

        // Set up the Share button to open the select following users screen
        val shareBtn = findViewById<ImageButton>(R.id.shareButton)
        shareBtn.setOnClickListener {
            val intentShare = Intent(this, SelectFollowingActivity::class.java)
            startActivity(intentShare)
        }

        // Stories are now handled entirely by the RecyclerView

        val notificationBtn = findViewById<ImageButton>(R.id.tab_4_notification)
        notificationBtn.setOnClickListener {
            val intentnotification = Intent(this, Notifications::class.java)
            startActivity(intentnotification)
            overridePendingTransition(0, 0)
        }

        val MyProfileBtn = findViewById<ImageButton>(R.id.tab_5)
        MyProfileBtn.setOnClickListener {
            val intentMyProfile = Intent(this, OwnProfile::class.java)
            imageUriString?.let {
                val imageUri = Uri.parse(it)
                intentMyProfile.putExtra("PROFILE_IMAGE_URI", imageUri.toString()) // Pass URI as String
            }
            startActivity(intentMyProfile)
            overridePendingTransition(0, 0)
        }

        // Set up the Plus button to open create post screen
        val plusBtn = findViewById<ImageButton>(R.id.tab_3_plus)
        plusBtn.setOnClickListener {
            val intentCreatePost = Intent(this, CreatePostActivity::class.java)
            startActivityForResult(intentCreatePost, 100)
        }

        // Load posts from API
        loadPostsFromApi()
    }

    private fun setupPostsRecyclerView() {
        try {
        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        postAdapter = PostAdapter(posts, postRepository, sessionManager, lifecycleScope) { post ->
            // Handle comment click
            val intentComments = Intent(this, CommentsActivity::class.java)
            intentComments.putExtra("post", post)
            startActivity(intentComments)
        }
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postsRecyclerView.adapter = postAdapter
        } catch (e: Exception) {
            // If RecyclerView setup fails, continue without posts
        }
    }
    
    private fun setupStoriesRecyclerView() {
        try {
            storiesRecyclerView = findViewById(R.id.storiesRecyclerView)
            
            // Disable nested scrolling to allow horizontal scrolling inside ScrollView
            storiesRecyclerView.isNestedScrollingEnabled = false
            
            // Set up horizontal layout manager
            val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            storiesRecyclerView.layoutManager = layoutManager
            
            // Create adapter with mutable empty list initially
            storyAdapter = com.hans.i221271_i220889.adapters.StoryAdapter(mutableListOf()) { clickedStory ->
                // Handle story click - open story viewer with all stories
                android.util.Log.d("HomeScreen", "Story clicked: ${clickedStory.storyId}, userId: ${clickedStory.userId}")
                try {
                    val intent = Intent(this@HomeScreen, storyViewOwn::class.java)
                    // Pass only story IDs and user IDs to avoid Intent size limits (Base64 images are too large)
                    intent.putExtra("initialStoryId", clickedStory.storyId)
                    intent.putExtra("initialUserId", clickedStory.userId)
                    
                    // Pass list of story IDs and user IDs instead of full Story objects
                    val storyIds = stories.map { it.storyId }.toTypedArray()
                    val userIds = stories.map { it.userId }.toTypedArray()
                    intent.putExtra("storyIds", storyIds)
                    intent.putExtra("userIds", userIds)
                    
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Error opening story view: ${e.message}", e)
                    e.printStackTrace()
                    Toast.makeText(this@HomeScreen, "Error opening story: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            storiesRecyclerView.adapter = storyAdapter
            android.util.Log.d("HomeScreen", "Stories RecyclerView initialized - width: ${storiesRecyclerView.width}, height: ${storiesRecyclerView.height}")
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error setting up stories RecyclerView: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun loadPostsFromApi() {
        lifecycleScope.launch {
            val result = postRepository.getFeed(page = 1, limit = 50)
            result.onSuccess { postDataList ->
                posts.clear()
                postDataList.forEach { postData ->
                    posts.add(Post(
                        postId = postData.id.toString(),
                        userId = postData.userId.toString(),
                        username = postData.username,
                        userProfileImageBase64 = postData.profilePicture ?: "",
                        caption = postData.caption ?: "",
                        imageBase64 = postData.mediaUrl ?: "",
                        videoBase64 = if (postData.mediaType == "video") postData.mediaUrl ?: "" else "",
                        timestamp = System.currentTimeMillis(),
                        likesCount = postData.likesCount,
                        commentsCount = postData.commentsCount,
                        isLikedByCurrentUser = postData.isLiked
                    ))
                }
                postAdapter.notifyDataSetChanged()
            }.onFailure { error ->
                Toast.makeText(this@HomeScreen, "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Refresh posts when returning from create post
            loadPostsFromApi()
        }
    }
    
    private fun loadStoriesFromApi() {
        lifecycleScope.launch {
            val result = storyRepository.getAllStories()
            result.onSuccess { storyDataList ->
                stories.clear()
                storyDataList.forEach { storyData ->
                    val imageUrl = ApiConfig.BASE_URL + storyData.mediaUrl
                    stories.add(com.hans.i221271_i220889.models.Story(
                        storyId = storyData.id.toString(),
                        userId = storyData.userId.toString(),
                        username = storyData.username,
                        userProfileImage = storyData.profilePicture ?: "",
                        imageUrl = imageUrl,
                        videoUrl = if (storyData.mediaType == "video") imageUrl else "",
                        timestamp = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                    ))
                }
                storyAdapter.updateStories(stories)
            }.onFailure { error ->
                Toast.makeText(this@HomeScreen, "Failed to load stories: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadStoriesFromFirebase() {
        try {
            val currentTime = System.currentTimeMillis()
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            
            if (currentUserId == null) {
                // If not logged in, show all stories
                loadAllStories(currentTime)
                return
            }
            
            // Build following list - always include current user
            val followingList = mutableListOf<String>()
            followingList.add(currentUserId) // Include own stories
            
            android.util.Log.d("HomeScreen", "Loading following list for user: $currentUserId")
            
            // Get user's following list to filter stories
            // Following is stored at /following/{userId}/{followingId}
            database.reference.child("following").child(currentUserId).get()
                .addOnSuccessListener { followingSnapshot ->
                    // If following node exists, add followed users
                    if (followingSnapshot.exists()) {
                        for (userSnapshot in followingSnapshot.children) {
                            val userId = userSnapshot.key
                            if (userId != null) {
                                followingList.add(userId)
                                android.util.Log.d("HomeScreen", "Added to following list: $userId")
                            }
                        }
                    } else {
                        android.util.Log.d("HomeScreen", "No following list found for user: $currentUserId")
                    }
                    
                    android.util.Log.d("HomeScreen", "Total following list size: ${followingList.size}")
                    
                    // Load stories from Firebase with 24-hour expiry
                    loadStoriesWithFilter(currentTime, followingList)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("HomeScreen", "Failed to load following list: ${e.message}")
                    // If error loading following list, still load stories (will show only own stories)
                    loadStoriesWithFilter(currentTime, followingList)
                }
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error in loadStoriesFromFirebase: ${e.message}", e)
        }
    }

    private fun loadStoriesWithFilter(currentTime: Long, followingList: List<String>) {
        database.reference.child("stories")
            .orderByChild("expiresAt")
            .startAt(currentTime.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val storyList = mutableListOf<com.hans.i221271_i220889.models.Story>()
                    android.util.Log.d("HomeScreen", "Stories snapshot has ${snapshot.childrenCount} children")

                    for (storySnapshot in snapshot.children) {
                        try {
                            // FIX: Use direct property access instead of getValue(Map::class.java)
                            val storyId = storySnapshot.child("storyId").getValue(String::class.java) ?: ""
                            val userId = storySnapshot.child("userId").getValue(String::class.java) ?: ""
                            val username = storySnapshot.child("username").getValue(String::class.java) ?: "User"
                            val userProfileImage = storySnapshot.child("userProfileImageBase64").getValue(String::class.java) ?: ""
                            val imageUrl = storySnapshot.child("imageBase64").getValue(String::class.java) ?: ""
                            val videoUrl = storySnapshot.child("videoBase64").getValue(String::class.java) ?: ""
                            val timestamp = storySnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val expiresAt = storySnapshot.child("expiresAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            // Filter: show stories from logged-in user AND users they follow
                            if (followingList.contains(userId)) {
                                val story = com.hans.i221271_i220889.models.Story(
                                    storyId = storyId,
                                    userId = userId,
                                    username = username,
                                    userProfileImage = userProfileImage,
                                    imageUrl = imageUrl,
                                    videoUrl = videoUrl,
                                    timestamp = timestamp,
                                    expiresAt = expiresAt
                                )
                                storyList.add(story)
                                android.util.Log.d("HomeScreen", "Added story: ${story.storyId} from user: $userId")
                            } else {
                                android.util.Log.d("HomeScreen", "Skipped story from user: $userId (not in following list)")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("HomeScreen", "Error parsing story: ${e.message}", e)
                        }
                    }

                    android.util.Log.d("HomeScreen", "Total stories loaded: ${storyList.size}")

                    // Update UI with Firebase stories
                    updateStoriesUI(storyList)

                    // Clean up expired stories
                    cleanupExpiredStories(currentTime)
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("HomeScreen", "Failed to load stories: ${error.message}")
                    Toast.makeText(this@HomeScreen, "Failed to load stories", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Also fix the loadAllStories method:
    private fun loadAllStories(currentTime: Long) {
        database.reference.child("stories")
            .orderByChild("expiresAt")
            .startAt(currentTime.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val storyList = mutableListOf<com.hans.i221271_i220889.models.Story>()
                    for (storySnapshot in snapshot.children) {
                        try {
                            // FIX: Use direct property access
                            val storyId = storySnapshot.child("storyId").getValue(String::class.java) ?: ""
                            val userId = storySnapshot.child("userId").getValue(String::class.java) ?: ""
                            val username = storySnapshot.child("username").getValue(String::class.java) ?: "User"
                            val userProfileImage = storySnapshot.child("userProfileImageBase64").getValue(String::class.java) ?: ""
                            val imageUrl = storySnapshot.child("imageBase64").getValue(String::class.java) ?: ""
                            val videoUrl = storySnapshot.child("videoBase64").getValue(String::class.java) ?: ""
                            val timestamp = storySnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val expiresAt = storySnapshot.child("expiresAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            val story = com.hans.i221271_i220889.models.Story(
                                storyId = storyId,
                                userId = userId,
                                username = username,
                                userProfileImage = userProfileImage,
                                imageUrl = imageUrl,
                                videoUrl = videoUrl,
                                timestamp = timestamp,
                                expiresAt = expiresAt
                            )
                            storyList.add(story)
                            android.util.Log.d("HomeScreen", "Added story: ${story.storyId}")
                        } catch (e: Exception) {
                            android.util.Log.e("HomeScreen", "Error parsing story: ${e.message}", e)
                        }
                    }
                    updateStoriesUI(storyList)
                    cleanupExpiredStories(currentTime)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeScreen, "Failed to load stories", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun updateStoriesUI(newStories: List<com.hans.i221271_i220889.models.Story>) {
        runOnUiThread {
            try {
                android.util.Log.d("HomeScreen", "updateStoriesUI called with ${newStories.size} stories")
                
                // Update the class property with all stories
                stories.clear()
                stories.addAll(newStories)
                
                // Group stories by user - show only the most recent story per user
                val groupedStories = groupStoriesByUser(stories)
                
                android.util.Log.d("HomeScreen", "Updating stories UI: ${stories.size} total stories, ${groupedStories.size} grouped stories")
                
                // Update the adapter with grouped stories
                if (::storyAdapter.isInitialized && ::storiesRecyclerView.isInitialized) {
                    // Update the existing adapter's data
                    storyAdapter.updateStories(groupedStories)
                    android.util.Log.d("HomeScreen", "Story adapter updated with ${groupedStories.size} items, adapter itemCount: ${storyAdapter.itemCount}")
                    
                    // Ensure adapter is set (in case it was lost)
                    if (storiesRecyclerView.adapter != storyAdapter) {
                        android.util.Log.w("HomeScreen", "Adapter mismatch! Re-setting adapter.")
                        storiesRecyclerView.adapter = storyAdapter
                    }
                    
                    // Force layout update
                    storiesRecyclerView.post {
                        storiesRecyclerView.requestLayout()
                        storiesRecyclerView.invalidate()
                        android.util.Log.d("HomeScreen", "RecyclerView forced layout - itemCount: ${storyAdapter.itemCount}, width: ${storiesRecyclerView.width}")
                    }
                } else {
                    android.util.Log.e("HomeScreen", "Story adapter or RecyclerView not initialized! adapter: ${::storyAdapter.isInitialized}, recyclerView: ${::storiesRecyclerView.isInitialized}")
                }
            } catch (e: Exception) {
                // If story UI update fails, continue without stories
                android.util.Log.e("HomeScreen", "Error displaying stories: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
    
    private fun groupStoriesByUser(stories: List<com.hans.i221271_i220889.models.Story>): List<com.hans.i221271_i220889.models.Story> {
        // Group stories by userId and keep only the most recent one per user
        val storyMap = mutableMapOf<String, com.hans.i221271_i220889.models.Story>()
        
        for (story in stories) {
            val existingStory = storyMap[story.userId]
            if (existingStory == null || story.timestamp > existingStory.timestamp) {
                storyMap[story.userId] = story
            }
        }
        
        // Return stories sorted by timestamp (most recent first)
        return storyMap.values.sortedByDescending { it.timestamp }
    }
    
    
    private fun cleanupExpiredStories(currentTime: Long) {
        try {
            // Remove expired stories from Firebase
            database.reference.child("stories")
                .orderByChild("expiresAt")
                .endAt(currentTime.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val expiredStoryIds = mutableListOf<String>()
                        for (storySnapshot in snapshot.children) {
                            expiredStoryIds.add(storySnapshot.key ?: "")
                        }
                        
                        // Remove expired stories
                        expiredStoryIds.forEach { storyId ->
                            database.reference.child("stories").child(storyId).removeValue()
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        // Handle error silently
                    }
                })
        } catch (e: Exception) {
            // If Firebase is not initialized, continue without cleanup
        }
    }
    
    private fun loadProfileImageForBottomNav(profileImageButton: ImageButton) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            database.reference
                .child("users")
                .child(currentUser.uid)
                .child("profileImageBase64")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val profileImageBase64 = snapshot.getValue(String::class.java) ?: ""
                        if (profileImageBase64.isNotEmpty()) {
                            try {
                                val bitmap = com.hans.i221271_i220889.utils.Base64Image.base64ToBitmap(profileImageBase64)
                                if (bitmap != null) {
                                    runOnUiThread {
                                        profileImageButton.setImageBitmap(bitmap)
                                    }
                                } else {
                                    runOnUiThread {
                                        profileImageButton.setImageResource(R.drawable.ic_default_profile)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Error decoding profile image: ${e.message}", e)
                                runOnUiThread {
                                    profileImageButton.setImageResource(R.drawable.ic_default_profile)
                                }
                            }
                        } else {
                            runOnUiThread {
                                profileImageButton.setImageResource(R.drawable.ic_default_profile)
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        android.util.Log.e("HomeScreen", "Failed to load profile image: ${error.message}")
                        runOnUiThread {
                            profileImageButton.setImageResource(R.drawable.ic_default_profile)
                        }
                    }
                })
        } else {
            profileImageButton.setImageResource(R.drawable.ic_default_profile)
        }
    }
    
    private fun createSimpleHomeScreen() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(50, 50, 50, 50)
        }
        
        val welcomeText = android.widget.TextView(this).apply {
            text = "Welcome to Socially!"
            textSize = 32f
            setTextColor(android.graphics.Color.parseColor("#8e3f42"))
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 50
            }
        }
        
        val statusText = android.widget.TextView(this).apply {
            text = "Home Screen - Demo Mode"
            textSize = 18f
            setTextColor(android.graphics.Color.GRAY)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30
            }
        }
        
        val logoutButton = android.widget.Button(this).apply {
            text = "Logout"
            setBackgroundColor(android.graphics.Color.parseColor("#8e3f42"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                val intent = Intent(this@HomeScreen, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        
        layout.addView(welcomeText)
        layout.addView(statusText)
        layout.addView(logoutButton)
        setContentView(layout)
    }
}
