package com.hans.i221271_i220889

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
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
import com.hans.i221271_i220889.adapters.NotificationAdapter
import com.hans.i221271_i220889.models.Notification

class Notifications : AppCompatActivity() {
    
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        val homeBtn = findViewById<ImageButton>(R.id.tab_1)
        homeBtn.setOnClickListener {
            val intentHome = Intent(this, HomeScreen::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intentHome)
            overridePendingTransition(0, 0)
            finish()
        }

        val MyProfileBtn = findViewById<ImageButton>(R.id.tab_5)
        MyProfileBtn.setOnClickListener {
            val intentMyProfile = Intent(this, OwnProfile::class.java)
            startActivity(intentMyProfile)
            overridePendingTransition(0, 0)
            finish()
        }

        // Set up the Search button to open the search screen
        val searchBtn = findViewById<ImageButton>(R.id.tab_2_search)
        searchBtn.setOnClickListener {
            val intentSearch = Intent(this, Search::class.java)
            startActivity(intentSearch)
            overridePendingTransition(0, 0)
            finish()
        }
        
        // Set up Follow Requests button
        val followRequestsButton = findViewById<android.widget.Button>(R.id.followRequestsButton)
        followRequestsButton.setOnClickListener {
            val intentFollowRequests = Intent(this, FollowRequestsActivity::class.java)
            startActivity(intentFollowRequests)
        }
        
        // Setup notifications RecyclerView
        setupNotificationsRecyclerView()
        
        // Load notifications
        if (currentUserId != null) {
            loadNotifications()
        }
    }
    
    private fun setupNotificationsRecyclerView() {
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        notificationAdapter = NotificationAdapter(notifications) { notification ->
            handleNotificationClick(notification)
        }
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationsRecyclerView.adapter = notificationAdapter
    }
    
    private fun loadNotifications() {
        if (currentUserId == null) return
        
        FirebaseDatabase.getInstance().reference
            .child("notifications")
            .child(currentUserId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notifications.clear()
                    
                    for (notificationSnapshot in snapshot.children) {
                        try {
                            // Use direct property access instead of Map deserialization
                            val notification = Notification(
                                notificationId = notificationSnapshot.child("notificationId").getValue(String::class.java) ?: notificationSnapshot.key ?: "",
                                type = notificationSnapshot.child("type").getValue(String::class.java) ?: "",
                                fromUserId = notificationSnapshot.child("fromUserId").getValue(String::class.java) ?: "",
                                fromUsername = notificationSnapshot.child("fromUsername").getValue(String::class.java) ?: "",
                                fromUserProfileImage = notificationSnapshot.child("fromUserProfileImage").getValue(String::class.java) ?: "",
                                title = notificationSnapshot.child("title").getValue(String::class.java) ?: "",
                                body = notificationSnapshot.child("body").getValue(String::class.java) ?: "",
                                timestamp = notificationSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                                postId = notificationSnapshot.child("postId").getValue(String::class.java) ?: "",
                                channelName = notificationSnapshot.child("channelName").getValue(String::class.java) ?: "",
                                callType = notificationSnapshot.child("callType").getValue(String::class.java) ?: "",
                                isRead = notificationSnapshot.child("isRead").getValue(Boolean::class.java) ?: false
                            )
                            notifications.add(notification)
                        } catch (e: Exception) {
                            android.util.Log.e("Notifications", "Error parsing notification: ${e.message}", e)
                        }
                    }
                    
                    // Sort by timestamp (newest first)
                    notifications.sortByDescending { it.timestamp }
                    
                    // Filter to show only follow_request and like notifications
                    val filteredNotifications = notifications.filter { 
                        it.type == "follow_request" || it.type == "like" 
                    }
                    
                    runOnUiThread {
                        notificationAdapter.updateNotifications(filteredNotifications)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("Notifications", "Failed to load notifications: ${error.message}")
                }
            })
    }
    
    private fun handleNotificationClick(notification: Notification) {
        // Mark as read
        if (!notification.isRead && currentUserId != null) {
            FirebaseDatabase.getInstance().reference
                .child("notifications")
                .child(currentUserId)
                .child(notification.notificationId)
                .child("isRead")
                .setValue(true)
        }
        
        when (notification.type) {
            "call" -> {
                val intent = Intent(this, CallActivity::class.java)
                intent.putExtra("channelName", notification.channelName)
                intent.putExtra("callType", notification.callType)
                intent.putExtra("isIncomingCall", true)
                intent.putExtra("otherUserId", notification.fromUserId)
                intent.putExtra("otherUserName", notification.fromUsername)
                startActivity(intent)
            }
            "message" -> {
                val intent = Intent(this, Chat::class.java)
                intent.putExtra("userId", notification.fromUserId)
                intent.putExtra("PersonName", notification.fromUsername)
                startActivity(intent)
            }
            "follow_request" -> {
                val intent = Intent(this, FollowRequestsActivity::class.java)
                startActivity(intent)
            }
            "like" -> {
                // Navigate to post or home screen
                val intent = Intent(this, HomeScreen::class.java)
                intent.putExtra("highlightPostId", notification.postId)
                startActivity(intent)
            }
        }
    }
}
