package com.hans.i221271_i220889

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.PostAdapter
import com.hans.i221271_i220889.adapters.StoryAdapter
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.models.Story
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.repositories.ProfileRepository
import com.hans.i221271_i220889.repositories.PostRepositoryApi
import com.hans.i221271_i220889.repositories.FollowRepository
import com.hans.i221271_i220889.utils.AuthHelper
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class OwnProfile : AppCompatActivity() {
    
    private lateinit var profileRepository: ProfileRepository
    private lateinit var postRepository: PostRepositoryApi
    private lateinit var followRepository: FollowRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var highlightsRecyclerView: RecyclerView
    private lateinit var highlightsAdapter: StoryAdapter
    private val posts = mutableListOf<Post>()
    private val highlights = mutableListOf<Story>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_own_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize repositories
        sessionManager = SessionManager(this)
        profileRepository = ProfileRepository(this)
        postRepository = PostRepositoryApi(this)
        followRepository = FollowRepository(this)
        
        setupPostsRecyclerView()
        setupHighlightsRecyclerView()
        setupMenuButton()
        loadUserProfile()
        loadUserPosts()
        loadUserStats()

        val homeBtn = findViewById<ImageButton>(R.id.tab_1)

        homeBtn.setOnClickListener {
            val intentHome = Intent(this, HomeScreen::class.java).apply {
                // If HomeScreen exists in the task, move it to front; else create it
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intentHome)
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

        val notificationBtn = findViewById<ImageButton>(R.id.tab_4_notification)
        notificationBtn.setOnClickListener {
            val intentnotification = Intent(this, Notifications::class.java)
            startActivity(intentnotification)
            overridePendingTransition(0, 0)
            finish()
        }
        
        // Add create post button access
        val createPostBtn = findViewById<ImageButton?>(R.id.tab_3_plus)
        createPostBtn?.setOnClickListener {
            val intentCreatePost = Intent(this, CreatePostActivity::class.java)
            startActivityForResult(intentCreatePost, 100)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Refresh posts when returning from create post
            loadUserPosts()
        }
    }
    
    private fun setupPostsRecyclerView() {
        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        postAdapter = PostAdapter(posts, postRepository, sessionManager, lifecycleScope) { post ->
            // Handle comment click
            val intentComments = Intent(this, CommentsActivity::class.java)
            intentComments.putExtra("post", post)
            startActivity(intentComments)
        }
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postsRecyclerView.adapter = postAdapter
    }
    
    private fun setupHighlightsRecyclerView() {
        highlightsRecyclerView = findViewById(R.id.highlightsRecyclerView)
        highlightsAdapter = StoryAdapter(highlights) { story ->
            // Handle highlight click - open story viewer
            val intent = Intent(this, UserStoryView::class.java)
            intent.putExtra("story", story)
            startActivity(intent)
        }
        highlightsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        highlightsRecyclerView.adapter = highlightsAdapter
    }
    
    private fun loadUserPosts() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return
        
        lifecycleScope.launch {
            val result = postRepository.getUserPosts(userId)
            result.onSuccess { postDataList ->
                // Convert API PostData to model Post
                posts.clear()
                postDataList.forEach { postData ->
                    posts.add(Post(
                        postId = postData.id.toString(),
                        userId = postData.userId.toString(),
                        username = postData.username,
                        userProfileImageBase64 = postData.profilePicture ?: "",
                        caption = postData.caption ?: "",
                        imageBase64 = postData.mediaUrl ?: "",
                        videoBase64 = if (postData.mediaType == "video") postData.mediaUrl ?: "" else "",
                        timestamp = System.currentTimeMillis(), // Would need proper parsing
                        likesCount = postData.likesCount,
                        commentsCount = postData.commentsCount,
                        isLikedByCurrentUser = postData.isLiked
                    ))
                }
                postAdapter.notifyDataSetChanged()
                
                // Update posts count
                val postsTextView = findViewById<android.widget.TextView>(R.id.Posts)
                postsTextView.text = "${posts.size}\nposts"
            }.onFailure { error ->
                Toast.makeText(this@OwnProfile, "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadUserStats() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return
        
        lifecycleScope.launch {
            // Load followers
            val followersResult = followRepository.getFollowers(userId)
            followersResult.onSuccess { followers ->
                val followersTextView = findViewById<android.widget.TextView>(R.id.Followers)
                followersTextView.text = formatCount(followers.size) + "\nfollowers"
                followersTextView.setOnClickListener {
                    val intent = Intent(this@OwnProfile, FollowersFollowingActivity::class.java)
                    intent.putExtra("mode", "followers")
                    intent.putExtra("userId", userId.toString())
                    startActivity(intent)
                }
                followersTextView.isClickable = true
            }
            
            // Load following
            val followingResult = followRepository.getFollowing(userId)
            followingResult.onSuccess { following ->
                val followingTextView = findViewById<android.widget.TextView>(R.id.Following)
                followingTextView.text = "${following.size}\nfollowing"
                followingTextView.setOnClickListener {
                    val intent = Intent(this@OwnProfile, FollowersFollowingActivity::class.java)
                    intent.putExtra("mode", "following")
                    intent.putExtra("userId", userId.toString())
                    startActivity(intent)
                }
                followingTextView.isClickable = true
            }
        }
        
        // Make posts clickable
        val postsTextView = findViewById<android.widget.TextView>(R.id.Posts)
        postsTextView.setOnClickListener {
            val intent = Intent(this, PostsActivity::class.java)
            intent.putExtra("userId", userId.toString())
            startActivity(intent)
        }
        postsTextView.isClickable = true
    }
    
    private fun loadUserProfile() {
        lifecycleScope.launch {
            val result = profileRepository.getOwnProfile()
            result.onSuccess { profile ->
                // Update username
                val usernameTextView = findViewById<android.widget.TextView>(R.id.header_title)
                usernameTextView.text = profile.username
                
                val usernameTextView2 = findViewById<android.widget.TextView>(R.id.header_title_2)
                usernameTextView2.text = profile.username
                
                val usernameTextView3 = findViewById<android.widget.TextView>(R.id.header_title_3)
                usernameTextView3.text = "@${profile.username}"
                
                // Update bio if exists
                val bioTextView = findViewById<android.widget.TextView?>(R.id.bio)
                bioTextView?.text = profile.bio ?: ""
                
                // Update full name if exists
                val fullNameTextView = findViewById<android.widget.TextView?>(R.id.full_name)
                fullNameTextView?.text = profile.fullName ?: profile.username
                
                // Update profile image using Picasso for caching
                val profileImageView = findViewById<ImageView>(R.id.UserStoryView)
                if (!profile.profilePicture.isNullOrEmpty()) {
                    val imageUrl = com.hans.i221271_i220889.network.ApiConfig.BASE_URL + profile.profilePicture
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_default_profile)
                }
                
                // Update cover photo if exists
                val coverImageView = findViewById<ImageView?>(R.id.cover_photo)
                if (!profile.coverPhoto.isNullOrEmpty() && coverImageView != null) {
                    val coverUrl = com.hans.i221271_i220889.network.ApiConfig.BASE_URL + profile.coverPhoto
                    Picasso.get()
                        .load(coverUrl)
                        .into(coverImageView)
                }
                
                // Update stats from profile data
                findViewById<android.widget.TextView>(R.id.Posts).text = "${profile.postsCount}\nposts"
                findViewById<android.widget.TextView>(R.id.Followers).text = formatCount(profile.followersCount) + "\nfollowers"
                findViewById<android.widget.TextView>(R.id.Following).text = "${profile.followingCount}\nfollowing"
                
            }.onFailure { error ->
                Toast.makeText(this@OwnProfile, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupMenuButton() {
        // Find menu/options button (usually three dots or similar)
        val menuButton = findViewById<ImageButton?>(R.id.options_button) 
            ?: findViewById<ImageButton?>(R.id.menu_button)
            ?: findViewById<ImageButton?>(R.id.settings_button)
        
        menuButton?.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(android.R.menu.submenu, popup.menu)
            
            // Add custom menu items
            popup.menu.clear()
            popup.menu.add(0, 1, 0, "Edit Profile")
            popup.menu.add(0, 2, 0, "Settings")
            popup.menu.add(0, 3, 0, "Logout")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        // Navigate to edit profile
                        val intent = Intent(this, ProfilePictureUpdateActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    2 -> {
                        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    3 -> {
                        // Logout
                        AuthHelper.logout(this, showToast = true)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
    
    private fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
            count >= 1000 -> String.format("%.1fK", count / 1000.0)
            else -> count.toString()
        }
    }
}
