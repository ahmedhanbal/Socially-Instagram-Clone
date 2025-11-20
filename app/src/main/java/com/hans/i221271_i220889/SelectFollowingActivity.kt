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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hans.i221271_i220889.adapters.UserAdapter
import com.hans.i221271_i220889.models.User

/**
 * Activity to select following users to send messages to
 */
class SelectFollowingActivity : AppCompatActivity() {
    
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val followingUsers = mutableListOf<User>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
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
                if (user.userId.isNotEmpty() && user.userId != currentUserId) {
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
        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Get list of users the current user is following
        FirebaseDatabase.getInstance().reference
            .child("following")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    followingUsers.clear()
                    
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        Toast.makeText(this@SelectFollowingActivity, "You are not following anyone yet", Toast.LENGTH_SHORT).show()
                        userAdapter.notifyDataSetChanged()
                        return
                    }
                    
                    val followingUserIds = mutableListOf<String>()
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key
                        if (userId != null) {
                            followingUserIds.add(userId)
                        }
                    }
                    
                    // Load user details for each following user
                    loadUserDetails(followingUserIds)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SelectFollowingActivity, "Failed to load following users", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun loadUserDetails(userIds: List<String>) {
        if (userIds.isEmpty()) {
            userAdapter.notifyDataSetChanged()
            return
        }
        
        val database = FirebaseDatabase.getInstance().reference
        var loadedCount = 0
        
        for (userId in userIds) {
            database.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            if (user != null && user.userId == userId) {
                                followingUsers.add(user)
                            }
                        }
                        
                        loadedCount++
                        if (loadedCount == userIds.size) {
                            // All users loaded, update adapter
                            runOnUiThread {
                                userAdapter.notifyDataSetChanged()
                                if (followingUsers.isEmpty()) {
                                    Toast.makeText(this@SelectFollowingActivity, "No following users found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == userIds.size) {
                            runOnUiThread {
                                userAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                })
        }
    }
}

