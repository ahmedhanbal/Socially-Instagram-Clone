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
import com.hans.i221271_i220889.utils.PostRepository
import com.google.firebase.auth.FirebaseAuth

class PostsActivity : AppCompatActivity() {
    
    private lateinit var postRepository: PostRepository
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
        
        // Initialize post repository
        postRepository = PostRepository()
        
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
        postAdapter = PostAdapter(posts) { post ->
            // Handle post click - open comments
            val intentComments = Intent(this, CommentsActivity::class.java)
            intentComments.putExtra("post", post)
            startActivity(intentComments)
        }
        postsRecyclerView.adapter = postAdapter
    }
    
    private fun loadUserPosts() {
        if (targetUserId == null) return
        
        postRepository.getUserPosts(targetUserId!!) { userPosts ->
            runOnUiThread {
                posts.clear()
                posts.addAll(userPosts)
                postAdapter.notifyDataSetChanged()
                
                // Update title with post count
                val titleTextView = findViewById<TextView>(R.id.titleTextView)
                titleTextView?.text = "${userPosts.size} Posts"
            }
        }
    }
}

