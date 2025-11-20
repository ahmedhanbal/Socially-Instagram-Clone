package com.hans.i221271_i220889.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.HomeScreen
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "New Message"
            val body = remoteMessage.data["body"] ?: "You have a new notification"
            val type = remoteMessage.data["type"] ?: "message"
            val fromUserId = remoteMessage.data["fromUserId"]
            val fromUsername = remoteMessage.data["fromUsername"]
            
            when (type) {
                "follow_request" -> {
                    showFollowRequestNotification(title, body, fromUserId, fromUsername)
                }
                "new_message" -> {
                    showMessageNotification(title, body, fromUserId, fromUsername)
                }
                "call" -> {
                    showCallNotification(title, body, fromUserId, fromUsername, remoteMessage.data)
                }
                "like" -> {
                    showLikeNotification(title, body, fromUserId, fromUsername, remoteMessage.data)
                }
                "screenshot_alert" -> {
                    showScreenshotAlertNotification(title, body, fromUserId, fromUsername)
                }
                else -> {
                    showNotification(title, body, type)
                }
            }
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "New Message"
            val body = notification.body ?: "You have a new notification"
            showNotification(title, body, "notification")
        }
    }

    private fun showNotification(title: String, body: String, type: String) {
        val intent = Intent(this, HomeScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("notification_type", type)
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "socially_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Socially Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server
        sendTokenToServer(token)
    }

    private fun showFollowRequestNotification(title: String, body: String, fromUserId: String?, fromUsername: String?) {
        val intent = Intent(this, com.hans.i221271_i220889.FollowRequestsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        showNotificationWithIntent(title, body, pendingIntent)
    }
    
    private fun showMessageNotification(title: String, body: String, fromUserId: String?, fromUsername: String?) {
        val intent = Intent(this, com.hans.i221271_i220889.Chat::class.java)
        intent.putExtra("userId", fromUserId)
        intent.putExtra("PersonName", fromUsername ?: "User")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            this, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        showNotificationWithIntent(title, body, pendingIntent)
    }
    
    private fun showScreenshotAlertNotification(title: String, body: String, fromUserId: String?, fromUsername: String?) {
        val intent = Intent(this, com.hans.i221271_i220889.Chat::class.java)
        intent.putExtra("userId", fromUserId)
        intent.putExtra("PersonName", fromUsername ?: "User")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            this, 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        showNotificationWithIntent(title, body, pendingIntent)
    }
    
    private fun showCallNotification(title: String, body: String, fromUserId: String?, fromUsername: String?, data: Map<String, String>) {
        val channelName = data["channelName"] ?: ""
        val callType = data["callType"] ?: "voice"
        
        val intent = Intent(this, com.hans.i221271_i220889.CallActivity::class.java)
        intent.putExtra("channelName", channelName)
        intent.putExtra("callType", callType)
        intent.putExtra("isIncomingCall", true)
        intent.putExtra("otherUserId", fromUserId)
        intent.putExtra("otherUserName", fromUsername ?: "User")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            this, 4, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        showNotificationWithIntent(title, body, pendingIntent)
    }
    
    private fun showLikeNotification(title: String, body: String, fromUserId: String?, fromUsername: String?, data: Map<String, String>) {
        val postId = data["postId"] ?: ""
        
        val intent = Intent(this, com.hans.i221271_i220889.HomeScreen::class.java)
        intent.putExtra("notification_type", "like")
        intent.putExtra("postId", postId)
        intent.putExtra("fromUserId", fromUserId)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            this, 5, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        showNotificationWithIntent(title, body, pendingIntent)
    }
    
    private fun showNotificationWithIntent(title: String, body: String, pendingIntent: PendingIntent) {
        val channelId = "socially_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Socially Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun sendTokenToServer(token: String) {
        // Save token to Firebase Database for the current user
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .reference
                .child("users")
                .child(it)
                .child("fcmToken")
                .setValue(token)
        }
    }
    
    companion object {
        fun sendNotificationToUser(userId: String, title: String, body: String, type: String, fromUserId: String? = null, fromUsername: String? = null) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                database.reference.child("users").child(userId).child("fcmToken").get()
                    .addOnSuccessListener { snapshot ->
                        val token = snapshot.getValue(String::class.java)
                        if (token != null) {
                            // Store notification in database for offline users
                            val notificationData = mapOf(
                                "title" to title,
                                "body" to body,
                                "type" to type,
                                "fromUserId" to (fromUserId ?: ""),
                                "fromUsername" to (fromUsername ?: ""),
                                "timestamp" to System.currentTimeMillis()
                            )
                            
                            database.reference.child("notifications").child(userId).push().setValue(notificationData)
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error sending notification: ${e.message}")
            }
        }
    }
}
