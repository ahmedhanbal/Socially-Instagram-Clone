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
        
        // Get story IDs from intent (to avoid Intent size limits with Base64 images)
        val initialStoryId = intent.getStringExtra("initialStoryId")
        val initialUserId = intent.getStringExtra("initialUserId")
        val storyIds = intent.getStringArrayExtra("storyIds")
        val userIds = intent.getStringArrayExtra("userIds")
        
        android.util.Log.d("storyViewOwn", "Received initialStoryId: $initialStoryId, initialUserId: $initialUserId, storyIds size: ${storyIds?.size ?: 0}")
        
        if (initialStoryId != null && initialUserId != null) {
            // Load stories from Firebase using the IDs
            loadStoriesFromFirebase(initialStoryId, initialUserId, storyIds, userIds)
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
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
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
            // Set story image
            if (story.imageUrl.isNotEmpty()) {
                try {
                    val bitmap = Base64Image.base64ToBitmap(story.imageUrl)
                    storyImageView?.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    storyImageView?.setImageResource(R.drawable.ic_default_profile)
                }
            } else {
                storyImageView?.setImageResource(R.drawable.ic_default_profile)
            }
            
            // Set profile image
            if (story.userProfileImage.isNotEmpty()) {
                try {
                    val bitmap = Base64Image.base64ToBitmap(story.userProfileImage)
                    profileImageView?.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    android.util.Log.e("storyViewOwn", "Error decoding profile image from story: ${e.message}", e)
                    // Fallback: load from user's profile
                    loadProfileImageFromUser(story.userId)
                }
            } else {
                // Fallback: load from user's profile
                loadProfileImageFromUser(story.userId)
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
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("profileImageBase64")
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val profileImageBase64 = snapshot.getValue(String::class.java) ?: ""
                    if (profileImageBase64.isNotEmpty()) {
                        try {
                            val bitmap = Base64Image.base64ToBitmap(profileImageBase64)
                            if (bitmap != null) {
                                runOnUiThread {
                                    profileImageView?.setImageBitmap(bitmap)
                                }
                            } else {
                                runOnUiThread {
                                    profileImageView?.setImageResource(R.drawable.ic_default_profile)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("storyViewOwn", "Error decoding profile image from user: ${e.message}", e)
                            runOnUiThread {
                                profileImageView?.setImageResource(R.drawable.ic_default_profile)
                            }
                        }
                    } else {
                        runOnUiThread {
                            profileImageView?.setImageResource(R.drawable.ic_default_profile)
                        }
                    }
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("storyViewOwn", "Failed to load profile image: ${error.message}")
                    runOnUiThread {
                        profileImageView?.setImageResource(R.drawable.ic_default_profile)
                    }
                }
            })
    }
    
    private fun markStoryAsViewed(story: Story) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null && story.storyId.isNotEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("stories")
                .child(story.storyId)
                .child("viewers")
                .child(currentUserId)
                .setValue(true)
        }
    }
    
    private fun loadStoriesFromFirebase(initialStoryId: String, initialUserId: String, storyIds: Array<String>?, userIds: Array<String>?) {
        android.util.Log.d("storyViewOwn", "Loading stories from Firebase: initialStoryId=$initialStoryId, initialUserId=$initialUserId")
        
        setupUI()
        
        // If story IDs are provided, load those specific stories
        if (storyIds != null && storyIds.isNotEmpty() && userIds != null && userIds.size == storyIds.size) {
            val storyList = mutableListOf<Story>()
            var loadedCount = 0
            
            for (i in storyIds.indices) {
                val storyId = storyIds[i]
                val userId = userIds[i]
                
                FirebaseDatabase.getInstance().reference
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
    }
    
    // Old method kept for backward compatibility (not used anymore)
    @Deprecated("Use loadStoriesFromFirebase with story IDs instead")
    private fun loadStoriesFromFirebaseOld(userId: String) {
        // This is a fallback if stories weren't passed via intent
        // Load all stories from Firebase
        val currentTime = System.currentTimeMillis()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (currentUserId == null) {
            // If not logged in, just show the initial story
            allStories = listOf(Story())
            setupUI()
            displayCurrentStory()
            return
        }
        
        // Get following list
        FirebaseDatabase.getInstance().reference.child("following").child(currentUserId).get()
            .addOnSuccessListener { followingSnapshot ->
                val followingList = mutableListOf<String>()
                followingList.add(currentUserId) // Include own stories
                
                for (userSnapshot in followingSnapshot.children) {
                    val uid = userSnapshot.key
                    if (uid != null) {
                        followingList.add(uid)
                    }
                }
                
                // Load stories
                FirebaseDatabase.getInstance().reference
                    .child("stories")
                    .orderByChild("expiresAt")
                    .startAt(currentTime.toDouble())
                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            val storyList = mutableListOf<Story>()
                            for (storySnapshot in snapshot.children) {
                                try {
                                    val storyData = storySnapshot.getValue(Map::class.java) as? Map<String, Any>
                                    if (storyData != null) {
                                        val storyUserId = storyData["userId"] as? String ?: ""
                                        if (followingList.contains(storyUserId)) {
                                            val story = Story(
                                                storyId = storyData["storyId"] as? String ?: "",
                                                userId = storyUserId,
                                                username = storyData["username"] as? String ?: "User",
                                                userProfileImage = storyData["userProfileImageBase64"] as? String ?: "",
                                                imageUrl = storyData["imageBase64"] as? String ?: "",
                                                videoUrl = storyData["videoBase64"] as? String ?: "",
                                                timestamp = (storyData["timestamp"] as? Long) ?: System.currentTimeMillis(),
                                                expiresAt = (storyData["expiresAt"] as? Long) ?: System.currentTimeMillis()
                                            )
                                            storyList.add(story)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Skip malformed story
                                }
                            }
                            allStories = storyList
                            
                            // Group stories by user and find initial position
                            val storiesByUser = groupStoriesByUser(allStories)
                            currentUserIndex = findUserIndex(storiesByUser, userId)
                            
                            // Find initial story index within the user's stories
                            if (currentUserIndex < storiesByUser.keys.size) {
                                val uid = storiesByUser.keys.toList()[currentUserIndex]
                                val userStories = storiesByUser[uid] ?: emptyList()
                                currentStoryIndex = 0
                            }
                            
                            setupUI()
                            setupGestureDetector()
                            displayCurrentStory()
                        }
                        
                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            // Handle error
                            allStories = listOf(Story())
                            setupUI()
                            displayCurrentStory()
                        }
                    })
            }
            .addOnFailureListener {
                allStories = listOf(Story())
                setupUI()
                displayCurrentStory()
            }
    }
}
