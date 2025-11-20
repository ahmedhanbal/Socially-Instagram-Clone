package com.hans.i221271_i220889.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hans.i221271_i220889.database.dao.*
import com.hans.i221271_i220889.database.entities.*

@Database(
    entities = [
        CachedPost::class,
        CachedMessage::class,
        CachedStory::class,
        PendingAction::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SociallyDatabase : RoomDatabase() {
    
    abstract fun cachedPostDao(): CachedPostDao
    abstract fun cachedMessageDao(): CachedMessageDao
    abstract fun cachedStoryDao(): CachedStoryDao
    abstract fun pendingActionDao(): PendingActionDao
    
    companion object {
        @Volatile
        private var INSTANCE: SociallyDatabase? = null
        
        fun getDatabase(context: Context): SociallyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SociallyDatabase::class.java,
                    "socially_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

