package com.hans.i221271_i220889

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.PostAdapter
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.repositories.PostRepositoryApi
import com.hans.i221271_i220889.network.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast

class PostsActivity : AppCompatActivity() {
    
    private lateinit var postRepository: PostRepositoryApi
    private lateinit var sessionManager: SessionManager
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()
    private var targetUserId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_posts)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Get userId from intent
        targetUserId = intent.getStringExtra("userId")
        
        // Initialize repositories
        sessionManager = SessionManager(this)
        postRepository = PostRepositoryApi(this)
        
        // Setup UI
        setupPostsRecyclerView()
        setupBackButton()
        
        // Load posts
        if (targetUserId != null) {
            loadUserPosts()
        }
    }
    
    private fun setupBackButton() {
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton?.setOnClickListener {
            finish()
        }
    }
    
    private fun setupPostsRecyclerView() {
        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        // Use LinearLayoutManager for vertical list view
        postsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postAdapter = PostAdapter(posts, postRepository, sessionManager, lifecycleScope) { post ->
            // Handle post click - open comments
            val intentComments = Intent(this, CommentsActivity::class.java)
            intentComments.putExtra("post", post)
            startActivity(intentComments)
        }
        postsRecyclerView.adapter = postAdapter
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
                
                // Update title with post count
                val titleTextView = findViewById<TextView>(R.id.titleTextView)
                titleTextView?.text = "${posts.size} Posts"
            }
        }
    }
}

