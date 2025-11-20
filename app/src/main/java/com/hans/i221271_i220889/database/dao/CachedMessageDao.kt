package com.hans.i221271_i220889.database.dao

import androidx.room.*
import com.hans.i221271_i220889.database.entities.CachedMessage

@Dao
interface CachedMessageDao {
    
    @Query("SELECT * FROM cached_messages WHERE (senderId = :userId1 AND receiverId = :userId2) OR (senderId = :userId2 AND receiverId = :userId1) ORDER BY createdAt ASC")
    suspend fun getConversation(userId1: Int, userId2: Int): List<CachedMessage>
    
    @Query("SELECT * FROM cached_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Int): CachedMessage?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CachedMessage)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)
    
    @Update
    suspend fun updateMessage(message: CachedMessage)
    
    @Delete
    suspend fun deleteMessage(message: CachedMessage)
    
    @Query("DELETE FROM cached_messages WHERE isVanish = 1 AND isSeen = 1")
    suspend fun deleteSeenVanishMessages()
    
    @Query("DELETE FROM cached_messages WHERE cachedAt < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
    
    @Query("DELETE FROM cached_messages")
    suspend fun deleteAllMessages()
}

