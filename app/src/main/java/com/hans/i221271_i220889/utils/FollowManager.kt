package com.hans.i221271_i220889.utils

import android.content.Context
import android.widget.Toast
import com.hans.i221271_i220889.models.User
import com.google.firebase.database.FirebaseDatabase

class FollowManager {
    private val database = FirebaseDatabase.getInstance()
    
    // Send follow request
    fun sendFollowRequest(fromUserId: String, toUserId: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
        try {
            val followRequestData = mapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            
            database.reference.child("followRequests").child(toUserId).child(fromUserId)
                .setValue(followRequestData)
                .addOnSuccessListener {
                    onComplete(true, "Follow request sent successfully")
                    Toast.makeText(context, "Follow request sent!", Toast.LENGTH_SHORT).show()
                    
                    // Get sender's username and profile image for notification
                    database.reference.child("users").child(fromUserId).get()
                        .addOnSuccessListener { userSnapshot ->
                            val user = userSnapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
                            val senderUsername = user?.username ?: "Someone"
                            val senderProfileImage = user?.profileImageBase64 ?: ""
                            
                            // Save notification to database
                            val notificationId = database.reference
                                .child("notifications")
                                .child(toUserId)
                                .push()
                                .key ?: return@addOnSuccessListener
                            
                            val notificationData = mapOf(
                                "notificationId" to notificationId,
                                "type" to "follow_request",
                                "fromUserId" to fromUserId,
                                "fromUsername" to senderUsername,
                                "fromUserProfileImage" to senderProfileImage,
                                "title" to "New Follow Request",
                                "body" to "$senderUsername wants to follow you",
                                "timestamp" to System.currentTimeMillis(),
                                "isRead" to false
                            )
                            
                            database.reference
                                .child("notifications")
                                .child(toUserId)
                                .child(notificationId)
                                .setValue(notificationData)
                            
                            // Send FCM push notification
                            try {
                                com.hans.i221271_i220889.services.MyFirebaseMessagingService.sendNotificationToUser(
                                    toUserId,
                                    "New Follow Request",
                                    "$senderUsername wants to follow you",
                                    "follow_request",
                                    fromUserId,
                                    senderUsername
                                )
                            } catch (e: Exception) {
                                // Notification sending failed, but follow request was sent
                            }
                        }
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
        } catch (e: Exception) {
            onComplete(false, "Error sending follow request: ${e.message}")
        }
    }
    
    // Accept follow request
    fun acceptFollowRequest(fromUserId: String, toUserId: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
        try {
            // Add to followers/following lists
            val followerData = mapOf(
                "userId" to fromUserId,
                "timestamp" to System.currentTimeMillis()
            )
            val followingData = mapOf(
                "userId" to toUserId,
                "timestamp" to System.currentTimeMillis()
            )
            
            database.reference.child("followers").child(toUserId).child(fromUserId).setValue(followerData)
            database.reference.child("following").child(fromUserId).child(toUserId).setValue(followingData)
            
            // Remove follow request
            database.reference.child("followRequests").child(toUserId).child(fromUserId).removeValue()
                .addOnSuccessListener {
                    onComplete(true, "Follow request accepted")
                    Toast.makeText(context, "Follow request accepted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
        } catch (e: Exception) {
            onComplete(false, "Error accepting follow request: ${e.message}")
        }
    }
    
    // Reject follow request
    fun rejectFollowRequest(fromUserId: String, toUserId: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
        try {
            database.reference.child("followRequests").child(toUserId).child(fromUserId).removeValue()
                .addOnSuccessListener {
                    onComplete(true, "Follow request rejected")
                    Toast.makeText(context, "Follow request rejected", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
        } catch (e: Exception) {
            onComplete(false, "Error rejecting follow request: ${e.message}")
        }
    }
    
    // Get followers list
    fun getFollowers(userId: String, onComplete: (List<User>) -> Unit) {
        try {
            database.reference.child("followers").child(userId)
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val followers = mutableListOf<User>()
                        for (followerSnapshot in snapshot.children) {
                            val followerId = followerSnapshot.key
                            if (followerId != null) {
                                // Get user details
                                database.reference.child("users").child(followerId)
                                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                                        override fun onDataChange(userSnapshot: com.google.firebase.database.DataSnapshot) {
                                            val user = userSnapshot.getValue(User::class.java)
                                            if (user != null) {
                                                followers.add(user)
                                                if (followers.size == snapshot.children.count().toInt()) {
                                                    onComplete(followers)
                                                }
                                            }
                                        }
                                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                            onComplete(emptyList())
                                        }
                                    })
                            }
                        }
                        if (snapshot.children.count().toInt() == 0) {
                            onComplete(emptyList())
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        onComplete(emptyList())
                    }
                })
        } catch (e: Exception) {
            onComplete(emptyList())
        }
    }
    
    // Get following list
    fun getFollowing(userId: String, onComplete: (List<User>) -> Unit) {
        try {
            database.reference.child("following").child(userId)
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val following = mutableListOf<User>()
                        for (followingSnapshot in snapshot.children) {
                            val followingId = followingSnapshot.key
                            if (followingId != null) {
                                // Get user details
                                database.reference.child("users").child(followingId)
                                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                                        override fun onDataChange(userSnapshot: com.google.firebase.database.DataSnapshot) {
                                            val user = userSnapshot.getValue(User::class.java)
                                            if (user != null) {
                                                following.add(user)
                                                if (following.size == snapshot.children.count().toInt()) {
                                                    onComplete(following)
                                                }
                                            }
                                        }
                                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                            onComplete(emptyList())
                                        }
                                    })
                            }
                        }
                        if (snapshot.children.count().toInt() == 0) {
                            onComplete(emptyList())
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        onComplete(emptyList())
                    }
                })
        } catch (e: Exception) {
            onComplete(emptyList())
        }
    }
    
    // Get pending follow requests - using real-time listener
    fun getPendingFollowRequests(userId: String, onComplete: (List<User>) -> Unit) {
        try {
            database.reference.child("followRequests").child(userId)
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val requests = mutableListOf<User>()
                        val totalRequests = snapshot.children.count().toInt()
                        
                        if (totalRequests == 0) {
                            onComplete(emptyList())
                            return
                        }
                        
                        var completedRequests = 0
                        for (requestSnapshot in snapshot.children) {
                            val requestUserId = requestSnapshot.key
                            if (requestUserId != null) {
                                // Get user details
                                database.reference.child("users").child(requestUserId)
                                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                                        override fun onDataChange(userSnapshot: com.google.firebase.database.DataSnapshot) {
                                            val user = userSnapshot.getValue(User::class.java)
                                            if (user != null) {
                                                requests.add(user)
                                            }
                                            completedRequests++
                                            // Only call onComplete when all requests are processed
                                            if (completedRequests == totalRequests) {
                                                onComplete(requests)
                                            }
                                        }
                                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                            completedRequests++
                                            if (completedRequests == totalRequests) {
                                                onComplete(requests)
                                            }
                                        }
                                    })
                            } else {
                                completedRequests++
                                if (completedRequests == totalRequests) {
                                    onComplete(requests)
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        onComplete(emptyList())
                    }
                })
        } catch (e: Exception) {
            onComplete(emptyList())
        }
    }
    
    // Check if user is following another user
    fun isFollowing(currentUserId: String, targetUserId: String, onComplete: (Boolean) -> Unit) {
        try {
            database.reference.child("following").child(currentUserId).child(targetUserId)
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        onComplete(snapshot.exists())
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        onComplete(false)
                    }
                })
        } catch (e: Exception) {
            onComplete(false)
        }
    }
    
    // Unfollow user
    fun unfollowUser(currentUserId: String, targetUserId: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
        try {
            database.reference.child("following").child(currentUserId).child(targetUserId).removeValue()
            database.reference.child("followers").child(targetUserId).child(currentUserId).removeValue()
                .addOnSuccessListener {
                    onComplete(true, "Unfollowed successfully")
                    Toast.makeText(context, "Unfollowed user", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
        } catch (e: Exception) {
            onComplete(false, "Error unfollowing user: ${e.message}")
        }
    }
}
