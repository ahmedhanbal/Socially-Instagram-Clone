package com.hans.i221271_i220889

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CallExtra : AppCompatActivity() {
    
    private lateinit var videoCallButton: Button
    private lateinit var voiceCallButton: Button
    private lateinit var endCallButton: ImageButton
    private lateinit var callStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        videoCallButton = findViewById(R.id.videoCallButton)
        voiceCallButton = findViewById(R.id.voiceCallButton)
        endCallButton = findViewById(R.id.callEnd)
        callStatusText = findViewById(R.id.callStatusText)
    }

    private fun setupClickListeners() {
        videoCallButton.setOnClickListener {
            startCall("video")
        }

        voiceCallButton.setOnClickListener {
            startCall("voice")
        }

        endCallButton.setOnClickListener {
            finish()
        }
    }

    private fun startCall(callType: String) {
        val channelName = generateChannelName()
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("channelName", channelName)
            putExtra("callType", callType)
            putExtra("isIncomingCall", false)
        }
        startActivity(intent)
    }

    private fun generateChannelName(): String {
        return "socially_call_${System.currentTimeMillis()}"
    }
}
