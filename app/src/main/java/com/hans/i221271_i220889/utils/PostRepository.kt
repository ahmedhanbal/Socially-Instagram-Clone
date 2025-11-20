package com.hans.i221271_i220889.utils

import android.content.Context
import android.net.Uri
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PostRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun createPost(context: Context, imageUri: Uri, caption: String, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false, null)
        val postId = db.child("posts").push().key ?: return onComplete(false, null)
        
        // Convert image to Base64
        val base64Image = Base64Image.uriToBase64(context, imageUri)
        if (base64Image == null) {
            onComplete(false, null)
            return
        }
        
        // Get user's profile image Base64 from database
        db.child("users").child(uid).get().addOnSuccessListener { userSnapshot ->
            val userData = userSnapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
            val profileImageBase64 = userData?.profileImageBase64 ?: ""
            val username = userData?.username ?: auth.currentUser?.displayName ?: "Unknown User"
            
            val post = Post(
                postId = postId,
                userId = uid,
                username = username,
                userProfileImage = profileImageBase64, // Store Base64 string from database
                imageUrl = base64Image, // Store Base64 string instead of URL
                caption = caption,
                timestamp = System.currentTimeMillis()
            )
            
            db.child("posts").child(postId).setValue(post)
                .addOnCompleteListener { task ->
                    onComplete(task.isSuccessful, if (task.isSuccessful) postId else null)
                }
        }.addOnFailureListener {
            // Fallback if user data fetch fails
            val post = Post(
                postId = postId,
                userId = uid,
                username = auth.currentUser?.displayName ?: "Unknown User",
                userProfileImage = "", // Empty if can't fetch
                imageUrl = base64Image,
                caption = caption,
                timestamp = System.currentTimeMillis()
            )
            
            db.child("posts").child(postId).setValue(post)
                .addOnCompleteListener { task ->
                    onComplete(task.isSuccessful, if (task.isSuccessful) postId else null)
                }
        }
    }

    fun likePost(postId: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        
        db.child("posts").child(postId).get().addOnSuccessListener { snapshot ->
            val post = snapshot.getValue(Post::class.java) ?: return@addOnSuccessListener
            
            val updatedLikes = post.likes.toMutableList()
            val wasLiked = updatedLikes.contains(uid)
            
            if (wasLiked) {
                updatedLikes.remove(uid)
            } else {
                updatedLikes.add(uid)
                // Send like notification (only when liking, not unliking)
                sendLikeNotification(post.userId, postId, post.username)
            }
            
            val updates = mapOf(
                "likes" to updatedLikes,
                "likeCount" to updatedLikes.size
            )
            
            db.child("posts").child(postId).updateChildren(updates)
                .addOnCompleteListener { onComplete(it.isSuccessful) }
        }
    }
    
    private fun sendLikeNotification(postOwnerId: String, postId: String, postOwnerUsername: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Don't send notification if user likes their own post
        if (currentUserId == postOwnerId) return
        
        // Get current user's username and profile image
        db.child("users").child(currentUserId).get()
            .addOnSuccessListener { userSnapshot ->
                val user = userSnapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
                val likerUsername = user?.username ?: "Someone"
                val likerProfileImage = user?.profileImageBase64 ?: ""
                
                // Save notification to database
                val notificationId = db.child("notifications").child(postOwnerId).push().key ?: return@addOnSuccessListener
                val notificationData = mapOf(
                    "notificationId" to notificationId,
                    "type" to "like",
                    "fromUserId" to currentUserId,
                    "fromUsername" to likerUsername,
                    "fromUserProfileImage" to likerProfileImage,
                    "title" to "$likerUsername liked your post",
                    "body" to "$likerUsername liked your post",
                    "timestamp" to System.currentTimeMillis(),
                    "postId" to postId,
                    "isRead" to false
                )
                
                db.child("notifications").child(postOwnerId).child(notificationId).setValue(notificationData)
                
                // Send FCM push notification
                com.hans.i221271_i220889.services.MyFirebaseMessagingService.sendNotificationToUser(
                    postOwnerId,
                    "$likerUsername liked your post",
                    "$likerUsername liked your post",
                    "like",
                    currentUserId,
                    likerUsername
                )
            }
    }

    fun addComment(postId: String, text: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        val commentId = db.child("posts").child(postId).child("comments").push().key ?: return onComplete(false)
        
        // Get user's profile image Base64 from database for comment
        db.child("users").child(uid).get().addOnSuccessListener { userSnapshot ->
            val userData = userSnapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
            val profileImageBase64 = userData?.profileImageBase64 ?: ""
            val username = userData?.username ?: auth.currentUser?.displayName ?: "Unknown User"
            
            val comment = Comment(
                commentId = commentId,
                userId = uid,
                username = username,
                userProfileImage = profileImageBase64, // Store Base64 string from database
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            db.child("posts").child(postId).child("comments").child(commentId).setValue(comment)
                .addOnSuccessListener {
                    // Update comment count
                    db.child("posts").child(postId).child("commentCount").setValue(
                        com.google.firebase.database.ServerValue.increment(1)
                    ).addOnCompleteListener { onComplete(it.isSuccessful) }
                }
                .addOnFailureListener { onComplete(false) }
        }.addOnFailureListener {
            // Fallback if user data fetch fails
            val comment = Comment(
                commentId = commentId,
                userId = uid,
                username = auth.currentUser?.displayName ?: "Unknown User",
                userProfileImage = "", // Empty if can't fetch
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            db.child("posts").child(postId).child("comments").child(commentId).setValue(comment)
                .addOnSuccessListener {
                    // Update comment count
                    db.child("posts").child(postId).child("commentCount").setValue(
                        com.google.firebase.database.ServerValue.increment(1)
                    ).addOnCompleteListener { onComplete(it.isSuccessful) }
                }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun getPosts(onPostsLoaded: (List<Post>) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            // If not logged in, show all posts
            getAllPosts(onPostsLoaded)
            return
        }
        
        // Get user's following list to filter posts
        db.child("users").child(currentUserId).child("following").get().addOnSuccessListener { followingSnapshot ->
            val followingList = mutableListOf<String>()
            followingList.add(currentUserId) // Include own posts
            
            for (userSnapshot in followingSnapshot.children) {
                val userId = userSnapshot.key
                if (userId != null) {
                    followingList.add(userId)
                }
            }
            
            // If not following anyone, show all posts
            if (followingList.size <= 1) {
                getAllPosts(onPostsLoaded)
            } else {
                // Get posts only from followed users
                db.child("posts").orderByChild("timestamp").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val posts = mutableListOf<Post>()
                        for (postSnapshot in snapshot.children) {
                            val post = postSnapshot.getValue(Post::class.java)
                            if (post != null && followingList.contains(post.userId)) {
                                posts.add(post)
                            }
                        }
                        onPostsLoaded(posts.reversed()) // Show newest first
                    }
                    
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        onPostsLoaded(emptyList())
                    }
                })
            }
        }.addOnFailureListener {
            // If error, show all posts
            getAllPosts(onPostsLoaded)
        }
    }
    
    private fun getAllPosts(onPostsLoaded: (List<Post>) -> Unit) {
        db.child("posts").orderByChild("timestamp").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val posts = mutableListOf<Post>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let { posts.add(it) }
                }
                onPostsLoaded(posts.reversed()) // Show newest first
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                onPostsLoaded(emptyList())
            }
        })
    }
    
    fun getUserPosts(userId: String, onComplete: (List<Post>) -> Unit) {
        db.child("posts").orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val postsList = mutableListOf<Post>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        postsList.add(post)
                    }
                }
                onComplete(postsList.reversed()) // Most recent first
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                onComplete(emptyList())
            }
        })
    }
}
