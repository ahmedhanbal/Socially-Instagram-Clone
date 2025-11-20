package com.hans.i221271_i220889

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import com.hans.i221271_i220889.utils.PostRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * Instagram-style Create Post Activity
 * - Select photos from gallery with grid view
 * - Add caption
 * - Upload with preview
 */
class CreatePostActivity : AppCompatActivity() {
    
    private lateinit var postRepository: PostRepository
    private var selectedImageUri: Uri? = null
    private lateinit var previewImageView: ImageView
    private lateinit var galleryRecyclerView: RecyclerView
    private val galleryImages = mutableListOf<Uri>()
    private lateinit var captionInput: EditText
    private val PERMISSION_REQUEST_CODE = 101
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        postRepository = PostRepository()
        
        if (checkPermissions()) {
            createInstagramStyleUI()
            loadGalleryImages()
        } else {
            requestPermissions()
        }
    }
    
    private fun checkPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createInstagramStyleUI()
            loadGalleryImages()
        } else {
            Toast.makeText(this, "Permission required to select photos", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun createInstagramStyleUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // ===== TOP BAR (Instagram Style) =====
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(15, 60, 15, 15)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val backButton = TextView(this).apply {
            text = "✕"
            textSize = 24f
            setTextColor(Color.BLACK)
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { finish() }
        }
        
        val title = TextView(this).apply {
            text = "New Post"
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }
        
        val shareButton = TextView(this).apply {
            text = "Share"
            textSize = 16f
            setTextColor(Color.parseColor("#0095F6")) // Instagram blue
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { createPost() }
        }
        
        topBar.addView(backButton)
        topBar.addView(title)
        topBar.addView(shareButton)
        
        // ===== PREVIEW & CAPTION SECTION =====
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(15, 15, 15, 15)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Selected image preview (square)
        previewImageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_default_profile)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                marginEnd = 15
            }
        }
        
        // Caption input
        captionInput = EditText(this).apply {
            hint = "Write a caption..."
            setHintTextColor(Color.parseColor("#999999"))
            setTextColor(Color.BLACK)
            textSize = 14f
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(10, 10, 10, 10)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
            gravity = Gravity.TOP
            minLines = 3
            maxLines = 5
        }
        
        contentLayout.addView(previewImageView)
        contentLayout.addView(captionInput)
        
        // Divider
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
        }
        
        // ===== GALLERY GRID =====
        val galleryLabel = TextView(this).apply {
            text = "Recents"
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(15, 15, 15, 10)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        galleryRecyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@CreatePostActivity, 3)
            setBackgroundColor(Color.WHITE)
            setPadding(2, 2, 2, 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        // Add all to main layout
        mainLayout.addView(topBar)
        mainLayout.addView(contentLayout)
        mainLayout.addView(divider)
        mainLayout.addView(galleryLabel)
        mainLayout.addView(galleryRecyclerView)
        
        setContentView(mainLayout)
    }
    
    private fun loadGalleryImages() {
        val imageUris = mutableListOf<Uri>()
        
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                imageUris.add(contentUri)
            }
        }
        
        galleryImages.clear()
        galleryImages.addAll(imageUris)
        
        // Set adapter
        galleryRecyclerView.adapter = GalleryAdapter(galleryImages) { uri ->
            selectedImageUri = uri
            previewImageView.setImageURI(uri)
        }
        
        // Auto-select first image
        if (galleryImages.isNotEmpty()) {
            selectedImageUri = galleryImages[0]
            previewImageView.setImageURI(galleryImages[0])
        }
    }
    
    private fun createPost() {
        val caption = captionInput.text.toString().trim()
        
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (caption.isEmpty()) {
            Toast.makeText(this, "Please add a caption", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Creating post...", Toast.LENGTH_SHORT).show()
            
            postRepository.createPost(this, selectedImageUri!!, caption) { success, postId ->
                runOnUiThread {
                    if (success) {
                    Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Gallery Adapter
    inner class GalleryAdapter(
        private val images: List<Uri>,
        private val onImageClick: (Uri) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(android.R.id.icon)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val imageView = ImageView(parent.context).apply {
                id = android.R.id.icon
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(2, 2, 2, 2)
                val size = parent.width / 3
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
            val container = FrameLayout(parent.context).apply {
                addView(imageView)
            }
            return ViewHolder(container)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = images[position]
            holder.imageView.setImageURI(uri)
            holder.itemView.setOnClickListener { onImageClick(uri) }
        }
        
        override fun getItemCount() = images.size
    }
}
