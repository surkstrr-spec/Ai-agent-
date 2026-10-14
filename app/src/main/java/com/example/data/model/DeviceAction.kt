package com.example.data.model

enum class ActionType(val titleAr: String) {
    TAP("نقر عادي"),
    DOUBLE_TAP("نقر مزدوج"),
    LONG_PRESS("ضغط مطوّل"),
    SWIPE("سحب وإفلات"),
    TEXT_INPUT("إدخال نص"),
    SYSTEM_KEY("زر نظام"),
    WAIT("انتظار")
}

data class ReverseAction(
    val type: ActionType,
    val description: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val textToUndo: String = "",
    val keyCode: Int = 0
)

data class DeviceAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ActionType,
    val description: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val durationMs: Long = 200,
    val text: String = "",
    val keyCode: Int = 0,
    val reverseAction: ReverseAction? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isReversible: Boolean = true
)
