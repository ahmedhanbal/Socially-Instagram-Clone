package com.hans.i221271_i220889.utils

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class AgoraCallManager private constructor() {
    
    companion object {
        private const val TAG = "AgoraCallManager"
        private var instance: AgoraCallManager? = null
        
        fun getInstance(): AgoraCallManager {
            return instance ?: synchronized(this) {
                instance ?: AgoraCallManager().also { instance = it }
            }
        }
    }

    private val AGORA_APP_ID = "a53373894de7464994c65e8158efb02d"
    
    private var rtcEngine: RtcEngine? = null
    private var isJoined = false
    private var localUid: Int = 0
    private var isVideoEnabled = true
    private var isAudioEnabled = true
    private var isSpeakerEnabled = false
    
    // Callbacks
    private var onUserJoinedCallback: ((Int) -> Unit)? = null
    private var onUserOfflineCallback: ((Int) -> Unit)? = null
    private var onJoinChannelSuccessCallback: ((String, Int, Int) -> Unit)? = null
    private var onLeaveChannelCallback: (() -> Unit)? = null
    private var onErrorCallback: ((Int, String) -> Unit)? = null
    
    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined channel: $channel with UID: $uid")
            isJoined = true
            localUid = uid
            onJoinChannelSuccessCallback?.invoke(channel, uid, elapsed)
        }
        
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "User joined: $uid")
            onUserJoinedCallback?.invoke(uid)
        }
        
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "User offline: $uid, reason: $reason")
            onUserOfflineCallback?.invoke(uid)
        }
        
        override fun onLeaveChannel(stats: RtcStats) {
            Log.d(TAG, "Left channel")
            isJoined = false
            onLeaveChannelCallback?.invoke()
        }
        
        override fun onError(err: Int) {
            val errorMsg = when (err) {
                Constants.ERR_INVALID_APP_ID -> "Invalid App ID"
                Constants.ERR_INVALID_CHANNEL_NAME -> "Invalid channel name"
                Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected"
                Constants.ERR_LEAVE_CHANNEL_REJECTED -> "Leave channel rejected"
                else -> "Error code: $err"
            }
            Log.e(TAG, "Error: $err, $errorMsg")
            onErrorCallback?.invoke(err, errorMsg)
        }
        
        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            Log.d(TAG, "Remote video state changed: uid=$uid, state=$state")
        }
    }
    
    fun initialize(context: Context): Boolean {
        return try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = AGORA_APP_ID
                mEventHandler = eventHandler
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.enableVideo()
            rtcEngine?.enableAudio()
            Log.d(TAG, "Agora RTC Engine initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora RTC Engine: ${e.message}", e)
            false
        }
    }
    
    fun joinChannel(channelName: String, uid: Int, token: String = ""): Boolean {
        return try {
            if (rtcEngine == null) {
                Log.e(TAG, "RTC Engine not initialized")
                return false
            }
            
            Log.d(TAG, "Joining channel: $channelName with UID: $uid")
            val result = rtcEngine?.joinChannel(token, channelName, uid, ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            })
            
            if (result == 0) {
                Log.d(TAG, "Join channel request sent successfully")
                true
            } else {
                Log.e(TAG, "Failed to join channel, error code: $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception joining channel: ${e.message}", e)
            false
        }
    }
    
    fun leaveChannel() {
        try {
            rtcEngine?.leaveChannel()
            Log.d(TAG, "Left channel")
        } catch (e: Exception) {
            Log.e(TAG, "Exception leaving channel: ${e.message}", e)
        }
    }
    
    fun setupLocalVideo(surfaceView: SurfaceView) {
        try {
            rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0))
            Log.d(TAG, "Local video setup")
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting up local video: ${e.message}", e)
        }
    }
    
    fun setupRemoteVideo(surfaceView: SurfaceView, uid: Int) {
        try {
            rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid))
            Log.d(TAG, "Remote video setup for UID: $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting up remote video: ${e.message}", e)
        }
    }
    
    fun toggleVideo(): Boolean {
        return try {
            isVideoEnabled = !isVideoEnabled
            rtcEngine?.enableLocalVideo(isVideoEnabled)
            Log.d(TAG, "Video ${if (isVideoEnabled) "enabled" else "disabled"}")
            isVideoEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Exception toggling video: ${e.message}", e)
            isVideoEnabled
        }
    }
    
    fun toggleAudio(): Boolean {
        return try {
            isAudioEnabled = !isAudioEnabled
            rtcEngine?.muteLocalAudioStream(!isAudioEnabled)
            Log.d(TAG, "Audio ${if (isAudioEnabled) "enabled" else "disabled"}")
            isAudioEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Exception toggling audio: ${e.message}", e)
            isAudioEnabled
        }
    }
    
    fun toggleSpeaker(): Boolean {
        return try {
            isSpeakerEnabled = !isSpeakerEnabled
            rtcEngine?.setEnableSpeakerphone(isSpeakerEnabled)
            Log.d(TAG, "Speaker ${if (isSpeakerEnabled) "enabled" else "disabled"}")
            isSpeakerEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Exception toggling speaker: ${e.message}", e)
            isSpeakerEnabled
        }
    }
    
    fun switchCamera() {
        try {
            rtcEngine?.switchCamera()
            Log.d(TAG, "Camera switched")
        } catch (e: Exception) {
            Log.e(TAG, "Exception switching camera: ${e.message}", e)
        }
    }
    
    fun isVideoEnabled(): Boolean = isVideoEnabled
    fun isAudioEnabled(): Boolean = isAudioEnabled
    fun isSpeakerEnabled(): Boolean = isSpeakerEnabled
    fun isInChannel(): Boolean = isJoined
    fun getLocalUid(): Int = localUid
    
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
        try {
            leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
            isJoined = false
            instance = null
            Log.d(TAG, "Agora RTC Engine destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Exception destroying engine: ${e.message}", e)
        }
    }
}

