package com.hans.i221271_i220889

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.UserAdapter
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.repositories.FollowRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Activity to select following users to send messages to
 */
class SelectFollowingActivity : AppCompatActivity() {
    
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var followRepository: FollowRepository
    private val followingUsers = mutableListOf<User>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_following)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        val headerTitle = findViewById<TextView>(R.id.header_title)
        headerTitle.text = "Select User to Message"
        
        // Back button
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        sessionManager = SessionManager(this)
        followRepository = FollowRepository(this)
        
        // Setup RecyclerView
        setupUsersRecyclerView()
        
        // Load following users
        loadFollowingUsers()
    }
    
    private fun setupUsersRecyclerView() {
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        userAdapter = UserAdapter(followingUsers) { user ->
            // Handle user click - start chat with selected user
            try {
                if (user.userId.isNotEmpty() && user.userId != sessionManager.getUserId().toString()) {
                    val intentChat = Intent(this, Chat::class.java)
                    intentChat.putExtra("PersonName", user.username.ifEmpty { "User" })
                    intentChat.putExtra("userId", user.userId)
                    startActivity(intentChat)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid user selected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("SelectFollowingActivity", "Error opening chat: ${e.message}", e)
                Toast.makeText(this, "Error opening chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        usersRecyclerView.adapter = userAdapter
    }
    
    private fun loadFollowingUsers() {
        lifecycleScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == -1) {
                Toast.makeText(this@SelectFollowingActivity, "Not logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val result = followRepository.getFollowing(userId)
            result.onSuccess { followDataList ->
                followingUsers.clear()
                followDataList.forEach { followData ->
                    followingUsers.add(User(
                        userId = followData.followingId.toString(),
                        username = followData.username,
                        fullName = followData.fullName ?: "",
                        profilePicture = followData.profilePicture ?: "",
                        bio = "",
                        isPrivate = false, // FollowData doesn't include privacy info
                        isFollowing = true,
                        isFollowedBy = false
                    ))
                }
                userAdapter.notifyDataSetChanged()
                
                if (followingUsers.isEmpty()) {
                    Toast.makeText(this@SelectFollowingActivity, "No following users found", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { exception ->
                Toast.makeText(this@SelectFollowingActivity, "Failed to load following: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

