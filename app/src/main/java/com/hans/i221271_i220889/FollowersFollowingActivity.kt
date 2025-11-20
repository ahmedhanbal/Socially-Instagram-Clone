package com.hans.i221271_i220889

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.UserAdapter
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.repositories.FollowRepository
import com.hans.i221271_i220889.network.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FollowersFollowingActivity : AppCompatActivity() {
    private lateinit var followRepository: FollowRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val users = mutableListOf<User>()
    private var currentMode = "followers" // "followers" or "following"
    private var targetUserId: String? = null // User ID whose followers/following to show
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get mode and userId from intent
        currentMode = intent.getStringExtra("mode") ?: "followers"
        targetUserId = intent.getStringExtra("userId")
        
        createSimpleFollowersFollowingScreen()
        
        followRepository = FollowRepository(this)
        sessionManager = SessionManager(this)
        
        setupUsersRecyclerView()
        loadUsers()
    }
    
    private fun createSimpleFollowersFollowingScreen() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
        }
        
        val titleText = android.widget.TextView(this).apply {
            text = if (currentMode == "followers") "Followers" else "Following"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#8e3f42"))
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30
            }
        }
        
        val backButton = android.widget.Button(this).apply {
            text = "← Back"
            setBackgroundColor(android.graphics.Color.parseColor("#8e3f42"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                finish()
            }
        }
        
        val switchButton = android.widget.Button(this).apply {
            text = if (currentMode == "followers") "Show Following" else "Show Followers"
            setBackgroundColor(android.graphics.Color.parseColor("#8e3f42"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                currentMode = if (currentMode == "followers") "following" else "followers"
                titleText.text = if (currentMode == "followers") "Followers" else "Following"
                text = if (currentMode == "followers") "Show Following" else "Show Followers"
                loadUsers()
            }
        }
        
        usersRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@FollowersFollowingActivity)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        layout.addView(titleText)
        layout.addView(backButton)
        layout.addView(switchButton)
        layout.addView(usersRecyclerView)
        setContentView(layout)
    }
    
    private fun setupUsersRecyclerView() {
        userAdapter = UserAdapter(users) { user ->
            // Handle user click - navigate to user profile
            val currentUserId = sessionManager.getUserId().toString()
            if (user.userId == currentUserId) {
                // Navigate to own profile
                val intent = android.content.Intent(this, OwnProfile::class.java)
                startActivity(intent)
            } else {
                // Navigate to user profile
                val intent = android.content.Intent(this, UserProfile::class.java)
                intent.putExtra("userId", user.userId)
                startActivity(intent)
            }
        }
        usersRecyclerView.adapter = userAdapter
    }
    
    private fun loadUsers() {
        lifecycleScope.launch {
            // Use targetUserId if provided, otherwise use current user ID
            val userId = targetUserId?.toIntOrNull() ?: sessionManager.getUserId()
            if (userId == -1) {
                Toast.makeText(this@FollowersFollowingActivity, "Not logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val result = if (currentMode == "followers") {
                followRepository.getFollowers(userId)
            } else {
                followRepository.getFollowing(userId)
            }
            
            result.onSuccess { followDataList ->
                users.clear()
                // Convert FollowData to User model
                followDataList.forEach { followData ->
                    // For followers: followData contains the follower info
                    // For following: followData contains the following info
                    val actualUserId = if (currentMode == "followers") {
                        followData.followerId
                    } else {
                        followData.followingId
                    }
                    
                    users.add(User(
                        userId = actualUserId.toString(),
                        username = followData.username,
                        fullName = followData.fullName ?: "",
                        profilePicture = followData.profilePicture ?: "",
                        bio = "",
                        isPrivate = false, // FollowData doesn't include privacy info
                        isFollowing = false, // Would need additional check
                        isFollowedBy = false // Would need additional check
                    ))
                }
                userAdapter.notifyDataSetChanged()
            }.onFailure { exception ->
                Toast.makeText(
                    this@FollowersFollowingActivity,
                    "Failed to load ${if (currentMode == "followers") "followers" else "following"}: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
