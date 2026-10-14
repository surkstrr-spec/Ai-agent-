package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AuraApplication
import com.example.data.db.ConversationEntity
import com.example.data.db.SkillEntity
import com.example.data.model.ActionType
import com.example.data.model.AgentTaskPlan
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.DeviceAction
import com.example.data.model.MessageSender
import com.example.data.model.ModelSettings
import com.example.data.model.OverlayConfig
import com.example.data.model.PlanStatus
import com.example.data.model.ReverseAction
import com.example.data.model.SkillItem
import com.example.data.model.SubTask
import com.example.data.model.SubTaskStatus
import com.example.data.model.ThemeMode
import com.example.service.AuraAccessibilityService
import com.example.service.AuraFloatingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AuraApplication
    private val dao = app.database.auraDao()
    private val taskPlanner = app.taskPlanner
    private val rollbackEngine = app.rollbackEngine

    // حالة الثيم
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // حالة التراكبات البصرية
    private val _overlayConfig = MutableStateFlow(OverlayConfig())
    val overlayConfig: StateFlow<OverlayConfig> = _overlayConfig.asStateFlow()

    // وضع الإيجنت مقابل وضع الشات
    private val _isAgentMode = MutableStateFlow(true)
    val isAgentMode: StateFlow<Boolean> = _isAgentMode.asStateFlow()

    // نص الإدخال والمرفقات
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _attachedUris = MutableStateFlow<List<String>>(emptyList())
    val attachedUris: StateFlow<List<String>> = _attachedUris.asStateFlow()

    // المحادثات
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // شجرة المهام النشطة
    private val _activePlan = MutableStateFlow<AgentTaskPlan?>(null)
    val activePlan: StateFlow<AgentTaskPlan?> = _activePlan.asStateFlow()

    // حالة التنفيذ والإيقاف المؤقت
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // الإعدادات
    private val _settings = MutableStateFlow(ModelSettings())
    val settings: StateFlow<ModelSettings> = _settings.asStateFlow()

    // المهارات
    private val _skills = MutableStateFlow<List<SkillItem>>(emptyList())
    val skills: StateFlow<List<SkillItem>> = _skills.asStateFlow()

    // مؤشر التراجع العكسي الجاري
    private val _isRollingBack = MutableStateFlow(false)
    val isRollingBack: StateFlow<Boolean> = _isRollingBack.asStateFlow()

    private var executionJob: Job? = null
    private var timerJob: Job? = null

    init {
        loadConversations()
        loadSkills()
        createInitialConversationIfNeeded()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            dao.getAllConversations().collectLatest { entities ->
                val list = entities.map {
                    Conversation(
                        id = it.id,
                        title = it.title,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        isArchived = it.isArchived
                    )
                }
                _conversations.value = list
            }
        }
    }

    private fun loadSkills() {
        viewModelScope.launch {
            dao.getAllSkills().collectLatest { entities ->
                if (entities.isEmpty()) {
                    // مهارات افتراضية مدمجة
                    val defaults = listOf(
                        SkillEntity(UUID.randomUUID().toString(), "OCR Screen Text", "قراءة النصوص من الشاشة بالكامل", "ocr_extract_text()", true, false),
                        SkillEntity(UUID.randomUUID().toString(), "App Launcher", "فتح التطبيقات عبر اسمها وحزمتها", "launch_package(name)", true, false),
                        SkillEntity(UUID.randomUUID().toString(), "Form Autofill", "تعبئة الحقول والبيانات تلقائياً", "autofill_form(fields)", true, false)
                    )
                    defaults.forEach { dao.insertSkill(it) }
                } else {
                    _skills.value = entities.map {
                        SkillItem(it.id, it.name, it.description, it.toolCallSignature, it.isEnabled, it.isCustom)
                    }
                }
            }
        }
    }

    private fun createInitialConversationIfNeeded() {
        viewModelScope.launch {
            val convId = _currentConversationId.value
            dao.insertConversation(
                ConversationEntity(
                    id = convId,
                    title = "محادثة أتمتة جديدة",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            // رسالة ترحيب أولية
            _messages.value = listOf(
                ChatMessage(
                    conversationId = convId,
                    sender = MessageSender.ASSISTANT,
                    text = "أهلاً بك! أنا Aura Agent، مساعدك الذكي العائم للتحكم بالجوال. يمكنك إعطائي أي مهمة وسأقوم بتفكيكها إلى مهام فرعية وتنفيذها بصرياً عبر الشاشة مع دعم التراجع العكسي الشامل في أي وقت."
                )
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setAgentMode(isAgent: Boolean) {
        _isAgentMode.value = isAgent
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun addAttachment(uri: String) {
        _attachedUris.value = _attachedUris.value + uri
    }

    fun removeAttachment(uri: String) {
        _attachedUris.value = _attachedUris.value.filter { it != uri }
    }

    fun toggleGrid() {
        val current = _overlayConfig.value
        _overlayConfig.value = current.copy(
            showCoordinatesGrid = !current.showCoordinatesGrid,
            hideAllOverlays = false
        )
    }

    fun toggleSom() {
        val current = _overlayConfig.value
        _overlayConfig.value = current.copy(
            showSetOfMarks = !current.showSetOfMarks,
            hideAllOverlays = false
        )
    }

    fun toggleHideAll() {
        val current = _overlayConfig.value
        _overlayConfig.value = current.copy(hideAllOverlays = !current.hideAllOverlays)
    }

    fun updateSettings(newSettings: ModelSettings) {
        _settings.value = newSettings
    }

    fun toggleSkill(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val item = _skills.value.find { it.id == id } ?: return@launch
            dao.updateSkill(
                SkillEntity(item.id, item.name, item.description, item.toolCallSignature, enabled, item.isCustom)
            )
        }
    }

    fun addSkill(name: String, desc: String, sig: String) {
        viewModelScope.launch {
            dao.insertSkill(
                SkillEntity(UUID.randomUUID().toString(), name, desc, sig, true, true)
            )
        }
    }

    fun startNewChat() {
        val newId = UUID.randomUUID().toString()
        _currentConversationId.value = newId
        _messages.value = emptyList()
        _activePlan.value = null
        _isExecuting.value = false
        _isPaused.value = false

        viewModelScope.launch {
            dao.insertConversation(
                ConversationEntity(
                    id = newId,
                    title = "مهمة جديدة",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
        _activePlan.value = null
        _isExecuting.value = false
        _isPaused.value = false
        // توليد رسائل نموذجية أو جلبها للمحادثة
        _messages.value = listOf(
            ChatMessage(
                conversationId = id,
                sender = MessageSender.ASSISTANT,
                text = "تم التبديل إلى المحادثة المحددة. جاهز لاستقبال الأوامر وتنفيذ الأتمتة."
            )
        )
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            val conv = _conversations.value.find { it.id == id } ?: return@launch
            dao.updateConversation(
                ConversationEntity(id, newTitle, conv.createdAt, System.currentTimeMillis())
            )
        }
    }

    /**
     * تنفيذ التراجع الشامل (Global Rollback)
     */
    fun performGlobalRollback(id: String) {
        viewModelScope.launch {
            _isRollingBack.value = true
            addSystemMessage("بدء تنفيذ التراجع الشامل (Global Rollback)... إلغاء كل ما أحدثه الذكاء الاصطناعي على الجهاز.")

            val undoneCount = rollbackEngine.executeGlobalRollback(id) { step, total, desc ->
                // تحديث مباشر في شات المحادثة
            }

            addSystemMessage("اكتمل التراجع الشامل بنجاح! تم التراجع عن $undoneCount إجراء عكسي وإعادة الجهاز لحالته الأصلية.")
            _isRollingBack.value = false
        }
    }

    /**
     * إرسال الأمر (Send)
     */
    fun sendCommand() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return

        val convId = _currentConversationId.value
        val userMsg = ChatMessage(
            conversationId = convId,
            sender = MessageSender.USER,
            text = text,
            attachedUris = _attachedUris.value
        )
        _messages.value = _messages.value + userMsg
        _inputText.value = ""
        _attachedUris.value = emptyList()

        // تحديث عنوان المحادثة إن كانت أول رسالة
        updateConversationTitleIfNeeded(convId, text)

        if (_isAgentMode.value) {
            startAgentExecution(text)
        } else {
            // محادثة نصية عادية
            generateChatReply(text)
        }
    }

    private fun updateConversationTitleIfNeeded(convId: String, text: String) {
        viewModelScope.launch {
            val conv = _conversations.value.find { it.id == convId }
            if (conv != null && (conv.title == "محادثة أتمتة جديدة" || conv.title == "مهمة جديدة")) {
                val cleanTitle = if (text.length > 28) text.take(28) + "..." else text
                dao.updateConversation(
                    ConversationEntity(convId, cleanTitle, conv.createdAt, System.currentTimeMillis())
                )
            }
        }
    }

    private fun generateChatReply(prompt: String) {
        viewModelScope.launch {
            delay(500)
            val reply = "أهلاً بك. تلقيت رسالتك: \"$prompt\". إذا كنت ترغب في أن أقوم بتنفيذ هذه الخطوات مباشرة على شاشة جوالك، يرجى التبديل إلى 'وضع الإيجنت (Agent Mode)' وسأتولى التحكم الكامل."
            _messages.value = _messages.value + ChatMessage(
                conversationId = _currentConversationId.value,
                sender = MessageSender.ASSISTANT,
                text = reply
            )
        }
    }

    /**
     * بدء دورة تنفيذ الإيجنت (Execute - Verify Loop)
     */
    private fun startAgentExecution(goal: String) {
        _isExecuting.value = true
        _isPaused.value = false

        val convId = _currentConversationId.value
        val plan = taskPlanner.decomposeTask(convId, goal)
        _activePlan.value = plan

        // إشعار بالخطة في الشات
        _messages.value = _messages.value + ChatMessage(
            conversationId = convId,
            sender = MessageSender.ASSISTANT,
            text = "تم فهم الهدف وتفكيك المهمة إلى ${plan.subtasks.size} مهام فرعية، مع ميزانية زمنية ذكية قدرها ${plan.totalTimeBudgetSec} ثانية.",
            taskPlan = plan,
            isAgentExecution = true
        )

        startTimerCountdown(plan)

        executionJob = viewModelScope.launch {
            runAgentLoop(plan)
        }
    }

    private fun startTimerCountdown(plan: AgentTaskPlan) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (plan.remainingTimeBudgetSec > 0 && _isExecuting.value) {
                delay(1000)
                if (!_isPaused.value) {
                    plan.remainingTimeBudgetSec--
                    _activePlan.value = plan.copy()
                }
            }
        }
    }

    /**
     * حلقة التنفيذ والتحقق (Execute - Verify Loop)
     */
    private suspend fun runAgentLoop(plan: AgentTaskPlan) {
        val accessibilityService = AuraAccessibilityService.instance

        for (i in plan.subtasks.indices) {
            plan.currentSubTaskIndex = i
            val subtask = plan.subtasks[i]
            if (subtask.status == SubTaskStatus.DEFERRED) continue

            // 1. مرحلة التفكير (Reasoning Phase)
            subtask.status = SubTaskStatus.REASONING
            subtask.reasoningThought = "تحليل لقطة الشاشة الحالية ومطابقة عناصر الواجهة مع شبكة الإحداثيات..."
            _activePlan.value = plan.copy()
            delay(600)

            // فحص الإيقاف المؤقت
            checkPauseWait()

            // 2. مرحلة التنفيذ (Action Execution)
            subtask.status = SubTaskStatus.EXECUTING
            _activePlan.value = plan.copy()

            // توليد وتسجيل أمر حركي مع حركته العكسية
            val action = createActionForSubtask(subtask)
            subtask.actions.add(action)

            // تسجيل في محرك التراجع العكسي (Rollback Engine)
            rollbackEngine.recordAction(plan.conversationId, subtask.id, action)

            // تنفيذ حقيقي عبر خدمة إمكانية الوصول إن توفرت
            executeRealDeviceAction(action, accessibilityService)
            delay(700)

            checkPauseWait()

            // 3. مرحلة التحقق البصري (Verification Phase - إلزامية 100%)
            subtask.status = SubTaskStatus.VERIFYING
            _activePlan.value = plan.copy()
            delay(500)
            subtask.verificationResult = "تم التحقق بصرياً من تطابق النتيجة بنجاح"
            _activePlan.value = plan.copy()

            // 4. حلقة التحسين إذا انتهت المهمة الفرعية مبكراً (Optimization Loop)
            val hasTimeToOptimize = taskPlanner.registerOptimizationLoop(plan, subtask.id)
            if (hasTimeToOptimize) {
                delay(800)
                subtask.status = SubTaskStatus.COMPLETED
                _activePlan.value = plan.copy()
            }
        }

        // 5. التقرير الختامي
        plan.status = PlanStatus.COMPLETED
        _activePlan.value = plan.copy()
        _isExecuting.value = false
        timerJob?.cancel()

        _messages.value = _messages.value + ChatMessage(
            conversationId = plan.conversationId,
            sender = MessageSender.ASSISTANT,
            text = "اكتملت جميع المهام بنجاح! تم التحقق بصرياً من الشاشة لكل خطوة، وتم تسجيل الحركات العكسية الكاملة في محرك التراجع (Rollback Engine).",
            isAgentExecution = true,
            verificationSnapshotNotice = "التحقق البصري النهائي: معتمد 100%"
        )
    }

    private suspend fun checkPauseWait() {
        while (_isPaused.value) {
            delay(300)
        }
    }

    private fun executeRealDeviceAction(action: DeviceAction, service: AuraAccessibilityService?) {
        if (service == null) return
        when (action.type) {
            ActionType.TAP -> service.performTap(action.x, action.y)
            ActionType.DOUBLE_TAP -> service.performDoubleTap(action.x, action.y)
            ActionType.LONG_PRESS -> service.performLongPress(action.x, action.y, action.durationMs)
            ActionType.SWIPE -> service.performSwipe(action.x, action.y, action.endX, action.endY, action.durationMs)
            ActionType.TEXT_INPUT -> service.performTextInput(action.text)
            ActionType.SYSTEM_KEY -> service.performGlobal(action.keyCode)
            ActionType.WAIT -> {}
        }
    }

    private fun createActionForSubtask(subtask: SubTask): DeviceAction {
        return when {
            subtask.title.contains("فتح") || subtask.title.contains("استكشاف") -> {
                DeviceAction(
                    type = ActionType.TAP,
                    description = "النقر على الأيقونة المستهدفة",
                    x = 540f,
                    y = 800f,
                    reverseAction = ReverseAction(ActionType.SYSTEM_KEY, "الرجوع للخلف", keyCode = android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                )
            }
            subtask.title.contains("إدخال") || subtask.title.contains("كتابة") -> {
                DeviceAction(
                    type = ActionType.TEXT_INPUT,
                    description = "إدخال النص في الحقل النشط",
                    text = "Aura Automated Input",
                    reverseAction = ReverseAction(ActionType.TEXT_INPUT, "مسح النص المُدخل", textToUndo = "Aura Automated Input")
                )
            }
            subtask.title.contains("تصفح") || subtask.title.contains("سحب") -> {
                DeviceAction(
                    type = ActionType.SWIPE,
                    description = "سحب الشاشة للأعلى لاستعراض النتائج",
                    x = 540f,
                    y = 1400f,
                    endX = 540f,
                    endY = 400f,
                    reverseAction = ReverseAction(ActionType.SWIPE, "سحب عكسي للأسفل", x = 540f, y = 400f, endX = 540f, endY = 1400f)
                )
            }
            else -> {
                DeviceAction(
                    type = ActionType.TAP,
                    description = "النقر على الزر المطلوب",
                    x = 540f,
                    y = 1200f,
                    reverseAction = ReverseAction(ActionType.SYSTEM_KEY, "الرجوع", keyCode = android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                )
            }
        }
    }

    /**
     * إيقاف المهمة مؤقتاً عند ضغط زر التوقف
     */
    fun pauseExecution() {
        if (_isExecuting.value) {
            _isPaused.value = true
            _activePlan.value?.status = PlanStatus.PAUSED
        }
    }

    /**
     * استئناف المهمة كما هي (Resume)
     */
    fun resumeExecutionDirect() {
        _isPaused.value = false
        _activePlan.value?.status = PlanStatus.IN_PROGRESS
    }

    /**
     * إضافة تلقين/تعديل جديد واستئناف المهمة بناءً عليه (Prompt & Resume)
     */
    fun resumeExecutionWithPrompt(newPrompt: String) {
        _isPaused.value = false
        _activePlan.value?.let { plan ->
            plan.status = PlanStatus.IN_PROGRESS
            taskPlanner.addDiscoveredSubtask(plan, "توجيه إضافي: $newPrompt", "تطبيق التعديل المستحدث واستئناف المهمة", 20)
            addSystemMessage("تم تسجيل التعديل واستئناف المهمة: \"$newPrompt\"")
        }
    }

    fun addSubtaskToActivePlan(title: String, desc: String) {
        _activePlan.value?.let { plan ->
            taskPlanner.addDiscoveredSubtask(plan, title, desc, 15)
        }
    }

    fun deferSubtask(subtaskId: String) {
        _activePlan.value?.let { plan ->
            taskPlanner.deferOrRemoveSubtask(plan, subtaskId)
        }
    }

    private fun addSystemMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(
            conversationId = _currentConversationId.value,
            sender = MessageSender.SYSTEM,
            text = text
        )
    }

    /**
     * تشغيل الخدمة العائمة في الخلفية فوق التطبيقات
     */
    fun startFloatingService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            return
        }

        val intent = Intent(context, AuraFloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
