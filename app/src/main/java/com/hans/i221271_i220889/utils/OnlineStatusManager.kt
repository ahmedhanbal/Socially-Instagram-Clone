package com.hans.i221271_i220889.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.hans.i221271_i220889.models.User
import com.google.firebase.database.FirebaseDatabase

class OnlineStatusManager : Application.ActivityLifecycleCallbacks {
    private val database = FirebaseDatabase.getInstance()
    private var currentUserId: String? = null

    fun setCurrentUser(userId: String) {
        currentUserId = userId
        setUserOnline(userId)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentUserId?.let { setUserOnline(it) }
    }

    override fun onActivityResumed(activity: Activity) {
        currentUserId?.let { setUserOnline(it) }
    }

    override fun onActivityPaused(activity: Activity) {
        currentUserId?.let { setUserOffline(it) }
    }

    override fun onActivityStopped(activity: Activity) {
        currentUserId?.let { setUserOffline(it) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        currentUserId?.let { setUserOffline(it) }
    }

    private fun setUserOnline(userId: String) {
        database.reference.child("users").child(userId).child("isOnline").setValue(true)
        database.reference.child("users").child(userId).child("lastSeen").setValue(System.currentTimeMillis())
    }

    private fun setUserOffline(userId: String) {
        database.reference.child("users").child(userId).child("isOnline").setValue(false)
        database.reference.child("users").child(userId).child("lastSeen").setValue(System.currentTimeMillis())
    }

    fun getUserOnlineStatus(userId: String, onComplete: (Boolean, Long) -> Unit) {
        database.reference.child("users").child(userId).child("isOnline").get()
            .addOnSuccessListener { onlineSnapshot ->
                database.reference.child("users").child(userId).child("lastSeen").get()
                    .addOnSuccessListener { lastSeenSnapshot ->
                        val isOnline = onlineSnapshot.getValue(Boolean::class.java) ?: false
                        val lastSeen = lastSeenSnapshot.getValue(Long::class.java) ?: 0L
                        onComplete(isOnline, lastSeen)
                    }
            }
    }
}
