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
import com.hans.i221271_i220889.repositories.ProfileRepository
import com.hans.i221271_i220889.repositories.PostRepositoryApi
import com.hans.i221271_i220889.repositories.FollowRepository
import com.hans.i221271_i220889.network.SessionManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch

class UserProfile : AppCompatActivity() {
    private lateinit var profileRepository: ProfileRepository
    private lateinit var postRepository: PostRepositoryApi
    private lateinit var followRepository: FollowRepository
    private lateinit var sessionManager: SessionManager
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

        // Initialize repositories and session
        sessionManager = SessionManager(this)
        profileRepository = ProfileRepository(this)
        postRepository = PostRepositoryApi(this)
        followRepository = FollowRepository(this)
        
        // Get user IDs
        targetUserId = intent.getStringExtra("userId")
        currentUserId = sessionManager.getUserId().toString()
        
        // If viewing own profile, redirect to OwnProfile
        if (targetUserId == null || targetUserId == currentUserId) {
            val intent = Intent(this, OwnProfile::class.java)
            startActivity(intent)
            finish()
            return
        }
        
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
        // Use vertical list (like home feed) for user posts
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(posts, postRepository, sessionManager, lifecycleScope) { post ->
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
                val targetUserIdInt = targetUserId!!.toIntOrNull()
                if (targetUserIdInt == null) {
                    Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                lifecycleScope.launch {
                    if (isFollowing) {
                        val result = followRepository.unfollow(targetUserIdInt)
                        result.onSuccess {
                            isFollowing = false
                            hasPendingRequest = false
                            updateFollowButton()
                            loadUserStats() // Refresh stats
                            Toast.makeText(this@UserProfile, "Unfollowed successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(this@UserProfile, "Failed to unfollow: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Send follow request
                        val result = followRepository.sendFollowRequest(targetUserIdInt)
                        result.onSuccess {
                            // Don't set isFollowing = true, just set hasPendingRequest = true
                            hasPendingRequest = true
                            isFollowing = false
                            updateFollowButton()
                            loadUserStats() // Refresh stats
                            Toast.makeText(this@UserProfile, "Follow request sent", Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(this@UserProfile, "Failed to send request: ${error.message}", Toast.LENGTH_SHORT).show()
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
            val targetUserIdInt = targetUserId!!.toIntOrNull() ?: return
            val currentUserIdInt = currentUserId!!.toIntOrNull() ?: return
            
            lifecycleScope.launch {
                // Check if current user is following the target user (accepted)
                val followingResult = followRepository.getFollowing(currentUserIdInt)
                followingResult.onSuccess { followingList ->
                    isFollowing = followingList.any { it.followingId == targetUserIdInt }
                    
                    // If not following, check if there's a pending request
                    if (!isFollowing) {
                        val pendingResult = followRepository.hasPendingRequestTo(targetUserIdInt)
                        pendingResult.onSuccess { hasPending ->
                            hasPendingRequest = hasPending
                            updateFollowButton()
                        }.onFailure {
                            // If check fails, assume no pending request
                            hasPendingRequest = false
                            updateFollowButton()
                        }
                    } else {
                        // If already following, no pending request
                        hasPendingRequest = false
                        updateFollowButton()
                    }
                }.onFailure {
                    // If check fails, try to check pending request anyway
                    val pendingResult = followRepository.hasPendingRequestTo(targetUserIdInt)
                    pendingResult.onSuccess { hasPending ->
                        hasPendingRequest = hasPending
                        updateFollowButton()
                    }.onFailure {
                        hasPendingRequest = false
                        updateFollowButton()
                    }
                }
            }
        }
    }
    
    private fun checkPendingRequest() {
        // This is now handled in checkFollowStatus()
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
        
        lifecycleScope.launch {
            val result = profileRepository.getProfile(targetUserId!!.toIntOrNull() ?: return@launch)
            result.onSuccess { userData ->
                // Update username
                findViewById<TextView>(R.id.header_title).text = userData.username
                findViewById<TextView>(R.id.header_title_2).text = userData.username
                findViewById<TextView>(R.id.header_title_3).text = "@${userData.username}"
                
                // Update profile image using Picasso
                val profileImageView = findViewById<ImageView>(R.id.UserStoryView)
                val profilePic = userData.profilePicture
                if (!profilePic.isNullOrEmpty()) {
                    val imageUrl = com.hans.i221271_i220889.network.ApiConfig.BASE_URL + profilePic
                    com.squareup.picasso.Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_default_profile)
                }
                
                // Update bio
                val bioTextView = findViewById<TextView>(R.id.paragraphText)
                bioTextView.text = userData.bio ?: ""
            }.onFailure { error ->
                Toast.makeText(this@UserProfile, "Failed to load user profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadUserPosts() {
        if (targetUserId == null) return
        
        lifecycleScope.launch {
            val result = postRepository.getUserPosts(targetUserId!!.toIntOrNull() ?: return@launch)
            result.onSuccess { postDataList ->
                posts.clear()
                postDataList.forEach { postData ->
                    posts.add(
                        Post(
                            postId = postData.id.toString(),
                            userId = postData.userId.toString(),
                            username = postData.username,
                            userProfileImage = postData.profilePicture ?: "",
                            caption = postData.caption ?: "",
                            imageUrl = postData.mediaUrl ?: "",
                            videoBase64 = if (postData.mediaType == "video") postData.mediaUrl ?: "" else "",
                            timestamp = System.currentTimeMillis(),
                            likesCount = postData.likesCount,
                            commentsCount = postData.commentsCount,
                            isLikedByCurrentUser = postData.isLiked
                        )
                    )
                }
                postAdapter.notifyDataSetChanged()
                
                // Update posts count
                findViewById<TextView>(R.id.Posts).text = "${postDataList.size}\nposts"
            }.onFailure { error ->
                Toast.makeText(this@UserProfile, "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadUserStats() {
        if (targetUserId == null) return
        
        lifecycleScope.launch {
            val result = profileRepository.getProfile(targetUserId!!.toIntOrNull() ?: return@launch)
            result.onSuccess { userData ->
                // Update followers count
                val followersTextView = findViewById<TextView>(R.id.Followers)
                followersTextView.text = formatCount(userData.followersCount) + "\nfollowers"
                followersTextView.setOnClickListener {
                    val intent = Intent(this@UserProfile, FollowersFollowingActivity::class.java)
                    intent.putExtra("mode", "followers")
                    intent.putExtra("userId", targetUserId)
                    startActivity(intent)
                }
                followersTextView.isClickable = true
                
                // Update following count
                val followingTextView = findViewById<TextView>(R.id.Following)
                followingTextView.text = "${userData.followingCount}\nfollowing"
                followingTextView.setOnClickListener {
                    val intent = Intent(this@UserProfile, FollowersFollowingActivity::class.java)
                    intent.putExtra("mode", "following")
                    intent.putExtra("userId", targetUserId)
                    startActivity(intent)
                }
                followingTextView.isClickable = true
            }.onFailure { error ->
                Toast.makeText(this@UserProfile, "Failed to load stats: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
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
