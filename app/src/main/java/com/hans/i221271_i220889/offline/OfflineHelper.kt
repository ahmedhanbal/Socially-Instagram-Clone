package com.hans.i221271_i220889.offline

import android.content.Context
import android.content.Intent
import android.util.Log
import com.hans.i221271_i220889.services.OfflineSyncService

/**
 * Helper class for offline functionality
 * Use this when you want to queue actions or manually trigger sync
 */
object OfflineHelper {
    
    private const val TAG = "OfflineHelper"
    
    /**
     * Manually trigger offline sync service
     * Useful to call after user manually refreshes or opens app
     */
    fun triggerSync(context: Context) {
        if (NetworkHelper.isOnline(context)) {
            Log.d(TAG, "Manually triggering offline sync...")
            val intent = Intent(context, OfflineSyncService::class.java)
            context.startService(intent)
        } else {
            Log.d(TAG, "Cannot trigger sync - device is offline")
        }
    }
    
    /**
     * Get queue manager instance
     */
    fun getQueueManager(context: Context): OfflineQueueManager {
        return OfflineQueueManager(context)
    }
    
    /**
     * Example: Queue a message when offline
     * 
     * Usage:
     * ```
     * if (!NetworkHelper.isOnline(context)) {
     *     val queueManager = OfflineHelper.getQueueManager(context)
     *     lifecycleScope.launch {
     *         queueManager.queueSendMessage(receiverId, text, null, "text", false)
     *         Toast.makeText(context, "Message queued - will send when online", Toast.LENGTH_SHORT).show()
     *     }
     * }
     * ```
     */
}

