package com.hans.i221271_i220889

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.CommentsAdapter
import com.hans.i221271_i220889.models.Comment
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.utils.PostRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommentsActivity : AppCompatActivity() {
    
    private lateinit var postRepository: PostRepository
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private val comments = mutableListOf<Comment>()
    private lateinit var post: Post
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_comments)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        postRepository = PostRepository()
        
        // Get post from intent
        post = intent.getSerializableExtra("post") as Post
        
        setupUI()
        setupCommentsRecyclerView()
        loadComments()
    }
    
    private fun setupUI() {
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val commentInput = findViewById<EditText>(R.id.commentInput)
        val sendBtn = findViewById<ImageButton>(R.id.sendBtn)
        
        backBtn.setOnClickListener {
            finish()
        }
        
        sendBtn.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postRepository.addComment(post.postId, commentText) { success ->
                    if (success) {
                        commentInput.setText("")
                        loadComments() // Reload comments
                    } else {
                        Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun setupCommentsRecyclerView() {
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        commentsAdapter = CommentsAdapter(comments)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentsAdapter
    }
    
    private fun loadComments() {
        FirebaseDatabase.getInstance().reference
            .child("posts")
            .child(post.postId)
            .child("comments")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    comments.clear()
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(Comment::class.java)
                        comment?.let { comments.add(it) }
                    }
                    commentsAdapter.notifyDataSetChanged()
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CommentsActivity, "Failed to load comments", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
