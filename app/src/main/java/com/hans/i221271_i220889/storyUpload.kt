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
import android.graphics.Paint
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.hans.i221271_i220889.utils.Base64Image
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.repositories.StoryRepository
import com.hans.i221271_i220889.offline.NetworkHelper
import com.hans.i221271_i220889.offline.OfflineHelper
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class storyUpload : AppCompatActivity() {
    private lateinit var storyRepository: StoryRepository
    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null
    private lateinit var previewImageView: ImageView
    private lateinit var galleryRecyclerView: RecyclerView
    private val galleryImages = mutableListOf<Uri>()
    private val PERMISSION_REQUEST_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize repositories
        storyRepository = StoryRepository(this)
        sessionManager = SessionManager(this)
        
        // Check and request permissions
        if (checkPermissions()) {
            createStoryUI()
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
            createStoryUI()
            loadGalleryImages()
        } else {
            Toast.makeText(this, "Permission required to access photos", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun createStoryUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // ===== TOP BAR =====
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 60, 20, 20)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val cancelButton = TextView(this).apply {
            text = "Cancel"
            textSize = 17f
            setTextColor(Color.parseColor("#FF3B30"))
            typeface = Typeface.DEFAULT
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { finish() }
        }
        
        val recentsButton = TextView(this).apply {
            text = "Recents ▼"
            textSize = 17f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val nextButton = TextView(this).apply {
            text = "Next"
            textSize = 17f
            setTextColor(Color.parseColor("#007AFF"))
            typeface = Typeface.DEFAULT
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { uploadStory() }
        }
        
        topBar.addView(cancelButton)
        topBar.addView(recentsButton)
        topBar.addView(nextButton)
        
        // ===== PREVIEW IMAGE (Top Half) =====
        val previewContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        previewImageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_default_profile)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Control buttons overlay
        val controlsOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        }
        
        val infinityBtn = TextView(this).apply {
            text = "∞"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(20, 10, 20, 10)
            setBackgroundColor(Color.parseColor("#80000000"))
        }
        
        val squareBtn = TextView(this).apply {
            text = "⬜"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(20, 10, 20, 10)
            setBackgroundColor(Color.parseColor("#80000000"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 15 }
        }
        
        val selectMultipleBtn = TextView(this).apply {
            text = "⬜  SELECT MULTIPLE"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.parseColor("#80000000"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 15 }
        }
        
        controlsOverlay.addView(infinityBtn)
        controlsOverlay.addView(squareBtn)
        controlsOverlay.addView(selectMultipleBtn)
        
        previewContainer.addView(previewImageView)
        previewContainer.addView(controlsOverlay)
        
        // ===== SCROLLABLE GALLERY GRID (Bottom Half) =====
        val galleryContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        // RecyclerView for gallery images
        galleryRecyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@storyUpload, 4)
            setBackgroundColor(Color.WHITE)
            setPadding(2, 10, 2, 10)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        galleryContainer.addView(galleryRecyclerView)
        
        // ===== BOTTOM TAB BAR =====
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 15, 0, 35)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val libraryTab = TextView(this).apply {
            text = "Library"
            textSize = 10f
            setTextColor(Color.parseColor("#A0522D"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val photoTab = TextView(this).apply {
            text = "Photo"
            textSize = 10f
            setTextColor(Color.parseColor("#A0522D"))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val videoTab = TextView(this).apply {
            text = "Video"
            textSize = 10f
            setTextColor(Color.parseColor("#A0522D"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                Toast.makeText(this@storyUpload, "Video support coming soon", Toast.LENGTH_SHORT).show()
            }
        }
        
        tabBar.addView(libraryTab)
        tabBar.addView(photoTab)
        tabBar.addView(videoTab)
        
        // Add all to main layout
        mainLayout.addView(topBar)
        mainLayout.addView(previewContainer)
        mainLayout.addView(galleryContainer)
        mainLayout.addView(tabBar)
        
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
            Picasso.get()
                .load(uri)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(previewImageView)
        }
        
        // Auto-select first image
        if (galleryImages.isNotEmpty()) {
            selectedImageUri = galleryImages[0]
            Picasso.get()
                .load(galleryImages[0])
                .fit()
                .centerCrop()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(previewImageView)
        }
    }
    
    private fun uploadStory() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }
        
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage("Uploading story...")
            setCancelable(false)
            show()
        }
        
        lifecycleScope.launch {
            try {
                if (NetworkHelper.isOnline(this@storyUpload)) {
                    // Online - upload directly
                    val result = storyRepository.createStory(selectedImageUri!!, "image")
                    
                    progressDialog.dismiss()
                    
                    result.onSuccess {
                        Toast.makeText(this@storyUpload, "Story uploaded! Expires in 24 hours", Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    }.onFailure { error ->
                        Toast.makeText(this@storyUpload, "Upload failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Offline - queue for later
                    progressDialog.dismiss()
                    
                    // Save to temp file for queue
                    val tempFile = java.io.File.createTempFile("story_", ".jpg", cacheDir)
                    contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val queueManager = OfflineHelper.getQueueManager(this@storyUpload)
                    queueManager.queueCreateStory(tempFile.absolutePath, "image")
                    
                    Toast.makeText(
                        this@storyUpload,
                        "Offline: Story queued, will upload when online",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@storyUpload, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val size = parent.width / 4
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
            val container = FrameLayout(parent.context).apply {
                addView(imageView)
            }
            return ViewHolder(container)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = images[position]
            Picasso.get()
                .load(uri)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(holder.imageView)
            holder.itemView.setOnClickListener { onImageClick(uri) }
        }
        
        override fun getItemCount() = images.size
    }
}
