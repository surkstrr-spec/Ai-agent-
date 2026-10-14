package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * خدمة إمكانية الوصول الحقيقية (Accessibility Service)
 * تتولى تنفيذ الأوامر الحركية المباشرة على شاشة الأندرويد:
 * Tap, Double Tap, Long Press, Swipe, Text Input, System Keys (Back/Home/Recents).
 */
class AuraAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AuraAccessibility"
        var instance: AuraAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AuraAccessibilityService Connected and Ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // يمكن استخدامها لتتبع تغير الشاشات ومحتوى النوافذ
    }

    override fun onInterrupt() {
        Log.w(TAG, "AuraAccessibilityService Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "AuraAccessibilityService Destroyed")
    }

    /**
     * تنفيذ نقر عادي عند إحداثيات (x, y)
     */
    fun performTap(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * تنفيذ نقر مزدوج
     */
    fun performDoubleTap(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null): Boolean {
        return performTap(x, y) { success1 ->
            if (success1) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    performTap(x, y, callback)
                }, 120)
            } else {
                callback?.invoke(false)
            }
        }
    }

    /**
     * تنفيذ ضغط مطول
     */
    fun performLongPress(x: Float, y: Float, durationMs: Long = 650, callback: ((Boolean) -> Unit)? = null): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * تنفيذ سحب وإفلات (Swipe / Drag)
     */
    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300,
        callback: ((Boolean) -> Unit)? = null
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * تنفيذ أزرار النظام (Back, Home, Recents)
     */
    fun performGlobal(action: Int): Boolean {
        return performGlobalAction(action)
    }

    /**
     * إدخال نص إلى الحقل النشط
     */
    fun performTextInput(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditableNode(rootNode)

        return if (focusedNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    /**
     * مسح النص للحقل النشط في حالات التراجع العكسي (Rollback)
     */
    fun performClearText(characterCount: Int): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditableNode(rootNode)

        return if (focusedNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findFirstEditableNode(child)
            if (result != null) return result
        }
        return null
    }
}
