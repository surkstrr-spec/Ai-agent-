package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false
)

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val subtaskId: String,
    val actionType: String,
    val description: String,
    val x: Float,
    val y: Float,
    val endX: Float,
    val endY: Float,
    val text: String,
    val keyCode: Int,
    // Reversal parameters
    val reverseActionType: String,
    val reverseDescription: String,
    val reverseX: Float,
    val reverseY: Float,
    val reverseEndX: Float,
    val reverseEndY: Float,
    val reverseText: String,
    val reverseKeyCode: Int,
    val timestamp: Long,
    val isRolledBack: Boolean = false
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val toolCallSignature: String,
    val isEnabled: Boolean,
    val isCustom: Boolean
)
