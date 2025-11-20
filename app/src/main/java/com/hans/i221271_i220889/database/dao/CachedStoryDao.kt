package com.hans.i221271_i220889.database.dao

import androidx.room.*
import com.hans.i221271_i220889.database.entities.CachedStory

@Dao
interface CachedStoryDao {
    
    @Query("SELECT * FROM cached_stories ORDER BY createdAt DESC")
    suspend fun getAllStories(): List<CachedStory>
    
    @Query("SELECT * FROM cached_stories WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getStoriesByUser(userId: Int): List<CachedStory>
    
    @Query("SELECT * FROM cached_stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: Int): CachedStory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: CachedStory)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<CachedStory>)
    
    @Delete
    suspend fun deleteStory(story: CachedStory)
    
    @Query("DELETE FROM cached_stories WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredStories(currentTime: String)
    
    @Query("DELETE FROM cached_stories WHERE cachedAt < :timestamp")
    suspend fun deleteOldStories(timestamp: Long)
    
    @Query("DELETE FROM cached_stories")
    suspend fun deleteAllStories()
}

