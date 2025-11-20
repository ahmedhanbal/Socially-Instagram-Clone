package com.hans.i221271_i220889.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object PresenceManager {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun setOnline() {
        val uid = auth.currentUser?.uid ?: return
        val userStatusRef = database.reference.child("status").child(uid)
        val updates = mapOf(
            "isOnline" to true,
            "lastSeen" to System.currentTimeMillis()
        )
        // Ensure we mark offline if connection drops
        userStatusRef.onDisconnect().updateChildren(
            mapOf(
                "isOnline" to false,
                "lastSeen" to System.currentTimeMillis()
            )
        )
        userStatusRef.updateChildren(updates)
    }

    fun setOffline() {
        val uid = auth.currentUser?.uid ?: return
        val userStatusRef = database.reference.child("status").child(uid)
        userStatusRef.updateChildren(
            mapOf(
                "isOnline" to false,
                "lastSeen" to System.currentTimeMillis()
            )
        )
    }
}


