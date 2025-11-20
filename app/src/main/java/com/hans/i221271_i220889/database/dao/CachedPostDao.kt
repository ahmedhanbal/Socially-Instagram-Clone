package com.hans.i221271_i220889.database.dao

import androidx.room.*
import com.hans.i221271_i220889.database.entities.CachedPost

@Dao
interface CachedPostDao {
    
    @Query("SELECT * FROM cached_posts ORDER BY createdAt DESC")
    suspend fun getAllPosts(): List<CachedPost>
    
    @Query("SELECT * FROM cached_posts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPostsByUser(userId: Int): List<CachedPost>
    
    @Query("SELECT * FROM cached_posts WHERE id = :postId")
    suspend fun getPostById(postId: Int): CachedPost?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CachedPost)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CachedPost>)
    
    @Update
    suspend fun updatePost(post: CachedPost)
    
    @Delete
    suspend fun deletePost(post: CachedPost)
    
    @Query("DELETE FROM cached_posts WHERE cachedAt < :timestamp")
    suspend fun deleteOldPosts(timestamp: Long)
    
    @Query("DELETE FROM cached_posts")
    suspend fun deleteAllPosts()
}

