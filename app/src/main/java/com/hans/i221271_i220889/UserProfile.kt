package com.hans.i221271_i220889

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.PostAdapter
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.utils.Base64Image
import com.hans.i221271_i220889.utils.FollowManager
import com.hans.i221271_i220889.utils.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserProfile : AppCompatActivity() {
    private lateinit var postRepository: PostRepository
    private lateinit var followManager: FollowManager
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()
    private var targetUserId: String? = null
    private var currentUserId: String? = null
    private var isFollowing = false
    private var hasPendingRequest = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get user IDs
        targetUserId = intent.getStringExtra("userId")
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        // If viewing own profile, redirect to OwnProfile
        if (targetUserId == null || targetUserId == currentUserId) {
            val intent = Intent(this, OwnProfile::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Initialize repositories
        postRepository = PostRepository()
        followManager = FollowManager()
        
        // Setup UI
        setupPostsRecyclerView()
        setupNavigationButtons()
        setupFollowButton()
        
        // Load user data
        loadUserProfile()
        loadUserPosts()
        loadUserStats()
        checkFollowStatus()
        checkPendingRequest()
    }
    
    private fun setupPostsRecyclerView() {
        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        // Use GridLayoutManager for grid view
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        postAdapter = PostAdapter(posts) { post ->
            // Handle post click - open comments
            val intentComments = Intent(this, CommentsActivity::class.java)
            intentComments.putExtra("post", post)
            startActivity(intentComments)
        }
        postsRecyclerView.adapter = postAdapter
    }
    
    private fun setupNavigationButtons() {
        val homeBtn = findViewById<ImageButton>(R.id.tab_1)
        homeBtn.setOnClickListener {
            val intentHome = Intent(this, HomeScreen::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intentHome)
            overridePendingTransition(0, 0)
            finish()
        }

        val searchBtn = findViewById<ImageButton>(R.id.tab_2_search)
        searchBtn.setOnClickListener {
            val intentSearch = Intent(this, Search::class.java)
            startActivity(intentSearch)
            overridePendingTransition(0, 0)
            finish()
        }

        val notificationBtn = findViewById<ImageButton>(R.id.tab_4_notification)
        notificationBtn.setOnClickListener {
            val intentnotification = Intent(this, Notifications::class.java)
            startActivity(intentnotification)
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
    }
    
    private fun setupFollowButton() {
        val followButton = findViewById<TextView>(R.id.FollowingBadge)
        val messageButton = findViewById<TextView>(R.id.MessageBadge)
        val emailButton = findViewById<TextView>(R.id.EmailBadge)
        
        followButton.setOnClickListener {
            if (currentUserId != null && targetUserId != null) {
                if (isFollowing) {
                    // Unfollow
                    followManager.unfollowUser(currentUserId!!, targetUserId!!, this) { success, _ ->
                        if (success) {
                            isFollowing = false
                            updateFollowButton()
                            loadUserStats() // Refresh stats
                        }
                    }
                } else {
                    // Send follow request
                    followManager.sendFollowRequest(currentUserId!!, targetUserId!!, this) { success, _ ->
                        if (success) {
                            hasPendingRequest = true
                            updateFollowButton()
                            // Check if request was auto-accepted (if user accepts immediately)
                            checkFollowStatus()
                        }
                    }
                }
            }
        }
        
        messageButton.setOnClickListener {
            // Navigate to chat
            if (targetUserId != null) {
                val intent = Intent(this, Chat::class.java)
                intent.putExtra("userId", targetUserId)
                val usernameTextView = findViewById<TextView>(R.id.header_title)
                intent.putExtra("username", usernameTextView.text.toString())
                startActivity(intent)
            }
        }
        
        emailButton.setOnClickListener {
            Toast.makeText(this, "Email feature not implemented", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkFollowStatus() {
        if (currentUserId != null && targetUserId != null) {
            followManager.isFollowing(currentUserId!!, targetUserId!!) { following ->
                isFollowing = following
                runOnUiThread {
                    updateFollowButton()
                }
            }
        }
    }
    
    private fun checkPendingRequest() {
        if (currentUserId != null && targetUserId != null) {
            FirebaseDatabase.getInstance().reference
                .child("followRequests")
                .child(targetUserId!!)
                .child(currentUserId!!)
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        hasPendingRequest = snapshot.exists()
                        runOnUiThread {
                            updateFollowButton()
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })
        }
    }
    
    private fun updateFollowButton() {
        val followButton = findViewById<TextView>(R.id.FollowingBadge)
        if (isFollowing) {
            followButton.text = "Following"
            followButton.backgroundTintList = getColorStateList(R.color.grey)
        } else if (hasPendingRequest) {
            followButton.text = "Requested"
            followButton.backgroundTintList = getColorStateList(R.color.grey)
        } else {
            followButton.text = "Follow"
            followButton.backgroundTintList = getColorStateList(R.color.brown)
        }
    }
    
    private fun loadUserProfile() {
        if (targetUserId == null) return
        
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(targetUserId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
                if (user != null) {
                    // Update username
                    val usernameTextView = findViewById<TextView>(R.id.header_title)
                    usernameTextView.text = user.username
                    
                    val usernameTextView2 = findViewById<TextView>(R.id.header_title_2)
                    usernameTextView2.text = user.username
                    
                    val usernameTextView3 = findViewById<TextView>(R.id.header_title_3)
                    usernameTextView3.text = "@${user.username}"
                    
                    // Update profile image
                    val profileImageView = findViewById<ImageView>(R.id.UserStoryView)
                    if (user.profileImageBase64.isNotEmpty()) {
                        val bitmap = Base64Image.base64ToBitmap(user.profileImageBase64)
                        if (bitmap != null) {
                            profileImageView.setImageBitmap(bitmap)
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile)
                        }
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_default_profile)
                    }
                    
                    // Bio field not in User model, leave empty
                    val bioTextView = findViewById<TextView>(R.id.paragraphText)
                    bioTextView.text = ""
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadUserPosts() {
        if (targetUserId == null) return
        
        postRepository.getUserPosts(targetUserId!!) { userPosts ->
            runOnUiThread {
                posts.clear()
                posts.addAll(userPosts)
                postAdapter.notifyDataSetChanged()
                
                // Update posts count
                val postsTextView = findViewById<TextView>(R.id.Posts)
                postsTextView.text = "${userPosts.size}\nposts"
            }
        }
    }
    
    private fun loadUserStats() {
        if (targetUserId == null) return
        
        val db = FirebaseDatabase.getInstance().reference
        
        // Load followers count
        db.child("followers").child(targetUserId!!).addListenerForSingleValueEvent(
            object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val followersCount = snapshot.children.count()
                    val followersTextView = findViewById<TextView>(R.id.Followers)
                    followersTextView.text = formatCount(followersCount) + "\nfollowers"
                    
                    // Make followers clickable
                    followersTextView.setOnClickListener {
                        val intent = Intent(this@UserProfile, FollowersFollowingActivity::class.java)
                        intent.putExtra("mode", "followers")
                        intent.putExtra("userId", targetUserId)
                        startActivity(intent)
                    }
                    followersTextView.isClickable = true
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
        )
        
        // Load following count
        db.child("following").child(targetUserId!!).addListenerForSingleValueEvent(
            object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val followingCount = snapshot.children.count()
                    val followingTextView = findViewById<TextView>(R.id.Following)
                    followingTextView.text = "$followingCount\nfollowing"
                    
                    // Make following clickable
                    followingTextView.setOnClickListener {
                        val intent = Intent(this@UserProfile, FollowersFollowingActivity::class.java)
                        intent.putExtra("mode", "following")
                        intent.putExtra("userId", targetUserId)
                        startActivity(intent)
                    }
                    followingTextView.isClickable = true
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
        )
        
        // Make posts clickable
        val postsTextView = findViewById<TextView>(R.id.Posts)
        postsTextView.setOnClickListener {
            val intent = Intent(this, PostsActivity::class.java)
            intent.putExtra("userId", targetUserId)
            startActivity(intent)
        }
        postsTextView.isClickable = true
    }
    
    private fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
            count >= 1000 -> String.format("%.1fK", count / 1000.0)
            else -> count.toString()
        }
    }
}
