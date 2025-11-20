package com.hans.i221271_i220889.utils

import android.app.Activity
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.hans.i221271_i220889.models.User
import com.google.firebase.database.FirebaseDatabase

class ScreenshotDetector(private val activity: Activity) {
    private val contentResolver: ContentResolver = activity.contentResolver
    private val database = FirebaseDatabase.getInstance()
    private var contentObserver: ContentObserver? = null

    fun startDetection(currentUser: User, chatPartnerId: String) {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null && uri.toString().contains("screenshot")) {
                    // Screenshot detected
                    notifyScreenshotTaken(currentUser, chatPartnerId)
                }
            }
        }

        // Monitor media store for screenshots
        contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
    }

    fun stopDetection() {
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
            contentObserver = null
        }
    }

    private fun notifyScreenshotTaken(currentUser: User, chatPartnerId: String) {
        // Send notification to the other user
        val notificationData = mapOf(
            "title" to "Screenshot Alert",
            "body" to "${currentUser.username} took a screenshot of your chat",
            "type" to "screenshot_alert",
            "fromUserId" to currentUser.userId,
            "fromUsername" to currentUser.username
        )

        // Save screenshot alert to database
        database.reference.child("screenshotAlerts").child(chatPartnerId).push().setValue(notificationData)
    }
}
