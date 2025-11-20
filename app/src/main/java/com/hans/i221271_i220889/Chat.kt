package com.hans.i221271_i220889

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.MessageAdapter
import com.hans.i221271_i220889.utils.ChatMessage
import com.hans.i221271_i220889.repositories.MessageRepository
import com.hans.i221271_i220889.repositories.SearchRepository
import com.hans.i221271_i220889.network.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Instagram-style Chat Activity
 * Features:
 * - Send text messages
 * - Send images (Base64)
 * - Real-time message updates (polling-based)
 */
class Chat : AppCompatActivity() {
    private lateinit var messageRepository: MessageRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var chatId: String
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private val PERMISSION_REQUEST_CODE = 102
    private var otherUserIdForProfile: String? = null
    private var isLoadingMessages = false
    private var screenshotDetector: com.hans.i221271_i220889.utils.ScreenshotDetector? = null
    private lateinit var searchRepository: SearchRepository
    private var statusUpdateJob: kotlinx.coroutines.Job? = null
    private var statusCheckJob: kotlinx.coroutines.Job? = null
    
    // Image picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sendImage(it) }
    }
    
    // File picker (for documents, etc.)
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sendFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("Chat", "onCreate: Starting Chat activity")
        
        try {
            android.util.Log.d("Chat", "onCreate: Setting content view")
            setContentView(R.layout.activity_chat)
            android.util.Log.d("Chat", "onCreate: Content view set successfully")
        } catch (e: Exception) {
            android.util.Log.e("Chat", "onCreate: Error setting content view: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
        
        try {
            android.util.Log.d("Chat", "onCreate: Setting up window insets")
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            android.util.Log.d("Chat", "onCreate: Window insets set successfully")
        } catch (e: Exception) {
            android.util.Log.e("Chat", "onCreate: Error setting window insets: ${e.message}", e)
            e.printStackTrace()
        }

        // Initialize repositories
        sessionManager = SessionManager(this)
        messageRepository = MessageRepository(this)
        searchRepository = SearchRepository(this)
        
        // Get chat ID from intent or generate one
        android.util.Log.d("Chat", "onCreate: Getting intent extras")
        val otherUserId = intent.getStringExtra("userId")
        val personName = intent.getStringExtra("PersonName")
        val currentUserId = sessionManager.getUserId().toString()
        
        android.util.Log.d("Chat", "onCreate: Intent data - otherUserId=$otherUserId, personName=$personName, currentUserId=$currentUserId")
        
        if (otherUserId == null || otherUserId.isEmpty() || currentUserId == null) {
            android.util.Log.e("Chat", "onCreate: Invalid user or not logged in: otherUserId=$otherUserId, currentUserId=$currentUserId")
            Toast.makeText(this, "Error: Invalid user or not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        if (otherUserId == currentUserId) {
            android.util.Log.w("Chat", "onCreate: User trying to chat with themselves")
            Toast.makeText(this, "Cannot chat with yourself", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        chatId = generateChatId(currentUserId, otherUserId)
        android.util.Log.d("Chat", "onCreate: Chat initialized - chatId=$chatId, otherUserId=$otherUserId, currentUserId=$currentUserId")
        
        // Store otherUserId for profile image loading
        otherUserIdForProfile = otherUserId
        
        try {
            android.util.Log.d("Chat", "onCreate: Starting setupUI()")
            setupUI()
            android.util.Log.d("Chat", "onCreate: setupUI() completed successfully")
            
            android.util.Log.d("Chat", "onCreate: Starting setupMessagesRecyclerView()")
            setupMessagesRecyclerView()
            android.util.Log.d("Chat", "onCreate: setupMessagesRecyclerView() completed successfully")
            
            android.util.Log.d("Chat", "onCreate: Starting loadMessagesRealTime()")
            loadMessagesRealTime()
            android.util.Log.d("Chat", "onCreate: loadMessagesRealTime() completed successfully")
            
            // Start screenshot detection
            android.util.Log.d("Chat", "onCreate: Starting screenshot detection")
            startScreenshotDetection()
            android.util.Log.d("Chat", "onCreate: Screenshot detection started")
            
            // Start status updates
            android.util.Log.d("Chat", "onCreate: Starting status updates")
            startStatusUpdates()
            startStatusPolling()
            android.util.Log.d("Chat", "onCreate: Status updates started")
            
            android.util.Log.d("Chat", "onCreate: All setup completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("Chat", "onCreate: Error setting up chat: ${e.message}", e)
            android.util.Log.e("Chat", "onCreate: Error class: ${e.javaClass.name}")
            android.util.Log.e("Chat", "onCreate: Error stack trace:")
            e.printStackTrace()
            Toast.makeText(this, "Error setting up chat: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun generateChatId(userId1: String, userId2: String): String {
        // Consistent chat ID regardless of who initiates
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    private fun setupUI() {
        try {
            android.util.Log.d("Chat", "setupUI: Starting")
            
            // Set person name
            android.util.Log.d("Chat", "setupUI: Getting person name from intent")
            val personName = intent.getStringExtra("PersonName") ?: "Chat"
            android.util.Log.d("Chat", "setupUI: Person name = $personName")
            
            android.util.Log.d("Chat", "setupUI: Finding personNameTextView")
            val personNameTextView = findViewById<TextView>(R.id.personNameTextView)
            android.util.Log.d("Chat", "setupUI: personNameTextView found: ${personNameTextView != null}")
            personNameTextView?.text = personName
            android.util.Log.d("Chat", "setupUI: Person name set")
            
            // Load profile image
            android.util.Log.d("Chat", "setupUI: Loading profile image")
            loadProfileImage()

            // Message input - use requireNotNull to throw if not found
            android.util.Log.d("Chat", "setupUI: Finding messageInput")
            val messageInputView = findViewById<EditText>(R.id.messageInput)
            android.util.Log.d("Chat", "setupUI: messageInput found: ${messageInputView != null}")
            if (messageInputView == null) {
                throw IllegalStateException("messageInput view not found in layout")
            }
            messageInput = messageInputView
            
            android.util.Log.d("Chat", "setupUI: Finding sendButton")
            val sendButtonView = findViewById<ImageButton>(R.id.sendButton)
            android.util.Log.d("Chat", "setupUI: sendButton found: ${sendButtonView != null}")
            if (sendButtonView == null) {
                throw IllegalStateException("sendButton view not found in layout")
            }
            sendButton = sendButtonView
            android.util.Log.d("Chat", "setupUI: Required views found")
            
            android.util.Log.d("Chat", "setupUI: Setting up sendButton click listener")
            sendButton.setOnClickListener {
                val message = messageInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendTextMessage(message)
                    messageInput.setText("")
                }
            }
            android.util.Log.d("Chat", "setupUI: Send button listener set")

            // Gallery functionality moved to camera button

            // Camera button (it's an ImageView, not ImageButton)
            android.util.Log.d("Chat", "setupUI: Finding camera button")
            val cameraButton = findViewById<ImageView>(R.id.btnCamera)
            android.util.Log.d("Chat", "setupUI: camera button found: ${cameraButton != null}")
            cameraButton?.setOnClickListener {
                // Show dialog to choose between image or file
                showMediaPickerDialog()
            }
            cameraButton?.isClickable = true
            cameraButton?.isFocusable = true
            android.util.Log.d("Chat", "setupUI: Camera button listener set")
            
            // Call buttons
            android.util.Log.d("Chat", "setupUI: Finding call buttons")
            
            // Voice call button
            val voiceCallBtn = findViewById<ImageButton>(R.id.voiceCallBtn)
            android.util.Log.d("Chat", "setupUI: voiceCallBtn found: ${voiceCallBtn != null}")
            voiceCallBtn?.setOnClickListener {
                startCall("voice")
            }
            
            // Video call button
            val videoCallBtn = findViewById<ImageButton>(R.id.videoCallBtn)
            android.util.Log.d("Chat", "setupUI: videoCallBtn found: ${videoCallBtn != null}")
            videoCallBtn?.setOnClickListener {
                startCall("video")
            }
            android.util.Log.d("Chat", "setupUI: Call buttons listeners set")
            
            android.util.Log.d("Chat", "setupUI: Completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("Chat", "setupUI: Error in setupUI: ${e.message}", e)
            android.util.Log.e("Chat", "setupUI: Error class: ${e.javaClass.name}")
            android.util.Log.e("Chat", "setupUI: Error stack trace:")
            e.printStackTrace()
            throw e // Re-throw to be caught by onCreate's try-catch
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

    private fun setupMessagesRecyclerView() {
        try {
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Starting")
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Finding messagesRecyclerView")
            messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
            android.util.Log.d("Chat", "setupMessagesRecyclerView: messagesRecyclerView found: ${messagesRecyclerView != null}")
            if (messagesRecyclerView == null) {
                throw IllegalStateException("messagesRecyclerView not found in layout")
            }
            
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Creating MessageAdapter")
            val currentUserId = sessionManager.getUserId().toString()
            messageAdapter = MessageAdapter(messages, currentUserId, 
                onMessageLongClick = { message ->
                    showMessageOptionsDialog(message)
                },
                onFileClick = { message ->
                    openFile(message)
                }
            )
            android.util.Log.d("Chat", "setupMessagesRecyclerView: MessageAdapter created")
            
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Setting layout manager")
            messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
                stackFromEnd = true // Start from bottom
            }
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Layout manager set")
            
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Setting adapter")
            messagesRecyclerView.adapter = messageAdapter
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Adapter set")
            android.util.Log.d("Chat", "setupMessagesRecyclerView: Completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("Chat", "setupMessagesRecyclerView: Error: ${e.message}", e)
            android.util.Log.e("Chat", "setupMessagesRecyclerView: Error class: ${e.javaClass.name}")
            e.printStackTrace()
            throw e
        }
    }

    private fun loadMessagesRealTime() {
        // Simulate real-time updates with periodic polling
        lifecycleScope.launch {
            while (true) {
                if (!isLoadingMessages) {
                    loadMessages()
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }
    
    private fun loadMessages() {
        if (isLoadingMessages) return
        isLoadingMessages = true
        
        val receiverId = otherUserIdForProfile?.toIntOrNull()
        if (receiverId == null) {
            isLoadingMessages = false
            return
        }
        
        lifecycleScope.launch {
            val result = messageRepository.getMessages(receiverId)
            result.onSuccess { messageDataList ->
                messages.clear()
                val currentUserId = sessionManager.getUserId().toString()
                messageDataList.forEach { msgData ->
                    // Determine message type based on media
                    val messageType = when {
                        msgData.mediaUrl.isNullOrEmpty() -> "text"
                        msgData.mediaType == "video" -> "video"
                        msgData.mediaType == "file" -> "file"
                        else -> "image" // Default to image for images
                    }
                    
                    messages.add(ChatMessage(
                        messageId = msgData.id.toString(),
                        chatId = chatId,
                        senderId = msgData.senderId.toString(),
                        type = messageType,
                        content = msgData.messageText ?: "",
                        mediaUrl = msgData.mediaUrl,
                        mediaType = msgData.mediaType,
                        timestamp = try {
                            // Try to parse created_at timestamp, fallback to current time
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(msgData.createdAt)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    ))
                }
                messageAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
            }.onFailure { error ->
                android.util.Log.e("Chat", "Failed to load messages: ${error.message}")
            }
            isLoadingMessages = false
        }
    }

    private fun sendTextMessage(text: String) {
        val receiverId = otherUserIdForProfile?.toIntOrNull()
        if (receiverId == null) {
            Toast.makeText(this, "Invalid receiver ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val result = messageRepository.sendTextMessage(receiverId, text)
            result.onSuccess {
                messageInput.text.clear()
                loadMessages() // Refresh messages
            }.onFailure { error ->
                Toast.makeText(this@Chat, "Failed to send message: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMediaPickerDialog() {
        val options = arrayOf("Image/Video", "File (PDF, Word, etc.)")
        AlertDialog.Builder(this)
            .setTitle("Select Media Type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Image/Video picker
                        if (checkPermissions()) {
                            imagePickerLauncher.launch("*/*") // Allow both images and videos
                        } else {
                            requestPermissions()
                        }
                    }
                    1 -> {
                        // File picker
                        filePickerLauncher.launch("*/*")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun sendImage(imageUri: Uri) {
        Toast.makeText(this, "Sending media...", Toast.LENGTH_SHORT).show()
        val receiverId = otherUserIdForProfile?.toIntOrNull()
        if (receiverId == null) {
            Toast.makeText(this, "Invalid receiver ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            // Determine media type from URI
            val mimeType = contentResolver.getType(imageUri) ?: ""
            val mediaType = when {
                mimeType.startsWith("image/") -> "image"
                mimeType.startsWith("video/") -> "video"
                else -> "file"
            }
            
            val result = messageRepository.sendMediaMessage(receiverId, imageUri, mediaType)
            result.onSuccess {
                Toast.makeText(this@Chat, "Media sent", Toast.LENGTH_SHORT).show()
                loadMessages() // Refresh messages
            }.onFailure { error ->
                Toast.makeText(this@Chat, "Failed to send: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun sendFile(fileUri: Uri) {
        Toast.makeText(this, "Sending file...", Toast.LENGTH_SHORT).show()
        val receiverId = otherUserIdForProfile?.toIntOrNull()
        if (receiverId == null) {
            Toast.makeText(this, "Invalid receiver ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val result = messageRepository.sendMediaMessage(receiverId, fileUri, "file")
            result.onSuccess {
                Toast.makeText(this@Chat, "File sent", Toast.LENGTH_SHORT).show()
                loadMessages() // Refresh messages
            }.onFailure { error ->
                Toast.makeText(this@Chat, "Failed to send file: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showSharePostDialog() {
        // Show dialog to select a post to share
        AlertDialog.Builder(this)
            .setTitle("Share Post")
            .setMessage("Post sharing feature - select from your recent posts")
            .setPositiveButton("Select") { _, _ ->
                // In a real app, show a list of posts to select from
                val postId = "sample_post_id"
                sharePost(postId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun sharePost(postId: String) {
        // Post sharing functionality - to be implemented
        Toast.makeText(this, "Post sharing not yet implemented", Toast.LENGTH_SHORT).show()
    }

    private fun showMessageOptionsDialog(message: ChatMessage) {
        val currentUserId = sessionManager.getUserId().toString()
        
        // Only allow editing/deleting own messages
        if (message.senderId != currentUserId) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceMessage = currentTime - message.timestamp
        val fiveMinutes = 5 * 60 * 1000L
        
        // Check if within 5 minutes
        val canEdit = timeSinceMessage <= fiveMinutes && message.type == "text"
        val canDelete = timeSinceMessage <= fiveMinutes

        val options = mutableListOf<String>()
        if (canEdit) options.add("Edit")
        if (canDelete) options.add("Delete")
        if (message.type == "text") options.add("Copy")

        if (options.isEmpty()) {
            Toast.makeText(this, "No actions available (5 minute limit expired)", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Edit" -> editMessage(message)
                    "Delete" -> deleteMessage(message)
                    "Copy" -> copyMessage(message)
                }
            }
            .show()
    }

    private fun editMessage(message: ChatMessage) {
        val editText = EditText(this).apply {
            setText(message.content)
            setSelection(message.content.length)
            setPadding(20, 20, 20, 20)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    lifecycleScope.launch {
                        val result = messageRepository.editMessage(message.messageId.toIntOrNull() ?: return@launch, newText)
                        result.onSuccess {
                            Toast.makeText(this@Chat, "Message edited", Toast.LENGTH_SHORT).show()
                            loadMessages()
                        }.onFailure { error ->
                            Toast.makeText(this@Chat, "Failed to edit: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(message: ChatMessage) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val result = messageRepository.deleteMessage(message.messageId.toIntOrNull() ?: return@launch)
                    result.onSuccess {
                        Toast.makeText(this@Chat, "Message deleted", Toast.LENGTH_SHORT).show()
                        loadMessages()
                    }.onFailure { error ->
                        Toast.makeText(this@Chat, "Failed to delete: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyMessage(message: ChatMessage) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Message", message.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show()
    }
    
    private fun openFile(message: ChatMessage) {
        val mediaUrl = message.mediaUrl ?: return
        
        lifecycleScope.launch {
            try {
                Toast.makeText(this@Chat, "Opening file...", Toast.LENGTH_SHORT).show()
                
                // Determine file URL
                val fileUrl = if (mediaUrl.startsWith("http")) {
                    mediaUrl
                } else {
                    com.hans.i221271_i220889.network.ApiConfig.BASE_URL + mediaUrl
                }
                
                // Download and open file
                val file = downloadAndCacheFile(fileUrl, message.mediaType ?: message.type)
                if (file != null && file.exists()) {
                    openFileWithIntent(file, message.mediaType ?: message.type)
                } else {
                    Toast.makeText(this@Chat, "Failed to open file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("Chat", "Error opening file: ${e.message}", e)
                Toast.makeText(this@Chat, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun downloadAndCacheFile(url: String, mediaType: String): File? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            // Determine file extension
            val extension = when (mediaType) {
                "image" -> {
                    when {
                        url.contains(".png", ignoreCase = true) -> ".png"
                        url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) -> ".jpg"
                        url.contains(".gif", ignoreCase = true) -> ".gif"
                        url.contains(".webp", ignoreCase = true) -> ".webp"
                        else -> ".jpg"
                    }
                }
                "video" -> {
                    when {
                        url.contains(".mp4", ignoreCase = true) -> ".mp4"
                        url.contains(".3gp", ignoreCase = true) -> ".3gp"
                        url.contains(".mkv", ignoreCase = true) -> ".mkv"
                        else -> ".mp4"
                    }
                }
                "file" -> {
                    val ext = url.substringAfterLast(".", "")
                    if (ext.isNotEmpty() && ext.length <= 5) {
                        ".$ext"
                    } else {
                        ".tmp"
                    }
                }
                else -> ".tmp"
            }
            
            val fileName = "chat_file_${System.currentTimeMillis()}$extension"
            val file = File(cacheDir, fileName)
            
            response.body?.byteStream()?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            file
        } catch (e: Exception) {
            android.util.Log.e("Chat", "Error downloading file: ${e.message}", e)
            null
        }
    }
    
    private fun openFileWithIntent(file: File, mediaType: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = when (mediaType) {
                "image" -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                "video" -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                "file" -> {
                    // Determine MIME type from file extension
                    val mimeType = when (file.extension.lowercase()) {
                        "pdf" -> "application/pdf"
                        "doc", "docx" -> "application/msword"
                        "xls", "xlsx" -> "application/vnd.ms-excel"
                        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                        "txt" -> "text/plain"
                        "zip", "rar" -> "application/zip"
                        else -> "*/*"
                    }
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                else -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            }
            
            // Add chooser to let user select app
            val chooser = Intent.createChooser(intent, "Open with")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("Chat", "Error opening file with intent: ${e.message}", e)
            Toast.makeText(this, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startCall(callType: String) {
        val currentUserId = sessionManager.getUserId().toString()
        val otherUserId = otherUserIdForProfile
        val otherUserName = intent.getStringExtra("PersonName") ?: "User"
        
        if (otherUserId == null) {
            Toast.makeText(this, "Error: Invalid user", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Generate consistent channel name based on user IDs
        val channelName = if (currentUserId < otherUserId) {
            "${currentUserId}_${otherUserId}"
        } else {
            "${otherUserId}_${currentUserId}"
        }
        
        // Send call notification to the other user
        sendCallNotification(otherUserId, channelName, callType, otherUserName)
        
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("channelName", channelName)
            putExtra("callType", callType)
            putExtra("isIncomingCall", false)
            putExtra("otherUserId", otherUserId)
            putExtra("otherUserName", otherUserName)
        }
        startActivity(intent)
    }
    
    private fun sendCallNotification(recipientId: String, channelName: String, callType: String, recipientName: String) {
        val currentUserId = sessionManager.getUserId().toString()
        val currentUserName = sessionManager.getUsername() ?: "User"
        
        // Get current user's profile data from backend
        lifecycleScope.launch {
            try {
                val profileResult = com.hans.i221271_i220889.repositories.ProfileRepository(this@Chat).getOwnProfile()
                val callerUsername = if (profileResult.isSuccess) {
                    profileResult.getOrNull()?.username ?: currentUserName
                } else {
                    currentUserName
                }
                
                val callTypeText = if (callType == "video") "video call" else "voice call"
                
                // Send notification via backend API
                // Note: This would use NotificationRepository when implemented
                android.util.Log.d("Chat", "Call notification: $callerUsername calling $recipientName ($callTypeText)")
                
                // For now, the Agora call will still work - notification system can be implemented later
            } catch (e: Exception) {
                android.util.Log.e("Chat", "Error sending call notification: ${e.message}")
            }
        }
    }
    
    private fun loadProfileImage() {
        val profileImageView = findViewById<ImageView>(R.id.profileImageView)
        val userId = otherUserIdForProfile
        
        if (userId == null || profileImageView == null) {
            android.util.Log.w("Chat", "loadProfileImage: userId or profileImageView is null")
            return
        }
        
        // Load profile image from ProfileRepository
        lifecycleScope.launch {
            try {
                val userIdInt = userId.toIntOrNull() ?: return@launch
                val result = com.hans.i221271_i220889.repositories.ProfileRepository(this@Chat).getProfile(userIdInt)
                result.onSuccess { profile ->
                    runOnUiThread {
                        if (!profile.profilePicture.isNullOrEmpty()) {
                            val imageUrl = com.hans.i221271_i220889.network.ApiConfig.BASE_URL + profile.profilePicture
                            com.squareup.picasso.Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(profileImageView)
                            android.util.Log.d("Chat", "loadProfileImage: Loaded profile image from URL: $imageUrl")
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile)
                            android.util.Log.d("Chat", "loadProfileImage: Using default profile image (no profile picture)")
                        }
                    }
                }.onFailure { error ->
                    android.util.Log.e("Chat", "loadProfileImage: Failed to load profile: ${error.message}")
                    runOnUiThread {
                        profileImageView.setImageResource(R.drawable.ic_default_profile)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Chat", "loadProfileImage: Error: ${e.message}", e)
                runOnUiThread {
                    profileImageView.setImageResource(R.drawable.ic_default_profile)
                }
            }
        }
    }
    
    private fun startScreenshotDetection() {
        val otherUserId = otherUserIdForProfile?.toIntOrNull()
        if (otherUserId != null) {
            screenshotDetector = com.hans.i221271_i220889.utils.ScreenshotDetector(
                this,
                this,
                otherUserId
            )
            screenshotDetector?.startDetection()
            android.util.Log.d("Chat", "Screenshot detection started for user: $otherUserId")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure detection is running
        if (screenshotDetector == null) {
            startScreenshotDetection()
        }
        // Update status to online
        updateOnlineStatus(true)
        // Refresh status check
        checkUserStatus()
    }
    
    override fun onPause() {
        super.onPause()
        // Update status to offline when leaving chat
        updateOnlineStatus(false)
    }
    
    private fun startStatusUpdates() {
        // Update status to online when entering chat
        updateOnlineStatus(true)
    }
    
    private fun updateOnlineStatus(isOnline: Boolean) {
        lifecycleScope.launch {
            try {
                val result = searchRepository.updateOnlineStatus(isOnline)
                result.onFailure { error ->
                    android.util.Log.e("Chat", "Failed to update status: ${error.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("Chat", "Error updating status: ${e.message}")
            }
        }
    }
    
    private fun startStatusPolling() {
        statusCheckJob = lifecycleScope.launch {
            while (true) {
                delay(5000) // Check every 5 seconds
                checkUserStatus()
            }
        }
    }
    
    private fun checkUserStatus() {
        val otherUserId = otherUserIdForProfile?.toIntOrNull() ?: return
        
        lifecycleScope.launch {
            try {
                val result = searchRepository.getUsersStatus(listOf(otherUserId))
                result.onSuccess { statusList ->
                    statusList.firstOrNull()?.let { status ->
                        updateStatusIndicator(status.isOnline)
                    }
                }.onFailure { error ->
                    android.util.Log.e("Chat", "Failed to get status: ${error.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("Chat", "Error checking status: ${e.message}")
            }
        }
    }
    
    private fun updateStatusIndicator(isOnline: Boolean) {
        runOnUiThread {
            try {
                val personNameTextView = findViewById<TextView>(R.id.personNameTextView)
                val currentText = personNameTextView?.text?.toString() ?: ""
                // Remove any existing status indicator
                val nameWithoutStatus = currentText.split(" • ").firstOrNull() ?: currentText
                val statusText = if (isOnline) " • Online" else " • Offline"
                personNameTextView?.text = "$nameWithoutStatus$statusText"
            } catch (e: Exception) {
                android.util.Log.e("Chat", "Error updating status indicator: ${e.message}")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        statusUpdateJob?.cancel()
        statusCheckJob?.cancel()
        updateOnlineStatus(false)
        screenshotDetector?.stopDetection()
        screenshotDetector = null
        android.util.Log.d("Chat", "Screenshot detection stopped")
    }
}
