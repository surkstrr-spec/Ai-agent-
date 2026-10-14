package com.example.data.model

enum class SubTaskStatus(val titleAr: String) {
    PENDING("قيد الانتظار"),
    REASONING("يفكر"),
    EXECUTING("ينفذ"),
    VERIFYING("يتأكد بصرياً"),
    OPTIMIZING("حلقة التحسين والاختبار"),
    ASKING_INFO("طلب معلومات"),
    COMPLETED("مكتملة"),
    DEFERRED("مؤجلة"),
    FAILED("تعذّر التنفيذ")
}

data class SubTask(
    val index: Int,
    val title: String,
    val description: String,
    var timeBudgetSec: Int,
    val id: String = java.util.UUID.randomUUID().toString(),
    var elapsedSec: Int = 0,
    var status: SubTaskStatus = SubTaskStatus.PENDING,
    var reasoningThought: String = "",
    var verificationResult: String = "",
    var optimizationIterations: Int = 0,
    val actions: MutableList<DeviceAction> = mutableListOf(),
    var isNewDiscovered: Boolean = false
)

enum class PlanStatus(val titleAr: String) {
    PLANNING("تفكيك وتخطيط المهمة"),
    IN_PROGRESS("قيد التنفيذ النشط"),
    PAUSED("متوقف مؤقتاً"),
    WAITING_INPUT("بانتظار مدخلات المستخدم"),
    COMPLETED("اكتملت المهمة بنجاح"),
    ROLLED_BACK("تم التراجع الشامل"),
    FAILED("توقفت المهمة مع خطأ")
}

data class AgentTaskPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val goal: String,
    var totalTimeBudgetSec: Int,
    var remainingTimeBudgetSec: Int,
    var subtasks: MutableList<SubTask> = mutableListOf(),
    var currentSubTaskIndex: Int = 0,
    var status: PlanStatus = PlanStatus.PLANNING,
    val createdAt: Long = System.currentTimeMillis()
)
