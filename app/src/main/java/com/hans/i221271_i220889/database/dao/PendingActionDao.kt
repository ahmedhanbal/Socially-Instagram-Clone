package com.hans.i221271_i220889.database.dao

import androidx.room.*
import com.hans.i221271_i220889.database.entities.PendingAction

@Dao
interface PendingActionDao {
    
    @Query("SELECT * FROM pending_actions WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getAllPendingActions(): List<PendingAction>
    
    @Query("SELECT * FROM pending_actions WHERE id = :actionId")
    suspend fun getActionById(actionId: Long): PendingAction?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingAction): Long
    
    @Update
    suspend fun updateAction(action: PendingAction)
    
    @Delete
    suspend fun deleteAction(action: PendingAction)
    
    @Query("UPDATE pending_actions SET status = :status WHERE id = :actionId")
    suspend fun updateActionStatus(actionId: Long, status: String)
    
    @Query("UPDATE pending_actions SET retryCount = retryCount + 1 WHERE id = :actionId")
    suspend fun incrementRetryCount(actionId: Long)
    
    @Query("DELETE FROM pending_actions WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedActions()
    
    @Query("DELETE FROM pending_actions WHERE status = 'FAILED' AND retryCount >= maxRetries")
    suspend fun deleteFailedActions()
    
    @Query("DELETE FROM pending_actions")
    suspend fun deleteAllActions()
}

