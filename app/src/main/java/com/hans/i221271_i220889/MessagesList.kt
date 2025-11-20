package com.hans.i221271_i220889

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.UserAdapter
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.repositories.FollowRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MessagesList : AppCompatActivity() {
    
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val users = mutableListOf<User>()
    private lateinit var sessionManager: SessionManager
    private lateinit var followRepository: FollowRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize session and repositories
        sessionManager = SessionManager(this)
        followRepository = FollowRepository(this)

        // Set header title to current username or default
        val usernameTextView = findViewById<TextView>(R.id.header_title)
        val username = sessionManager.getUsername() ?: "Messages"
        usernameTextView.text = username

        // Setup users list
        setupUsersRecyclerView()
        loadUsers()
    }
    
    private fun setupUsersRecyclerView() {
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        userAdapter = UserAdapter(users) { user ->
            // Handle user click - start chat
            val intentChat = Intent(this, Chat::class.java)
            intentChat.putExtra("PersonName", user.username.ifEmpty { "User" })
            intentChat.putExtra("userId", user.userId)
            startActivity(intentChat)
        }
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        usersRecyclerView.adapter = userAdapter
    }
    
    private fun loadUsers() {
        // Show list of users the current user is following (as message contacts)
        lifecycleScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == -1) {
                Toast.makeText(this@MessagesList, "Not logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val result = followRepository.getFollowing(userId)
            result.onSuccess { followDataList ->
                users.clear()
                followDataList.forEach { followData ->
                    users.add(
                        User(
                            userId = followData.followingId.toString(),
                            username = followData.username,
                            email = "",
                            fullName = followData.fullName ?: "",
                            bio = "",
                            profilePicture = followData.profilePicture ?: "",
                            profileImageUrl = "",
                            profileImageBase64 = "",
                            coverPhoto = "",
                            isPrivate = false,
                            isFollowing = true,
                            isFollowedBy = false
                        )
                    )
                }
                userAdapter.notifyDataSetChanged()

                if (users.isEmpty()) {
                    Toast.makeText(this@MessagesList, "No users to message yet", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                Toast.makeText(this@MessagesList, "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
