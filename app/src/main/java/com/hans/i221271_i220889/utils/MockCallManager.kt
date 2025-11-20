package com.hans.i221271_i220889.utils

import android.content.Context
import android.util.Log
import java.util.*

class MockCallManager private constructor() {
    
    companion object {
        private const val TAG = "MockCallManager"
        private var instance: MockCallManager? = null
        
        fun getInstance(): MockCallManager {
            return instance ?: synchronized(this) {
                instance ?: MockCallManager().also { instance = it }
            }
        }
    }
    
    private var isJoined = false
    private var isVideoEnabled = true
    private var isAudioEnabled = true
    private var isSpeakerEnabled = true
    private var callStartTime: Long = 0
    private var callTimer: Timer? = null
    
    // Callbacks
    private var onUserJoinedCallback: ((Int) -> Unit)? = null
    private var onUserOfflineCallback: ((Int) -> Unit)? = null
    private var onJoinChannelSuccessCallback: ((String, Int, Int) -> Unit)? = null
    private var onLeaveChannelCallback: (() -> Unit)? = null
    private var onErrorCallback: ((Int, String) -> Unit)? = null
    
    fun initialize(context: Context): Boolean {
        Log.d(TAG, "MockCallManager initialized successfully")
        return true
    }
    
    fun joinChannel(channelName: String, uid: Int, token: String = ""): Boolean {
        Log.d(TAG, "Mock: Joining channel: $channelName with UID: $uid")
        
        // Simulate connection delay
        Timer().schedule(object : TimerTask() {
            override fun run() {
                isJoined = true
                callStartTime = System.currentTimeMillis()
                Log.d(TAG, "Mock: Successfully joined channel: $channelName")
                onJoinChannelSuccessCallback?.invoke(channelName, uid, 1000)
                
                // Simulate another user joining after 2 seconds
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        Log.d(TAG, "Mock: Simulated user joined")
                        onUserJoinedCallback?.invoke(12345)
                    }
                }, 2000)
            }
        }, 1500)
        
        return true
    }
    
    fun leaveChannel() {
        callTimer?.cancel()
        isJoined = false
        Log.d(TAG, "Mock: Left channel")
        onLeaveChannelCallback?.invoke()
    }
    
    fun setupLocalVideo(surfaceView: Any) {
        Log.d(TAG, "Mock: Local video setup")
    }
    
    fun setupRemoteVideo(surfaceView: Any, uid: Int) {
        Log.d(TAG, "Mock: Remote video setup for UID: $uid")
    }
    
    fun toggleVideo(): Boolean {
        isVideoEnabled = !isVideoEnabled
        Log.d(TAG, "Mock: Video ${if (isVideoEnabled) "enabled" else "disabled"}")
        return isVideoEnabled
    }
    
    fun toggleAudio(): Boolean {
        isAudioEnabled = !isAudioEnabled
        Log.d(TAG, "Mock: Audio ${if (isAudioEnabled) "enabled" else "disabled"}")
        return isAudioEnabled
    }
    
    fun toggleSpeaker(): Boolean {
        isSpeakerEnabled = !isSpeakerEnabled
        Log.d(TAG, "Mock: Speaker ${if (isSpeakerEnabled) "enabled" else "disabled"}")
        return isSpeakerEnabled
    }
    
    fun switchCamera() {
        Log.d(TAG, "Mock: Camera switched")
    }
    
    fun isVideoEnabled(): Boolean = isVideoEnabled
    fun isAudioEnabled(): Boolean = isAudioEnabled
    fun isSpeakerEnabled(): Boolean = isSpeakerEnabled
    fun isInChannel(): Boolean = isJoined
    
    // Callback setters
    fun setOnUserJoinedCallback(callback: (Int) -> Unit) {
        onUserJoinedCallback = callback
    }
    
    fun setOnUserOfflineCallback(callback: (Int) -> Unit) {
        onUserOfflineCallback = callback
    }
    
    fun setOnJoinChannelSuccessCallback(callback: (String, Int, Int) -> Unit) {
        onJoinChannelSuccessCallback = callback
    }
    
    fun setOnLeaveChannelCallback(callback: () -> Unit) {
        onLeaveChannelCallback = callback
    }
    
    fun setOnErrorCallback(callback: (Int, String) -> Unit) {
        onErrorCallback = callback
    }
    
    fun destroy() {
        callTimer?.cancel()
        isJoined = false
        instance = null
        Log.d(TAG, "Mock: Call manager destroyed")
    }
}
