package com.hans.i221271_i220889

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.offline.NetworkHelper
import com.hans.i221271_i220889.offline.OfflineHelper
import com.hans.i221271_i220889.repositories.PostRepositoryApi
import kotlinx.coroutines.launch

class CreatePostActivity : AppCompatActivity() {

    private lateinit var postRepository: PostRepositoryApi
    private lateinit var sessionManager: SessionManager
    private lateinit var captionInput: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var selectImageBtn: Button
    private lateinit var createPostBtn: Button
    private lateinit var backBtn: ImageButton

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedImageUri = uri
                imagePreview.setImageURI(uri)
                imagePreview.visibility = ImageView.VISIBLE
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openGallery()
        } else {
            Toast.makeText(this, "Permission required to select photos", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        postRepository = PostRepositoryApi(this)
        sessionManager = SessionManager(this)

        captionInput = findViewById(R.id.captionInput)
        imagePreview = findViewById(R.id.imagePreview)
        selectImageBtn = findViewById(R.id.selectImageBtn)
        createPostBtn = findViewById(R.id.createPostBtn)
        backBtn = findViewById(R.id.backBtn)

        backBtn.setOnClickListener { finish() }
        selectImageBtn.setOnClickListener { checkPermissionAndPickImage() }
        createPostBtn.setOnClickListener { createPost() }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "Please allow photo permission to continue", Toast.LENGTH_SHORT).show()
                permissionLauncher.launch(permission)
            }
            else -> permissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun createPost() {
        val caption = captionInput.text.toString().trim()

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        if (caption.isEmpty()) {
            Toast.makeText(this, "Please add a caption", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Creating post...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                if (NetworkHelper.isOnline(this@CreatePostActivity)) {
                    val result = postRepository.createPost(
                        caption = caption,
                        mediaUri = selectedImageUri,
                        mediaType = "image"
                    )

                    progressDialog.dismiss()

                    result.onSuccess {
                        Toast.makeText(this@CreatePostActivity, "Post created!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }.onFailure { error ->
                        Toast.makeText(this@CreatePostActivity, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressDialog.dismiss()

                    val tempFile = java.io.File.createTempFile("post_", ".jpg", cacheDir)
                    contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }

                    val queueManager = OfflineHelper.getQueueManager(this@CreatePostActivity)
                    queueManager.queueCreatePost(caption, tempFile.absolutePath, "image")

                    Toast.makeText(
                        this@CreatePostActivity,
                        "Offline: Post queued, will upload when online",
                        Toast.LENGTH_LONG
                    ).show()

                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@CreatePostActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
