package com.example.data.model

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM,
    AGENT_STATUS
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val sender: MessageSender,
    val text: String,
    val attachedUris: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val taskPlan: AgentTaskPlan? = null,
    val isAgentExecution: Boolean = false,
    val verificationSnapshotNotice: String? = null
)

data class Conversation(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isArchived: Boolean = false
)
