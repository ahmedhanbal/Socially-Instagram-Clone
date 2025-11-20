package com.hans.i221271_i220889

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.PostAdapter
import com.hans.i221271_i220889.adapters.StoryAdapter
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.models.Story
import com.hans.i221271_i220889.utils.Base64Image
import com.hans.i221271_i220889.utils.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class OwnProfile : AppCompatActivity() {
    
    private lateinit var postRepository: PostRepository
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

        // Initialize post repository and setup posts
        postRepository = PostRepository()
        setupPostsRecyclerView()
        setupHighlightsRecyclerView()
        loadUserPosts()
        loadUserStats()
        loadUserProfile()
        loadHighlights()

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
        postAdapter = PostAdapter(posts) { post ->
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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            postRepository.getUserPosts(currentUser.uid) { userPosts ->
                runOnUiThread {
                    posts.clear()
                    posts.addAll(userPosts)
                    postAdapter.notifyDataSetChanged()
                    // Update posts count
                    val postsTextView = findViewById<android.widget.TextView>(R.id.Posts)
                    postsTextView.text = "${userPosts.size}\nposts"
                }
            }
        }
    }
    
    private fun loadUserStats() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            
            // Load followers count
            db.child("followers").child(userId).addListenerForSingleValueEvent(
                object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val followersCount = snapshot.children.count()
                        val followersTextView = findViewById<android.widget.TextView>(R.id.Followers)
                        followersTextView.text = formatCount(followersCount) + "\nfollowers"
                        
                        // Make followers clickable
                        followersTextView.setOnClickListener {
                            val intent = Intent(this@OwnProfile, FollowersFollowingActivity::class.java)
                            intent.putExtra("mode", "followers")
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                        }
                        followersTextView.isClickable = true
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                }
            )
            
            // Load following count
            db.child("following").child(userId).addListenerForSingleValueEvent(
                object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val followingCount = snapshot.children.count()
                        val followingTextView = findViewById<android.widget.TextView>(R.id.Following)
                        followingTextView.text = "$followingCount\nfollowing"
                        
                        // Make following clickable
                        followingTextView.setOnClickListener {
                            val intent = Intent(this@OwnProfile, FollowersFollowingActivity::class.java)
                            intent.putExtra("mode", "following")
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                        }
                        followingTextView.isClickable = true
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                }
            )
            
            // Make posts clickable
            val postsTextView = findViewById<android.widget.TextView>(R.id.Posts)
            postsTextView.setOnClickListener {
                val intent = Intent(this, PostsActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
            }
            postsTextView.isClickable = true
        }
    }
    
    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseDatabase.getInstance().reference
            db.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { snapshot ->
                    val user = snapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
                    if (user != null) {
                        // Update username
                        val usernameTextView = findViewById<android.widget.TextView>(R.id.header_title)
                        usernameTextView.text = user.username
                        
                        val usernameTextView2 = findViewById<android.widget.TextView>(R.id.header_title_2)
                        usernameTextView2.text = user.username
                        
                        val usernameTextView3 = findViewById<android.widget.TextView>(R.id.header_title_3)
                        usernameTextView3.text = "@${user.username}"
                        
                        // Update profile image from Firebase
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
                    }
                }
        }
    }
    
    private fun loadHighlights() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseDatabase.getInstance().reference
                .child("highlights")
                .child(currentUser.uid)
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val highlightsList = mutableListOf<Story>()
                        for (highlightSnapshot in snapshot.children) {
                            try {
                                val highlightData = highlightSnapshot.getValue(Map::class.java) as? Map<String, Any>
                                if (highlightData != null) {
                                    val story = Story(
                                        storyId = highlightData["storyId"] as? String ?: "",
                                        userId = currentUser.uid,
                                        username = highlightData["username"] as? String ?: "User",
                                        userProfileImage = highlightData["userProfileImageBase64"] as? String ?: "",
                                        imageUrl = highlightData["imageBase64"] as? String ?: "",
                                        videoUrl = highlightData["videoBase64"] as? String ?: "",
                                        timestamp = (highlightData["timestamp"] as? Long) ?: System.currentTimeMillis(),
                                        expiresAt = Long.MAX_VALUE // Highlights don't expire
                                    )
                                    highlightsList.add(story)
                                }
                            } catch (e: Exception) {
                                // Skip malformed highlight
                            }
                        }
                        runOnUiThread {
                            highlights.clear()
                            highlights.addAll(highlightsList)
                            highlightsAdapter.notifyDataSetChanged()
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })
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
