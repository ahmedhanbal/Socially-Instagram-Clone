package com.hans.i221271_i220889

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hans.i221271_i220889.models.Story
import com.hans.i221271_i220889.utils.Base64Image
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class UserStoryView : AppCompatActivity() {
    private lateinit var story: Story
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_story_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get story from intent
        story = intent.getSerializableExtra("story") as? Story
            ?: Story() // Fallback if story not provided

        setupStoryView()
        setupCloseButton()
        setupProfileClick()
        markStoryAsViewed()
    }
    
    private fun setupStoryView() {
        val storyImageView = findViewById<ImageView>(R.id.story_image)
        val profileImageView = findViewById<ImageView>(R.id.profile_image)
        val nameTextView = findViewById<TextView>(R.id.name)
        val timeTextView = findViewById<TextView>(R.id.time_text)
        
        // Set story image from Base64
        if (story.imageUrl.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(story.imageUrl)
                storyImageView?.setImageBitmap(bitmap)
            } catch (e: Exception) {
                storyImageView?.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            storyImageView?.setImageResource(R.drawable.ic_default_profile)
        }
        
        // Set profile image from Base64
        if (story.userProfileImage.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(story.userProfileImage)
                profileImageView?.setImageBitmap(bitmap)
            } catch (e: Exception) {
                profileImageView?.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            profileImageView?.setImageResource(R.drawable.ic_default_profile)
        }
        
        // Set username
        nameTextView?.text = story.username
        
        // Set time (format: "Xh" or "Xd")
        val timeAgo = getTimeAgo(story.timestamp)
        timeTextView?.text = timeAgo
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        
        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            else -> "now"
        }
    }
    
    private fun setupCloseButton() {
        val closeButton = findViewById<Button>(R.id.storyEnd)
        closeButton?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
        
        // Also close on X button click
        val xButton = findViewById<TextView>(R.id.x_button)
        xButton?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }
    
    private fun setupProfileClick() {
        val profileImageView = findViewById<ImageView>(R.id.profile_image)
        profileImageView?.setOnClickListener {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (story.userId == currentUserId) {
                // Navigate to own profile
                val intent = Intent(this, OwnProfile::class.java)
                startActivity(intent)
            } else {
                // Navigate to user profile
                val intent = Intent(this, UserProfile::class.java)
                intent.putExtra("userId", story.userId)
                startActivity(intent)
            }
            finish()
            overridePendingTransition(0, 0)
        }
    }
    
    private fun markStoryAsViewed() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null && story.storyId.isNotEmpty()) {
            // Mark story as viewed by adding current user to viewers list
            FirebaseDatabase.getInstance().reference
                .child("stories")
                .child(story.storyId)
                .child("viewers")
                .child(currentUserId)
                .setValue(true)
        }
    }
}
