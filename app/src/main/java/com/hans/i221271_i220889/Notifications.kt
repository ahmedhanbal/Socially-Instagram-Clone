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
import com.hans.i221271_i220889.adapters.NotificationAdapter
import com.hans.i221271_i220889.models.Notification
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.repositories.NotificationRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Notifications : AppCompatActivity() {
    
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var notificationRepository: NotificationRepository
    private val notifications = mutableListOf<Notification>()
    
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
        sessionManager = SessionManager(this)
        notificationRepository = NotificationRepository(this)
        setupNotificationsRecyclerView()
        
        // Load notifications from API
        loadNotifications()
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
        lifecycleScope.launch {
            val result = notificationRepository.getNotifications()
            result.onSuccess { notificationDataList ->
                notifications.clear()
                // Convert NotificationData to Notification model
                notificationDataList.forEach { notificationData ->
                    notifications.add(Notification(
                        notificationId = notificationData.id.toString(),
                        type = notificationData.type,
                        title = notificationData.title,
                        message = notificationData.message,
                        fromUserId = notificationData.relatedUserId?.toString() ?: "",
                        fromUsername = notificationData.relatedUserName ?: "",
                        profilePicture = notificationData.relatedUserPicture ?: "",
                        postId = notificationData.relatedItemId?.toString() ?: "",
                        timestamp = notificationData.createdAt,
                        isRead = notificationData.isRead,
                        channelName = "", // Not stored in backend
                        callType = "" // Not stored in backend
                    ))
                }
                notificationAdapter.notifyDataSetChanged()
            }.onFailure { exception ->
                Toast.makeText(this@Notifications, "Failed to load notifications: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleNotificationClick(notification: Notification) {
        // Mark as read
        if (!notification.isRead) {
            lifecycleScope.launch {
                notificationRepository.markAsRead(notification.notificationId.toInt())
                notification.isRead = true
                notificationAdapter.notifyDataSetChanged()
            }
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
