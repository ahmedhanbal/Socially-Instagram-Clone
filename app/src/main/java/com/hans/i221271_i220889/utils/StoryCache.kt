package com.hans.i221271_i220889.utils

import com.hans.i221271_i220889.models.Story

/**
 * Singleton cache to temporarily store stories for viewing
 * This avoids Intent size limits when passing Story objects with large Base64 images
 */
object StoryCache {
    private var cachedStories: List<Story> = emptyList()
    
    fun setStories(stories: List<Story>) {
        cachedStories = stories
    }
    
    fun getStories(): List<Story> {
        return cachedStories
    }
    
    fun getStoryById(storyId: String): Story? {
        return cachedStories.firstOrNull { it.storyId == storyId }
    }
    
    fun getStoriesByUserId(userId: String): List<Story> {
        return cachedStories.filter { it.userId == userId }
    }
    
    fun clear() {
        cachedStories = emptyList()
    }
}

