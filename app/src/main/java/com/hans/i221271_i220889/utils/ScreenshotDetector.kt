package com.hans.i221271_i220889.utils

import android.app.Activity
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hans.i221271_i220889.repositories.SearchRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File

class ScreenshotDetector(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val chatPartnerId: Int
) {
    private val contentResolver: ContentResolver = activity.contentResolver
    private var externalObserver: ContentObserver? = null
    private var internalObserver: ContentObserver? = null
    private var lastScreenshotTime: Long = 0
    private var lastScreenshotId: Long = -1
    private val screenshotRepository = SearchRepository(activity)
    
    // Track the last checked screenshot timestamp
    private var lastCheckedTimestamp: Long = System.currentTimeMillis()

    fun startDetection() {
        if (externalObserver != null || internalObserver != null) return
        
        val handler = Handler(Looper.getMainLooper())
        externalObserver = createObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        internalObserver = createObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        
        externalObserver?.let {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                it
            )
        }
        internalObserver?.let {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                true,
                it
            )
        }
        
        // Also start periodic checking as a backup
        startPeriodicCheck()
    }

    fun stopDetection() {
        externalObserver?.let {
            contentResolver.unregisterContentObserver(it)
            externalObserver = null
        }
        internalObserver?.let {
            contentResolver.unregisterContentObserver(it)
            internalObserver = null
        }
    }

    private fun startPeriodicCheck() {
        lifecycleOwner.lifecycleScope.launch {
            while (externalObserver != null || internalObserver != null) {
                delay(2000) // Check every 2 seconds
                checkForScreenshot(null)
            }
        }
    }
    
    private fun createObserver(sourceUri: Uri): ContentObserver {
        return object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Some OEMs pass null URIs, so fall back to the source URI
                val targetUri = uri ?: sourceUri
                checkForScreenshot(targetUri)
            }
        }
    }

    private fun checkForScreenshot(targetUri: Uri? = null) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                val projection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.RELATIVE_PATH
                    )
                } else {
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATA
                    )
                }
                
                val queryUri = if (targetUri != null && targetUri.toString().startsWith("content://media/")) {
                    targetUri
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                
                val selectionData = buildSelection(targetUri)
                
                val cursor = contentResolver.query(
                    queryUri,
                    projection,
                    selectionData.first,
                    selectionData.second,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )
                
                cursor?.use {
                    var count = 0
                    val maxResults = 10 // Limit manually
                    while (it.moveToNext() && count < maxResults) {
                        try {
                            val idIndex = it.getColumnIndex(MediaStore.Images.Media._ID)
                            val dateAddedIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                            val displayNameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                            
                            val mediaId = if (idIndex >= 0) it.getLong(idIndex) else -1
                            val dateAddedSeconds = it.getLong(dateAddedIndex)
                            val dateAdded = dateAddedSeconds * 1000 // Convert to milliseconds
                            val displayName = it.getString(displayNameIndex) ?: ""
                            
                            // Get path (different column for different Android versions)
                            var filePath = ""
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                val relativePathIndex = it.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                                if (relativePathIndex >= 0) {
                                    filePath = it.getString(relativePathIndex) ?: ""
                                }
                            } else {
                                val dataIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                                if (dataIndex >= 0) {
                                    filePath = it.getString(dataIndex) ?: ""
                                }
                            }
                            
                            val timeDiffMillis = System.currentTimeMillis() - dateAdded
                            val isRecent = timeDiffMillis <= 15_000 // 15 seconds window
                            val isScreenshot = isScreenshotFile(displayName, filePath)
                            
                            if (isRecent && isScreenshot && dateAdded > lastScreenshotTime && mediaId != lastScreenshotId) {
                                lastScreenshotTime = dateAdded
                                lastScreenshotId = mediaId
                                val secondsAgo = (timeDiffMillis / 1000).coerceAtLeast(0)
                                Log.d("ScreenshotDetector", "Screenshot detected: $displayName in $filePath (${secondsAgo}s ago)")
                                Log.i("ScreenshotDetector", "Screenshot detected for user $chatPartnerId, notifying backend")
                                notifyScreenshotTaken()
                                break // Only report one screenshot at a time
                            }
                            count++
                        } catch (e: Exception) {
                            Log.e("ScreenshotDetector", "Error processing screenshot entry: ${e.message}")
                        }
                    }
                }
                
                // Update last checked timestamp
                lastCheckedTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e("ScreenshotDetector", "Error checking for screenshot: ${e.message}", e)
            }
        }
    }
    
    private fun buildSelection(targetUri: Uri?): Pair<String?, Array<String>?> {
        return if (targetUri != null && targetUri.path?.contains("/images/media") == true) {
            val id = targetUri.lastPathSegment
            if (id != null) {
                Pair("${MediaStore.Images.Media._ID} = ?", arrayOf(id))
            } else {
                Pair(null, null)
            }
        } else {
            val lastCheckedSeconds = lastCheckedTimestamp / 1000
            Pair("${MediaStore.Images.Media.DATE_ADDED} > ?", arrayOf(lastCheckedSeconds.toString()))
        }
    }
    
    private fun isScreenshotFile(displayName: String, filePath: String): Boolean {
        val keywords = listOf(
            "Screenshot","screenshot", "screen_shot", "screen-shot", "screen capture",
            "スクリーンショット", "截圖", "截屏", "screencap", "ss", "screen_cap"
        )
        val combined = "${displayName.lowercase()} $filePath".lowercase()
        return keywords.any { combined.contains(it) }
    }

    private fun notifyScreenshotTaken() {
        lifecycleOwner.lifecycleScope.launch {
            try {
                val result = screenshotRepository.reportScreenshot(chatPartnerId)
                result.onSuccess {
                    Log.d("ScreenshotDetector", "Screenshot reported successfully")
                }.onFailure { error ->
                    Log.e("ScreenshotDetector", "Failed to report screenshot: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("ScreenshotDetector", "Error reporting screenshot: ${e.message}", e)
            }
        }
    }
}
