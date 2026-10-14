package com.example

import com.example.data.model.ActionType
import com.example.data.model.DeviceAction
import com.example.data.model.LocalGgufConfig
import com.example.data.model.PlanStatus
import com.example.data.model.ReverseAction
import com.example.data.model.SubTaskStatus
import com.example.engine.RollbackEngine
import com.example.engine.TaskTimePlanner
import com.example.engine.VisionPromptProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTaskDecompositionAndDynamicTimeBudgeting() {
        val planner = TaskTimePlanner()
        val plan = planner.decomposeTask(
            conversationId = "test-conv-1",
            goal = "ابحث عن أقرب مطعم في الخريطة خلال 60 ثانية",
            userSpecifiedBudgetSec = 60
        )

        assertEquals("test-conv-1", plan.conversationId)
        assertEquals(60, plan.totalTimeBudgetSec)
        assertTrue("Subtasks should not be empty", plan.subtasks.isNotEmpty())
        assertEquals(PlanStatus.PLANNING, plan.status)

        // التحقق من أن مجموع أوقات المهام الفرعية يطابق الميزانية الكلية
        val sumBudget = plan.subtasks.sumOf { it.timeBudgetSec }
        assertEquals(60, sumBudget)
    }

    @Test
    fun testEarlyCompletionOptimizationLoop() {
        val planner = TaskTimePlanner()
        val plan = planner.decomposeTask(
            conversationId = "test-conv-2",
            goal = "فتح الإعدادات وضبط البلوتوث",
            userSpecifiedBudgetSec = 45
        )

        val firstTask = plan.subtasks.first()
        firstTask.elapsedSec = 2 // انتهت بسرعة فائقة قبل الوقت المحدد (مثلاً 15 ثانية)

        val shouldOptimize = planner.registerOptimizationLoop(plan, firstTask.id)
        assertTrue("Should enter optimization loop when finished early", shouldOptimize)
        assertEquals(SubTaskStatus.OPTIMIZING, firstTask.status)
        assertEquals(1, firstTask.optimizationIterations)
    }

    @Test
    fun testRollbackEngineInverseActionGeneration() {
        val dummyDao = object : com.example.data.db.AuraDao {
            override fun getAllConversations() = kotlinx.coroutines.flow.emptyFlow<List<com.example.data.db.ConversationEntity>>()
            override suspend fun insertConversation(conversation: com.example.data.db.ConversationEntity) {}
            override suspend fun updateConversation(conversation: com.example.data.db.ConversationEntity) {}
            override suspend fun deleteConversation(id: String) {}
            override suspend fun getActionsForConversation(conversationId: String) = emptyList<com.example.data.db.ActionLogEntity>()
            override suspend fun getActionsReverseForConversation(conversationId: String) = emptyList<com.example.data.db.ActionLogEntity>()
            override suspend fun insertActionLog(action: com.example.data.db.ActionLogEntity) {}
            override suspend fun markAllRolledBack(conversationId: String) {}
            override fun getAllSkills() = kotlinx.coroutines.flow.emptyFlow<List<com.example.data.db.SkillEntity>>()
            override suspend fun insertSkill(skill: com.example.data.db.SkillEntity) {}
            override suspend fun updateSkill(skill: com.example.data.db.SkillEntity) {}
            override suspend fun deleteSkill(id: String) {}
        }

        val rollbackEngine = RollbackEngine(dummyDao)

        // 1. اختبار عكس السحب (SWIPE)
        val swipeAction = DeviceAction(
            type = ActionType.SWIPE,
            description = "سحب للأعلى",
            x = 500f,
            y = 1000f,
            endX = 500f,
            endY = 200f
        )
        val reverseSwipe = rollbackEngine.createReverseAction(swipeAction)
        assertEquals(ActionType.SWIPE, reverseSwipe.type)
        assertEquals(500f, reverseSwipe.x)
        assertEquals(200f, reverseSwipe.y)
        assertEquals(500f, reverseSwipe.endX)
        assertEquals(1000f, reverseSwipe.endY)

        // 2. اختبار عكس إدخال النص (TEXT_INPUT)
        val textAction = DeviceAction(
            type = ActionType.TEXT_INPUT,
            description = "كتابة الاسم",
            text = "Aura Agent"
        )
        val reverseText = rollbackEngine.createReverseAction(textAction)
        assertEquals(ActionType.TEXT_INPUT, reverseText.type)
        assertEquals("Aura Agent", reverseText.textToUndo)

        // 3. اختبار عكس النقر (TAP)
        val tapAction = DeviceAction(
            type = ActionType.TAP,
            description = "نقر",
            x = 300f,
            y = 400f
        )
        val reverseTap = rollbackEngine.createReverseAction(tapAction)
        assertEquals(ActionType.SYSTEM_KEY, reverseTap.type)
        assertEquals(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK, reverseTap.keyCode)
    }

    @Test
    fun testVisionPromptProtocolArchitectureNotes() {
        // فحص البرومبت مع نموذج مدمج الرؤية (Embedded mmproj)
        val embeddedConfig = LocalGgufConfig(
            mainModelPath = "/sdcard/qwen.gguf",
            isEmbeddedMmproj = true,
            modelName = "Qwen-VL-Unified"
        )
        val promptEmbedded = VisionPromptProtocol.buildSystemPrompt(localConfig = embeddedConfig)
        assertTrue(promptEmbedded.contains("EMBEDDED MULTIMODAL PROJECTION"))
        assertTrue(promptEmbedded.contains("STRICT MANDATORY VISION ENFORCEMENT"))

        // فحص البرومبت مع بروجيكتور منفصل (Separate mmproj)
        val separateConfig = LocalGgufConfig(
            mainModelPath = "/sdcard/llama.gguf",
            mmprojPath = "/sdcard/mmproj.gguf",
            isEmbeddedMmproj = false,
            modelName = "Llama-Vision-Dual"
        )
        val promptSeparate = VisionPromptProtocol.buildSystemPrompt(localConfig = separateConfig)
        assertTrue(promptSeparate.contains("SEPARATE MMPROJ CLIP/SIGLIP PROJECTOR"))
    }
}
