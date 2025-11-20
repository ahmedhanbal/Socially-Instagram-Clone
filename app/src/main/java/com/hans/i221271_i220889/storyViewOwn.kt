package com.hans.i221271_i220889

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hans.i221271_i220889.models.Story
import com.hans.i221271_i220889.utils.Base64Image
import com.hans.i221271_i220889.network.SessionManager
import java.util.*

class storyViewOwn : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var allStories: List<Story>
    private var currentStoryIndex = 0
    private var currentUserIndex = 0
    private lateinit var gestureDetector: GestureDetectorCompat
    
    // UI elements
    private lateinit var storyImageView: ImageView
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var xButton: TextView
    private lateinit var uploadButton: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_story_view_own)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize SessionManager
        sessionManager = SessionManager(this)
        
        // Get story IDs from intent
        val initialStoryId = intent.getStringExtra("initialStoryId")
        val initialUserId = intent.getStringExtra("initialUserId")
        
        android.util.Log.d("storyViewOwn", "Received initialStoryId: $initialStoryId, initialUserId: $initialUserId")
        
        if (initialStoryId != null && initialUserId != null) {
            // Load stories from cache
            loadStoriesFromCache(initialStoryId, initialUserId)
        } else {
            android.util.Log.e("storyViewOwn", "No initialStoryId or initialUserId provided. Finishing activity.")
            finish()
        }
    }
    
    private fun setupUI() {
        storyImageView = findViewById(R.id.story_image)
        profileImageView = findViewById(R.id.profile_image)
        nameTextView = findViewById(R.id.name)
        timeTextView = findViewById(R.id.time_text)
        xButton = findViewById(R.id.x_button)
        uploadButton = findViewById<ImageButton?>(R.id.uploadStoryButton)
        
        // Close button
        xButton?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
        
        // Upload story button
        uploadButton?.setOnClickListener {
            val intent = Intent(this, storyUpload::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }
        
        // Profile image click - navigate to profile
        profileImageView?.setOnClickListener {
            val currentStory = getCurrentStory()
            if (currentStory != null) {
                val currentUserId = sessionManager.getUserId().toString()
                if (currentStory.userId == currentUserId) {
                    val intent = Intent(this, OwnProfile::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, UserProfile::class.java)
                    intent.putExtra("userId", currentStory.userId)
                    startActivity(intent)
                }
                finish()
                overridePendingTransition(0, 0)
            }
        }
        
        // Tap to change story - use onTouchEvent instead to avoid conflicts with gesture detector
        // The gesture detector will handle swipes, and we'll handle taps separately
    }
    
    private fun setupGestureDetector() {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    val deltaX = e2.x - e1.x
                    val deltaY = e2.y - e1.y
                    
                    // Horizontal swipe (change user)
                    if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 100) {
                        if (deltaX > 0) {
                            // Swipe right - previous user
                            previousUser()
                        } else {
                            // Swipe left - next user
                            nextUser()
                        }
                        return true
                    }
                }
                return false
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Tap to change story
                nextStory()
                return true
            }
        }
        
        gestureDetector = GestureDetectorCompat(this, listener)
        
        findViewById<View>(R.id.main)?.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event) || false
        }
    }
    
    private fun groupStoriesByUser(stories: List<Story>): Map<String, List<Story>> {
        val grouped = mutableMapOf<String, MutableList<Story>>()
        for (story in stories) {
            if (!grouped.containsKey(story.userId)) {
                grouped[story.userId] = mutableListOf()
            }
            grouped[story.userId]?.add(story)
        }
        // Sort stories by timestamp for each user
        grouped.values.forEach { it.sortBy { story -> story.timestamp } }
        return grouped
    }
    
    private fun findUserIndex(storiesByUser: Map<String, List<Story>>, userId: String): Int {
        return storiesByUser.keys.indexOf(userId).takeIf { it >= 0 } ?: 0
    }
    
    private fun getCurrentStory(): Story? {
        val storiesByUser = groupStoriesByUser(allStories)
        val userIds = storiesByUser.keys.toList()
        if (currentUserIndex < userIds.size) {
            val userId = userIds[currentUserIndex]
            val userStories = storiesByUser[userId] ?: return null
            if (currentStoryIndex < userStories.size) {
                return userStories[currentStoryIndex]
            }
        }
        return null
    }
    
    private fun displayCurrentStory() {
        val story = getCurrentStory()
        if (story != null) {
            // Set story image (URL or Base64)
            if (story.imageUrl.isNotEmpty()) {
                if (story.imageUrl.startsWith("http") || story.imageUrl.startsWith("uploads/")) {
                    // Load from URL using Picasso
                    val imageUrl = if (story.imageUrl.startsWith("http")) {
                        story.imageUrl
                    } else {
                        com.hans.i221271_i220889.network.ApiConfig.BASE_URL + story.imageUrl
                    }
                    com.squareup.picasso.Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(storyImageView)
                } else {
                    // Fallback to Base64
                    try {
                        val bitmap = Base64Image.base64ToBitmap(story.imageUrl)
                        storyImageView?.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        storyImageView?.setImageResource(R.drawable.ic_default_profile)
                    }
                }
            } else {
                storyImageView?.setImageResource(R.drawable.ic_default_profile)
            }
            
            // Set profile image (URL or Base64)
            if (story.userProfileImage.isNotEmpty()) {
                if (story.userProfileImage.startsWith("http") || story.userProfileImage.startsWith("uploads/")) {
                    // Load from URL using Picasso
                    val imageUrl = if (story.userProfileImage.startsWith("http")) {
                        story.userProfileImage
                    } else {
                        com.hans.i221271_i220889.network.ApiConfig.BASE_URL + story.userProfileImage
                    }
                    com.squareup.picasso.Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(profileImageView)
                } else {
                    // Fallback to Base64
                    try {
                        val bitmap = Base64Image.base64ToBitmap(story.userProfileImage)
                        profileImageView?.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        android.util.Log.e("storyViewOwn", "Error decoding profile image from story: ${e.message}", e)
                        profileImageView?.setImageResource(R.drawable.ic_default_profile)
                    }
                }
            } else {
                profileImageView?.setImageResource(R.drawable.ic_default_profile)
            }
            
            // Set username
            nameTextView?.text = story.username
            
            // Set time
            timeTextView?.text = getTimeAgo(story.timestamp)
            
            // Mark as viewed
            markStoryAsViewed(story)
        }
    }
    
    private fun nextStory() {
        val storiesByUser = groupStoriesByUser(allStories)
        val userIds = storiesByUser.keys.toList()
        if (currentUserIndex < userIds.size) {
            val userId = userIds[currentUserIndex]
            val userStories = storiesByUser[userId] ?: return
            
            // Move to next story of current user
            currentStoryIndex++
            if (currentStoryIndex >= userStories.size) {
                // Move to next user's first story
                currentStoryIndex = 0
                currentUserIndex++
                if (currentUserIndex >= userIds.size) {
                    // Loop back to first user
                    currentUserIndex = 0
                }
            }
            displayCurrentStory()
        }
    }
    
    private fun previousUser() {
        val storiesByUser = groupStoriesByUser(allStories)
        val userIds = storiesByUser.keys.toList()
        if (userIds.isNotEmpty()) {
            currentUserIndex--
            if (currentUserIndex < 0) {
                currentUserIndex = userIds.size - 1
            }
            currentStoryIndex = 0 // Start from first story of new user
            displayCurrentStory()
        }
    }
    
    private fun nextUser() {
        val storiesByUser = groupStoriesByUser(allStories)
        val userIds = storiesByUser.keys.toList()
        if (userIds.isNotEmpty()) {
            currentUserIndex++
            if (currentUserIndex >= userIds.size) {
                currentUserIndex = 0
            }
            currentStoryIndex = 0 // Start from first story of new user
            displayCurrentStory()
        }
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        
        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            else -> "now"
        }
    }
    
    private fun loadProfileImageFromUser(userId: String) {
        // Profile image is already included in the Story object
        // No need to fetch separately from backend
    }
    
    private fun markStoryAsViewed(story: Story) {
        // Story views are tracked on the backend
        // No client-side marking needed
    }
    
    private fun loadStoriesFromCache(initialStoryId: String, initialUserId: String) {
        android.util.Log.d("storyViewOwn", "Loading stories from cache")
        
        setupUI()
        
        // Get stories from cache
        val cachedStories = com.hans.i221271_i220889.utils.StoryCache.getStories()
        
        if (cachedStories.isEmpty()) {
            android.util.Log.e("storyViewOwn", "No stories in cache")
            android.widget.Toast.makeText(this, "No stories available", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        allStories = cachedStories
        
        // Group stories by user and find initial position
        val storiesByUser = groupStoriesByUser(allStories)
        currentUserIndex = findUserIndex(storiesByUser, initialUserId)
        
        // Find initial story index within the user's stories
        if (currentUserIndex < storiesByUser.keys.size) {
            val uid = storiesByUser.keys.toList()[currentUserIndex]
            val userStories = storiesByUser[uid] ?: emptyList()
            currentStoryIndex = userStories.indexOfFirst { it.storyId == initialStoryId }
            if (currentStoryIndex == -1) currentStoryIndex = 0
        }
        
        setupGestureDetector()
        displayCurrentStory()
    }
    
    private fun loadStoriesFromFirebase(initialStoryId: String, initialUserId: String, storyIds: Array<String>?, userIds: Array<String>?) {
        // Deprecated - now using loadStoriesFromCache
        loadStoriesFromCache(initialStoryId, initialUserId)
        
        /* OLD FIREBASE CODE - REMOVED
        if (storyIds != null && storyIds.isNotEmpty() && userIds != null && userIds.size == storyIds.size) {
            val storyList = mutableListOf<Story>()
            var loadedCount = 0
            
            for (i in storyIds.indices) {
                val storyId = storyIds[i]
                val userId = userIds[i]
                
                // FirebaseDatabase.getInstance().reference
                    .child("stories")
                    .child(storyId)
                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            if (snapshot.exists()) {
                                try {
                                    // Access data directly from snapshot
                                    val loadedStoryId = snapshot.child("storyId").getValue(String::class.java) ?: storyId
                                    val loadedUserId = snapshot.child("userId").getValue(String::class.java) ?: userId
                                    val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                                    val userProfileImage = snapshot.child("userProfileImageBase64").getValue(String::class.java) ?: ""
                                    val imageUrl = snapshot.child("imageBase64").getValue(String::class.java) ?: ""
                                    val videoUrl = snapshot.child("videoBase64").getValue(String::class.java) ?: ""
                                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                                    val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                                    
                                    val story = Story(
                                        storyId = loadedStoryId,
                                        userId = loadedUserId,
                                        username = username,
                                        userProfileImage = userProfileImage,
                                        imageUrl = imageUrl,
                                        videoUrl = videoUrl,
                                        timestamp = timestamp,
                                        expiresAt = expiresAt
                                    )
                                    storyList.add(story)
                                } catch (e: Exception) {
                                    android.util.Log.e("storyViewOwn", "Error parsing story $storyId: ${e.message}", e)
                                }
                            }
                            
                            loadedCount++
                            if (loadedCount == storyIds.size) {
                                // All stories loaded
                                allStories = storyList
                                
                                // Group stories by user and find initial position
                                val storiesByUser = groupStoriesByUser(allStories)
                                currentUserIndex = findUserIndex(storiesByUser, initialUserId)
                                
                                // Find initial story index within the user's stories
                                if (currentUserIndex < storiesByUser.keys.size) {
                                    val uid = storiesByUser.keys.toList()[currentUserIndex]
                                    val userStories = storiesByUser[uid] ?: emptyList()
                                    currentStoryIndex = userStories.indexOfFirst { it.storyId == initialStoryId }
                                    if (currentStoryIndex == -1) currentStoryIndex = 0
                                }
                                
                                setupGestureDetector()
                                displayCurrentStory()
                            }
                        }
                        
                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            android.util.Log.e("storyViewOwn", "Failed to load story $storyId: ${error.message}")
                            loadedCount++
                            if (loadedCount == storyIds.size) {
                                if (allStories.isEmpty()) {
                                    finish()
                                } else {
                                    setupGestureDetector()
                                    displayCurrentStory()
                                }
                            }
                        }
                    })
            }
        } else {
            // Fallback: load just the initial story
            FirebaseDatabase.getInstance().reference
                .child("stories")
                .child(initialStoryId)
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        if (snapshot.exists()) {
                            try {
                                val storyId = snapshot.child("storyId").getValue(String::class.java) ?: initialStoryId
                                val userId = snapshot.child("userId").getValue(String::class.java) ?: initialUserId
                                val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                                val userProfileImage = snapshot.child("userProfileImageBase64").getValue(String::class.java) ?: ""
                                val imageUrl = snapshot.child("imageBase64").getValue(String::class.java) ?: ""
                                val videoUrl = snapshot.child("videoBase64").getValue(String::class.java) ?: ""
                                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                                val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                                
                                val story = Story(
                                    storyId = storyId,
                                    userId = userId,
                                    username = username,
                                    userProfileImage = userProfileImage,
                                    imageUrl = imageUrl,
                                    videoUrl = videoUrl,
                                    timestamp = timestamp,
                                    expiresAt = expiresAt
                                )
                                allStories = listOf(story)
                                currentStoryIndex = 0
                                currentUserIndex = 0
                                
                                setupGestureDetector()
                                displayCurrentStory()
                            } catch (e: Exception) {
                                android.util.Log.e("storyViewOwn", "Error loading initial story: ${e.message}", e)
                                finish()
                            }
                        } else {
                            android.util.Log.e("storyViewOwn", "Story $initialStoryId not found")
                            finish()
                        }
                    }
                    
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        android.util.Log.e("storyViewOwn", "Failed to load story: ${error.message}")
                        finish()
                    }
                })
        }
        */
    }
    
    // Old method - deprecated - not used with backend API
    @Deprecated("Stories are now loaded from backend API")
    private fun loadStoriesFromFirebaseOld(userId: String) {
        // No longer used - stories come from backend
        android.widget.Toast.makeText(this, "Story viewing unavailable", android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }
}
