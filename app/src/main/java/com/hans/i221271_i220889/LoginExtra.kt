package com.hans.i221271_i220889

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.TextView


// Login screen where user can either log in or go back to sign up
class LoginExtra : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login2)

        // Handle status bar and navigation bar padding for proper UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Button to go back to signup screen
        val btnSignUp = findViewById<Button>(R.id.signupBtn2)
        btnSignUp.setOnClickListener {
            val intentSignup = Intent(this, signup::class.java)
            startActivity(intentSignup)
            finish()
        }

        // Receive username from signup activity
        val username = intent.getStringExtra("USERNAME_KEY") ?: ""
        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
        usernameTextView.text = username

        // Receive image Uri from signup activity
        val imageUriString = intent.getStringExtra("IMAGE_URI_KEY")
        val profileImageView = findViewById<ImageView>(R.id.profileImage)
        if (imageUriString!=null) {
            val imageUri = Uri.parse(imageUriString)
            profileImageView.setImageURI(imageUri)  // Display the received image
        }

        val btnLogin3 = findViewById<Button>(R.id.btnLogin)

        btnLogin3.setOnClickListener {
            val imageUri = (profileImageView.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?.let { android.provider.MediaStore.Images.Media.insertImage(contentResolver, it, "PROFILE_IMAGE_URI", null) }

            val intentHome = Intent(this, HomeScreen::class.java)

            intentHome.putExtra("PROFILE_IMAGE_URI", imageUri)

            intentHome.putExtra("USERNAME_KEY", username)

            intentHome.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intentHome)
            finish()
        }
    }

}
