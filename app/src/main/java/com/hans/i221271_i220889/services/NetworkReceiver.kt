package com.hans.i221271_i220889.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.hans.i221271_i220889.offline.NetworkHelper

/**
 * BroadcastReceiver to detect network connectivity changes
 * Starts OfflineSyncService when device comes online
 */
class NetworkReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NetworkReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val isOnline = NetworkHelper.isOnline(context)
            
            Log.d(TAG, "Network connectivity changed. Online: $isOnline")
            
            if (isOnline) {
                // Device is online - start sync service
                Log.d(TAG, "Device is online. Starting OfflineSyncService...")
                val serviceIntent = Intent(context, OfflineSyncService::class.java)
                context.startService(serviceIntent)
            } else {
                Log.d(TAG, "Device is offline. Sync will start when connection is restored.")
            }
        }
    }
}

