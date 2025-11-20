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
import com.hans.i221271_i220889.network.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    
    // Image picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sendImage(it) }
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
        
        // Get chat ID from intent or generate one
        android.util.Log.d("Chat", "onCreate: Getting intent extras")
        val otherUserId = intent.getStringExtra("userId")
        val personName = intent.getStringExtra("PersonName")
        val currentUserId = sessionManager.getSession()?.userId?.toString()
        
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
                if (checkPermissions()) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    requestPermissions()
                }
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
            messageAdapter = MessageAdapter(messages) { message ->
                showMessageOptionsDialog(message)
            }
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
                val currentUserId = sessionManager.getSession()?.userId?.toString() ?: ""
                messageDataList.forEach { msgData ->
                    messages.add(ChatMessage(
                        messageId = msgData.id.toString(),
                        senderId = msgData.senderId.toString(),
                        receiverId = msgData.receiverId.toString(),
                        message = msgData.content,
                        imageBase64 = msgData.mediaUrl ?: "",
                        postId = null,
                        timestamp = System.currentTimeMillis(),
                        isEdited = false,
                        isDeleted = false,
                        isSentByCurrentUser = msgData.senderId.toString() == currentUserId
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
            val result = messageRepository.sendMessage(receiverId, text)
            result.onSuccess {
                messageInput.text.clear()
                loadMessages() // Refresh messages
            }.onFailure { error ->
                Toast.makeText(this@Chat, "Failed to send message: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendImage(imageUri: Uri) {
        Toast.makeText(this, "Sending image...", Toast.LENGTH_SHORT).show()
        val receiverId = otherUserIdForProfile?.toIntOrNull()
        if (receiverId == null) {
            Toast.makeText(this, "Invalid receiver ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    val result = messageRepository.sendMessage(receiverId, "", base64Image)
                    result.onSuccess {
                        Toast.makeText(this@Chat, "Image sent", Toast.LENGTH_SHORT).show()
                        loadMessages() // Refresh messages
                    }.onFailure { error ->
                        Toast.makeText(this@Chat, "Failed to send image: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Chat, "Failed to read image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Chat, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
        chatRepository.sendPost(chatId, postId) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Post shared", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to share post", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showMessageOptionsDialog(message: ChatMessage) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
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
                    chatRepository.editMessage(chatId, message.messageId, newText) { success ->
                        runOnUiThread {
                            if (success) {
                                Toast.makeText(this, "Message edited", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to edit (5 minute limit)", Toast.LENGTH_SHORT).show()
                            }
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
                chatRepository.deleteMessage(chatId, message.messageId) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to delete (5 minute limit)", Toast.LENGTH_SHORT).show()
                        }
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
    
    private fun startCall(callType: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val otherUserId = otherUserIdForProfile
        val otherUserName = intent.getStringExtra("PersonName") ?: "User"
        
        if (currentUserId == null || otherUserId == null) {
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentUserName = intent.getStringExtra("PersonName") ?: "User"
        
        // Get current user's profile image
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUserId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val user = userSnapshot.getValue(com.hans.i221271_i220889.models.User::class.java)
                val callerUsername = user?.username ?: currentUserName
                val callerProfileImage = user?.profileImageBase64 ?: ""
                
                val callTypeText = if (callType == "video") "video call" else "voice call"
                
                // Save notification to database
                val notificationId = FirebaseDatabase.getInstance().reference
                    .child("notifications")
                    .child(recipientId)
                    .push()
                    .key ?: return@addOnSuccessListener
                
                val notificationData = mapOf(
                    "notificationId" to notificationId,
                    "type" to "call",
                    "fromUserId" to currentUserId,
                    "fromUsername" to callerUsername,
                    "fromUserProfileImage" to callerProfileImage,
                    "title" to "Incoming $callTypeText",
                    "body" to "$callerUsername is calling you",
                    "timestamp" to System.currentTimeMillis(),
                    "channelName" to channelName,
                    "callType" to callType,
                    "isRead" to false
                )
                
                FirebaseDatabase.getInstance().reference
                    .child("notifications")
                    .child(recipientId)
                    .child(notificationId)
                    .setValue(notificationData)
                
                // Send FCM push notification with call data
                // Note: In production, you'd send this via Firebase Cloud Functions
                // For now, the notification is saved to database and will be shown when app is opened
                android.util.Log.d("Chat", "Call notification saved to database for user: $recipientId")
            }
    }
    
    private fun loadProfileImage() {
        val profileImageView = findViewById<ImageView>(R.id.profileImageView)
        val userId = otherUserIdForProfile
        
        if (userId == null || profileImageView == null) {
            android.util.Log.w("Chat", "loadProfileImage: userId or profileImageView is null")
            return
        }
        
        android.util.Log.d("Chat", "loadProfileImage: Loading profile image for userId=$userId")
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("profileImageBase64")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profileImageBase64 = snapshot.getValue(String::class.java) ?: ""
                    if (profileImageBase64.isNotEmpty()) {
                        try {
                            val bitmap = com.hans.i221271_i220889.utils.Base64Image.base64ToBitmap(profileImageBase64)
                            if (bitmap != null) {
                                runOnUiThread {
                                    profileImageView.setImageBitmap(bitmap)
                                    android.util.Log.d("Chat", "loadProfileImage: Profile image loaded successfully")
                                }
                            } else {
                                runOnUiThread {
                                    profileImageView.setImageResource(R.drawable.ic_default_profile)
                                    android.util.Log.w("Chat", "loadProfileImage: Failed to decode bitmap, using default")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Chat", "loadProfileImage: Error decoding profile image: ${e.message}", e)
                            runOnUiThread {
                                profileImageView.setImageResource(R.drawable.ic_default_profile)
                            }
                        }
                    } else {
                        android.util.Log.d("Chat", "loadProfileImage: No profile image found, using default")
                        runOnUiThread {
                            profileImageView.setImageResource(R.drawable.ic_default_profile)
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("Chat", "loadProfileImage: Failed to load profile image: ${error.message}")
                    runOnUiThread {
                        profileImageView.setImageResource(R.drawable.ic_default_profile)
                    }
                }
            })
    }
}
