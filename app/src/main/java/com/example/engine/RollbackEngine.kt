package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.example.data.db.ActionLogEntity
import com.example.data.db.AuraDao
import com.example.data.model.ActionType
import com.example.data.model.DeviceAction
import com.example.data.model.ReverseAction
import com.example.service.AuraAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * محرك سجل الأحداث والتراجع العكسي (Rollback Engine)
 *
 * يلتزم النظام بتسجيل الحركة العكسية المقابلة لكل إجراء (Reversal Action Index).
 * عند ضغط زر التراجع الشامل (Global Rollback) لمحادثة معينة، يقوم المحرك
 * بقراءة سجل التغييرات للوراء بالتتابع العكسي (LIFO) وتطبيق خطوات التراجع
 * لإعادة الجهاز إلى حالته الأصلية تماماً.
 */
class RollbackEngine(
    private val auraDao: AuraDao
) {
    companion object {
        private const val TAG = "RollbackEngine"
    }

    private val _rollbackState = MutableStateFlow<RollbackStatus>(RollbackStatus.Idle)
    val rollbackState: StateFlow<RollbackStatus> = _rollbackState.asStateFlow()

    sealed class RollbackStatus {
        object Idle : RollbackStatus()
        data class Progress(
            val conversationId: String,
            val currentStep: Int,
            val totalSteps: Int,
            val currentActionDescription: String
        ) : RollbackStatus()
        data class Completed(val conversationId: String, val stepsUndone: Int) : RollbackStatus()
        data class Error(val message: String) : RollbackStatus()
    }

    /**
     * حساب الحركة العكسية تلقائياً لأي إجراء يتم تنفيذه
     */
    fun createReverseAction(action: DeviceAction): ReverseAction {
        return when (action.type) {
            ActionType.TAP -> {
                ReverseAction(
                    type = ActionType.SYSTEM_KEY,
                    description = "الرجوع خطوة للوراء لإلغاء النقر أو إعادة النقر على (${action.x}, ${action.y})",
                    x = action.x,
                    y = action.y,
                    keyCode = AccessibilityService.GLOBAL_ACTION_BACK
                )
            }
            ActionType.DOUBLE_TAP -> {
                ReverseAction(
                    type = ActionType.SYSTEM_KEY,
                    description = "إلغاء النقر المزدوج عبر الرجوع للخلف",
                    keyCode = AccessibilityService.GLOBAL_ACTION_BACK
                )
            }
            ActionType.LONG_PRESS -> {
                ReverseAction(
                    type = ActionType.SYSTEM_KEY,
                    description = "إغلاق القائمة المنبثقة عبر زر الرجوع",
                    keyCode = AccessibilityService.GLOBAL_ACTION_BACK
                )
            }
            ActionType.SWIPE -> {
                // سحب عكسي بنفس المسافة والاتجاه المعاكس
                ReverseAction(
                    type = ActionType.SWIPE,
                    description = "سحب عكسي من (${action.endX}, ${action.endY}) إلى (${action.x}, ${action.y})",
                    x = action.endX,
                    y = action.endY,
                    endX = action.x,
                    endY = action.y
                )
            }
            ActionType.TEXT_INPUT -> {
                ReverseAction(
                    type = ActionType.TEXT_INPUT,
                    description = "مسح النص المُدخل ('${action.text}')",
                    textToUndo = action.text
                )
            }
            ActionType.SYSTEM_KEY -> {
                ReverseAction(
                    type = ActionType.SYSTEM_KEY,
                    description = "إلغاء إجراء النظام السابق",
                    keyCode = AccessibilityService.GLOBAL_ACTION_BACK
                )
            }
            ActionType.WAIT -> {
                ReverseAction(
                    type = ActionType.WAIT,
                    description = "تخطي وقت الانتظار"
                )
            }
        }
    }

    /**
     * تسجيل الإجراء وعكسه في قاعدة بيانات السجل
     */
    suspend fun recordAction(
        conversationId: String,
        subtaskId: String,
        action: DeviceAction
    ) {
        val reverse = action.reverseAction ?: createReverseAction(action)
        val entity = ActionLogEntity(
            id = action.id,
            conversationId = conversationId,
            subtaskId = subtaskId,
            actionType = action.type.name,
            description = action.description,
            x = action.x,
            y = action.y,
            endX = action.endX,
            endY = action.endY,
            text = action.text,
            keyCode = action.keyCode,
            reverseActionType = reverse.type.name,
            reverseDescription = reverse.description,
            reverseX = reverse.x,
            reverseY = reverse.y,
            reverseEndX = reverse.endX,
            reverseEndY = reverse.endY,
            reverseText = reverse.textToUndo,
            reverseKeyCode = reverse.keyCode,
            timestamp = action.timestamp,
            isRolledBack = false
        )
        auraDao.insertActionLog(entity)
        Log.d(TAG, "Recorded action [${action.type}] with reverse [${reverse.type}] for conversation $conversationId")
    }

    /**
     * تنفيذ التراجع الشامل (Global Rollback)
     * يقرأ سجل التغييرات للوراء بالتتابع العكسي وتطبيق خطوات التراجع
     */
    suspend fun executeGlobalRollback(
        conversationId: String,
        onStepUndone: (step: Int, total: Int, desc: String) -> Unit = { _, _, _ -> }
    ): Int {
        val actionsToReverse = auraDao.getActionsReverseForConversation(conversationId)
            .filter { !it.isRolledBack }

        if (actionsToReverse.isEmpty()) {
            _rollbackState.value = RollbackStatus.Completed(conversationId, 0)
            return 0
        }

        val total = actionsToReverse.size
        Log.i(TAG, "Starting Global Rollback for conversation $conversationId: $total actions to undo")

        val accessibilityService = AuraAccessibilityService.instance

        for ((index, item) in actionsToReverse.withIndex()) {
            val stepNumber = index + 1
            _rollbackState.value = RollbackStatus.Progress(
                conversationId = conversationId,
                currentStep = stepNumber,
                totalSteps = total,
                currentActionDescription = item.reverseDescription
            )
            onStepUndone(stepNumber, total, item.reverseDescription)

            // تنفيذ الحركة العكسية
            val reverseType = try {
                ActionType.valueOf(item.reverseActionType)
            } catch (e: Exception) {
                ActionType.SYSTEM_KEY
            }

            when (reverseType) {
                ActionType.TAP -> {
                    accessibilityService?.performTap(item.reverseX, item.reverseY)
                }
                ActionType.SWIPE -> {
                    accessibilityService?.performSwipe(
                        item.reverseX,
                        item.reverseY,
                        item.reverseEndX,
                        item.reverseEndY,
                        300
                    )
                }
                ActionType.TEXT_INPUT -> {
                    // مسح النص السابق
                    accessibilityService?.performClearText(item.reverseText.length)
                }
                ActionType.SYSTEM_KEY -> {
                    val code = if (item.reverseKeyCode != 0) {
                        item.reverseKeyCode
                    } else {
                        AccessibilityService.GLOBAL_ACTION_BACK
                    }
                    accessibilityService?.performGlobal(code)
                }
                else -> {
                    accessibilityService?.performGlobal(AccessibilityService.GLOBAL_ACTION_BACK)
                }
            }

            // مهلة زمنية إنسانية آمنة بين خطوات التراجع العكسي للسماح بتحديث الشاشة
            delay(400)
        }

        // تعليم كل الحركات في المحادثة بأنها تم التراجع عنها
        auraDao.markAllRolledBack(conversationId)
        _rollbackState.value = RollbackStatus.Completed(conversationId, total)
        Log.i(TAG, "Global Rollback completed for conversation $conversationId: $total steps undone")
        return total
    }
}
