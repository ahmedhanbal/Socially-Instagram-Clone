package com.hans.i221271_i220889

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hans.i221271_i220889.adapters.PostAdapter
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.repositories.PostRepositoryApi
import com.hans.i221271_i220889.repositories.StoryRepository
import com.hans.i221271_i220889.repositories.SearchRepository
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.network.ApiConfig
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class HomeScreen : AppCompatActivity() {
    private lateinit var postRepository: PostRepositoryApi
    private lateinit var storyRepository: StoryRepository
    private lateinit var searchRepository: SearchRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()

    // Stories RecyclerView properties
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: com.hans.i221271_i220889.adapters.StoryAdapter
    private val stories = mutableListOf<com.hans.i221271_i220889.models.Story>()
    private val allStoriesFlat = mutableListOf<com.hans.i221271_i220889.models.Story>()
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

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
        searchRepository = SearchRepository(this)
        
        // Update status to online when app starts
        lifecycleScope.launch {
            searchRepository.updateOnlineStatus(true)
        }
        
        // Setup SwipeRefresh
        try {
            swipeRefreshLayout = findViewById(R.id.swipeRefresh)
            swipeRefreshLayout?.setOnRefreshListener {
                loadStoriesFromApi()
                loadPostsFromApi()
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error setting up SwipeRefreshLayout: ${e.message}", e)
        }

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
        
        // Load profile image for bottom nav bar
        try {
            val profileImageInFeed = findViewById<ImageButton>(R.id.tab_5)
            loadProfileImageForBottomNav(profileImageInFeed)
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error loading profile image: ${e.message}", e)
        }

        // Load stories from API
        loadStoriesFromApi()
        val uploadStory = findViewById<ImageView>(R.id.uploadStoryButton)
        uploadStory?.setOnClickListener {
            val intent = Intent(this, storyUpload::class.java)
            startActivity(intent)
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

    override fun onResume() {
        super.onResume()
        // Update status to online
        lifecycleScope.launch {
            searchRepository.updateOnlineStatus(true)
        }
        // Refresh feed whenever HomeScreen comes to foreground
        loadStoriesFromApi()
        loadPostsFromApi()
    }
    
    override fun onPause() {
        super.onPause()
        // Don't set offline on pause - only when app goes to background
    }
    
    override fun onStop() {
        super.onStop()
        // Update status to offline when app goes to background
        lifecycleScope.launch {
            searchRepository.updateOnlineStatus(false)
        }
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
                    // Cache stories in singleton to avoid Intent size limits
                    com.hans.i221271_i220889.utils.StoryCache.setStories(allStoriesFlat)
                    
                    val intent = Intent(this@HomeScreen, storyViewOwn::class.java)
                    intent.putExtra("initialStoryId", clickedStory.storyId)
                    intent.putExtra("initialUserId", clickedStory.userId)
                    
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
            try {
                val result = postRepository.getFeed(page = 1, limit = 50)
                result.onSuccess { postDataList ->
                    Log.d("HomeScreen", "Successfully loaded ${postDataList.size} posts")
                    posts.clear()
                    val currentUserId = sessionManager.getUserId()
                    postDataList.forEach { postData ->
                        Log.d("HomeScreen", "Processing post: ${postData.id} by ${postData.username}")

                        // Fallback to session profile picture for own posts when feed doesn't include it
                        val rawProfilePic = postData.profilePicture
                            ?: if (postData.userId == currentUserId) {
                                sessionManager.getProfilePicture()
                            } else {
                                null
                            }

                        posts.add(
                            Post(
                                postId = postData.id.toString(),
                                userId = postData.userId.toString(),
                                username = postData.username,
                                userProfileImage = rawProfilePic ?: "",
                                caption = postData.caption ?: "",
                                imageUrl = postData.mediaUrl ?: "",
                                videoBase64 = if (postData.mediaType == "video") postData.mediaUrl ?: "" else "",
                                timestamp = System.currentTimeMillis(),
                                likesCount = postData.likesCount,
                                commentsCount = postData.commentsCount,
                                isLikedByCurrentUser = postData.isLiked
                            )
                        )
                    }
                    runOnUiThread {
                        postAdapter.notifyDataSetChanged()
                        swipeRefreshLayout?.isRefreshing = false
                    }
                }.onFailure { error ->
                    Log.e("HomeScreen", "Error loading posts: ${error.message}", error)
                    runOnUiThread {
                        Toast.makeText(
                            this@HomeScreen, 
                            "Failed to load posts: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        swipeRefreshLayout?.isRefreshing = false
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Unexpected error in loadPostsFromApi", e)
                runOnUiThread {
                    Toast.makeText(
                        this@HomeScreen, 
                        "Unexpected error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    swipeRefreshLayout?.isRefreshing = false
                }
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
            try {
                val result = storyRepository.getAllStories()
                result.onSuccess { storyDataList ->
                    Log.d("HomeScreen", "Successfully loaded ${storyDataList.size} stories")
                    stories.clear()
                    allStoriesFlat.clear()

                    val currentUserId = sessionManager.getUserId()
                    val flatStories = storyDataList.mapNotNull { storyData ->
                        try {
                            // Use story profile picture if present; otherwise fallback to session profile picture for own stories
                            val rawProfilePic = storyData.profilePicture
                                ?: if (storyData.userId == currentUserId) {
                                    sessionManager.getProfilePicture()
                                } else {
                                    null
                                }

                            val profilePicUrl =
                                if (!rawProfilePic.isNullOrEmpty()) ApiConfig.BASE_URL + rawProfilePic
                                else ""

                            val mediaPath = storyData.mediaUrl ?: return@mapNotNull null
                            val fullMediaUrl = ApiConfig.BASE_URL + mediaPath

                            com.hans.i221271_i220889.models.Story(
                                storyId = storyData.id.toString(),
                                userId = storyData.userId.toString(),
                                username = storyData.username,
                                userProfileImage = profilePicUrl,
                                imageUrl = if (storyData.mediaType == "video") "" else fullMediaUrl,
                                videoUrl = if (storyData.mediaType == "video") fullMediaUrl else "",
                                timestamp = parseTimestamp(storyData.createdAt),
                                expiresAt = parseTimestamp(storyData.expiresAt)
                            )
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error processing story ${storyData.id}", e)
                            null
                        }
                    }

                    allStoriesFlat.addAll(flatStories)

                    val groupedStories = flatStories.groupBy { it.userId }
                    groupedStories.values.forEach { userStories ->
                        val mostRecent = userStories.maxByOrNull { it.timestamp } ?: return@forEach
                        stories.add(mostRecent)
                    }

                    runOnUiThread {
                        com.hans.i221271_i220889.utils.StoryCache.setStories(allStoriesFlat)
                        storyAdapter.updateStories(stories)
                        swipeRefreshLayout?.isRefreshing = false
                    }
                }.onFailure { error ->
                    Log.e("HomeScreen", "Error loading stories: ${error.message}", error)
                    runOnUiThread {
                        Toast.makeText(
                            this@HomeScreen, 
                            "Failed to load stories: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        swipeRefreshLayout?.isRefreshing = false
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Unexpected error in loadStoriesFromApi", e)
                runOnUiThread {
                    Toast.makeText(
                        this@HomeScreen, 
                        "Unexpected error loading stories: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    swipeRefreshLayout?.isRefreshing = false
                }
            }
        }
    }

    private fun parseTimestamp(value: String?): Long {
        return try {
            value?.let { java.sql.Timestamp.valueOf(it).time } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun loadProfileImageForBottomNav(profileImageButton: ImageButton) {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()
                if (userId == -1) {
                    profileImageButton.setImageResource(R.drawable.ic_default_profile)
                    return@launch
                }
                
                val result = com.hans.i221271_i220889.repositories.ProfileRepository(this@HomeScreen)
                    .getProfile(userId)
                
                result.onSuccess { profile ->
                    runOnUiThread {
                        if (!profile.profilePicture.isNullOrEmpty()) {
                            val imageUrl = ApiConfig.BASE_URL + profile.profilePicture
                            Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(profileImageButton)
                        } else {
                            profileImageButton.setImageResource(R.drawable.ic_default_profile)
                        }
                    }
                }.onFailure {
                    runOnUiThread {
                        profileImageButton.setImageResource(R.drawable.ic_default_profile)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Error loading profile image: ${e.message}", e)
                runOnUiThread {
                    profileImageButton.setImageResource(R.drawable.ic_default_profile)
                }
            }
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
