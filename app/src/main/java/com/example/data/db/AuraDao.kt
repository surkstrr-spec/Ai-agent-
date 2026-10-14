package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AuraDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("SELECT * FROM action_logs WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getActionsForConversation(conversationId: String): List<ActionLogEntity>

    @Query("SELECT * FROM action_logs WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    suspend fun getActionsReverseForConversation(conversationId: String): List<ActionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(action: ActionLogEntity)

    @Query("UPDATE action_logs SET isRolledBack = 1 WHERE conversationId = :conversationId")
    suspend fun markAllRolledBack(conversationId: String)

    @Query("SELECT * FROM skills")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkill(id: String)
}
