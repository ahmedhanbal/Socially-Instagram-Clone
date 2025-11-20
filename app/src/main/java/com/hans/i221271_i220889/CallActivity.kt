package com.hans.i221271_i220889

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hans.i221271_i220889.utils.AgoraCallManager
import com.hans.i221271_i220889.utils.Base64Image
import android.view.SurfaceView
import android.widget.FrameLayout

class CallActivity : AppCompatActivity() {
    
    private lateinit var localVideoView: FrameLayout
    private lateinit var remoteVideoView: FrameLayout
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var callStatusText: TextView
    private lateinit var endCallBtn: ImageButton
    
    private var channelName: String = ""
    private var callType: String = "voice"
    private var isIncomingCall: Boolean = false
    private var otherUserId: String? = null
    private var otherUserName: String? = ""
    
    private val callManager = AgoraCallManager.getInstance()
    private val PERMISSION_REQUEST_CODE = 200
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Get intent extras
        channelName = intent.getStringExtra("channelName") ?: "default_channel"
        callType = intent.getStringExtra("callType") ?: "voice"
        isIncomingCall = intent.getBooleanExtra("isIncomingCall", false)
        otherUserId = intent.getStringExtra("otherUserId")
        otherUserName = intent.getStringExtra("otherUserName") ?: "User"
        
        initializeViews()
        setupCallManager()
        requestPermissions()
    }
    
    private fun initializeViews() {
        localVideoView = findViewById(R.id.localVideoView)
        remoteVideoView = findViewById(R.id.remoteVideoView)
        profileImageView = findViewById(R.id.image)
        nameTextView = findViewById(R.id.name)
        callStatusText = findViewById(R.id.callStatusText)
        endCallBtn = findViewById(R.id.callEnd)
        
        // Set user name
        nameTextView.text = otherUserName
        
        // Load profile image
        loadProfileImage()
        
        // Set initial status
        if (isIncomingCall) {
            callStatusText.text = "Incoming call..."
            // For incoming calls, wait for user to accept before joining
        } else {
            callStatusText.text = "Calling..."
            // For outgoing calls, join immediately after permissions
        }
        
        // Show/hide video views based on call type
        if (callType == "voice") {
            localVideoView.visibility = View.GONE
            remoteVideoView.visibility = View.GONE
            profileImageView.visibility = View.VISIBLE
        } else {
            profileImageView.visibility = View.GONE
        }
        
        setupClickListeners()
        
        // For incoming calls, show accept/reject buttons
        if (isIncomingCall) {
            setupIncomingCallUI()
        }
    }
    
    private fun setupIncomingCallUI() {
        // Add accept and reject buttons for incoming calls
        // These can be added to the layout or created programmatically
        // For now, the user can accept by clicking the notification which opens CallActivity
        // and they can reject by clicking end call button
    }
    
    private fun setupCallManager() {
        // Initialize Agora
        if (!callManager.initialize(this)) {
            Toast.makeText(this, "Failed to initialize call service", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Set up callbacks
        callManager.setOnJoinChannelSuccessCallback { channel, uid, elapsed ->
            runOnUiThread {
                callStatusText.text = "Connected"
                if (callType == "video") {
                    setupLocalVideo()
                }
            }
        }
        
        callManager.setOnUserJoinedCallback { uid ->
            runOnUiThread {
                callStatusText.text = "User joined"
                if (callType == "video") {
                    setupRemoteVideo(uid)
                }
            }
        }
        
        callManager.setOnUserOfflineCallback { uid ->
            runOnUiThread {
                callStatusText.text = "User left"
                endCall()
            }
        }
        
        callManager.setOnErrorCallback { err, msg ->
            runOnUiThread {
                Toast.makeText(this, "Call error: $msg", Toast.LENGTH_SHORT).show()
                android.util.Log.e("CallActivity", "Agora error: $err, $msg")
            }
        }
        
        callManager.setOnLeaveChannelCallback {
            runOnUiThread {
                finish()
            }
        }
    }
    
    private fun setupClickListeners() {
        endCallBtn.setOnClickListener {
            endCall()
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (callType == "video" && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // Permissions already granted
            if (!isIncomingCall) {
                // Outgoing call - join immediately
                joinChannel()
            } else {
                // Incoming call - wait for user to accept
                callStatusText.text = "Incoming call - Tap to accept"
                endCallBtn.setOnClickListener {
                    // Accept call and join
                    joinChannel()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                // Only auto-join for outgoing calls
                // Incoming calls wait for user to accept (click endCallBtn to accept for now)
                if (!isIncomingCall) {
                    joinChannel()
                } else {
                    // For incoming calls, user can accept by clicking endCallBtn (which will join)
                    // Or reject by clicking endCallBtn (which will end)
                    // In a full implementation, you'd have separate accept/reject buttons
                    callStatusText.text = "Incoming call - Tap to accept"
                    endCallBtn.setOnClickListener {
                        // Accept call and join
                        joinChannel()
                    }
                }
            } else {
                Toast.makeText(this, "Permissions required for calls", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun joinChannel() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Use a hash of user ID as UID (Agora requires Int UID)
        val uid = currentUserId.hashCode() and 0x7FFFFFFF // Ensure positive
        
        callStatusText.text = "Joining channel..."
        callManager.joinChannel(channelName, uid)
        
        // Restore end call functionality after joining
        endCallBtn.setOnClickListener {
            endCall()
        }
    }
    
    private fun setupLocalVideo() {
        localVideoView.removeAllViews()
        val surfaceView = SurfaceView(this)
        surfaceView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        localVideoView.addView(surfaceView)
        callManager.setupLocalVideo(surfaceView)
        localVideoView.visibility = View.VISIBLE
    }
    
    private fun setupRemoteVideo(uid: Int) {
        remoteVideoView.removeAllViews()
        val surfaceView = SurfaceView(this)
        surfaceView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        remoteVideoView.addView(surfaceView)
        callManager.setupRemoteVideo(surfaceView, uid)
        remoteVideoView.visibility = View.VISIBLE
    }
    
    private fun loadProfileImage() {
        val userId = otherUserId ?: return
        
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("profileImageBase64")
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val profileImageBase64 = snapshot.getValue(String::class.java) ?: ""
                    if (profileImageBase64.isNotEmpty()) {
                        try {
                            val bitmap = Base64Image.base64ToBitmap(profileImageBase64)
                            if (bitmap != null) {
                                runOnUiThread {
                                    profileImageView.setImageBitmap(bitmap)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CallActivity", "Error loading profile image: ${e.message}", e)
                        }
                    }
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("CallActivity", "Failed to load profile image: ${error.message}")
                }
            })
    }
    
    private fun endCall() {
        callManager.leaveChannel()
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            callManager.leaveChannel()
        }
    }
}
