package com.hans.i221271_i220889

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.hans.i221271_i220889.utils.Base64Image
import com.hans.i221271_i220889.repositories.ProfileRepository
import com.hans.i221271_i220889.network.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfilePictureUpdateActivity : AppCompatActivity() {
    private lateinit var profileRepository: ProfileRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var profileImageView: ImageView
    private var selectedImageUri: Uri? = null
    
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            profileImageView.setImageURI(it)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple UI programmatically
        createSimpleProfilePictureScreen()
        
        profileRepository = ProfileRepository(this)
        sessionManager = SessionManager(this)
    }
    
    private fun createSimpleProfilePictureScreen() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(50, 50, 50, 50)
        }
        
        val titleText = android.widget.TextView(this).apply {
            text = "Update Profile Picture"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#8e3f42"))
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 50
            }
        }
        
        profileImageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_default_profile)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = android.widget.LinearLayout.LayoutParams(200, 200).apply {
                gravity = android.view.Gravity.CENTER
                bottomMargin = 30
            }
        }
        
        val selectImageButton = android.widget.Button(this).apply {
            text = "Select Image"
            setBackgroundColor(android.graphics.Color.parseColor("#8e3f42"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                pickImage.launch("image/*")
            }
        }
        
        val updateButton = android.widget.Button(this).apply {
            text = "Update Profile Picture"
            setBackgroundColor(android.graphics.Color.parseColor("#8e3f42"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                updateProfilePicture()
            }
        }
        
        val backButton = android.widget.Button(this).apply {
            text = "← Back"
            setBackgroundColor(android.graphics.Color.parseColor("#8e3f42"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                finish()
            }
        }
        
        layout.addView(titleText)
        layout.addView(profileImageView)
        layout.addView(selectImageButton)
        layout.addView(updateButton)
        layout.addView(backButton)
        setContentView(layout)
    }
    
    private fun updateProfilePicture() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val result = profileRepository.uploadPicture(selectedImageUri!!, "profile_picture")
                result.onSuccess { imageUrl ->
                    Toast.makeText(this@ProfilePictureUpdateActivity, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { error ->
                    Toast.makeText(this@ProfilePictureUpdateActivity, "Failed to update: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfilePictureUpdateActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
