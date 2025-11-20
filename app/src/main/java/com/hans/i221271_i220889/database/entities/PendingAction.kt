package com.hans.i221271_i220889.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // "SEND_MESSAGE", "CREATE_POST", "CREATE_STORY", "LIKE_POST", "ADD_COMMENT", etc.
    val payload: String, // JSON string of the data to send
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val status: String = "PENDING" // "PENDING", "IN_PROGRESS", "FAILED", "COMPLETED"
)

