package com.example.engine

import com.example.data.model.AgentTaskPlan
import com.example.data.model.DeviceAction
import com.example.data.model.PlanStatus
import com.example.data.model.SubTask
import com.example.data.model.SubTaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * محرك تفكيك المهام والتخطيط الزمني والديناميكي
 * Task & Dynamic Time Planner Engine
 *
 * يتولى:
 * 1. تفكيك الهدف إلى شجرة مهام فرعية (Sub-tasks).
 * 2. تقسيم الميزانية الزمنية المتاحة بنسب منطقية ديناميكية.
 * 3. حلقة التحسين (Optimization Loop) إذا انتهت المهمة مبكراً.
 * 4. إعادة الجدولة وإعادة توزيع الوقت المتبقي عند التأخر.
 * 5. المرونة لإضافة، حذف، تأجيل، أو إعادة ترتيب المهام الفرعية.
 */
class TaskTimePlanner {

    private val _currentPlan = MutableStateFlow<AgentTaskPlan?>(null)
    val currentPlan: StateFlow<AgentTaskPlan?> = _currentPlan.asStateFlow()

    /**
     * تفكيك التلقين إلى قائمة مهام فرعية متسلسلة وتوزيع الوقت الذكي
     */
    fun decomposeTask(
        conversationId: String,
        goal: String,
        userSpecifiedBudgetSec: Int? = null
    ): AgentTaskPlan {
        // تحديد المهام الفرعية الأساسية المناسبة للمهمة
        val generatedSubtasks = generateSubtasksForGoal(goal)
        val defaultTotalBudget = when {
            userSpecifiedBudgetSec != null && userSpecifiedBudgetSec > 0 -> userSpecifiedBudgetSec
            else -> calculateDefaultBudget(goal, generatedSubtasks.size)
        }

        // توزيع الوقت بنسب منطقية ديناميكية
        distributeTimeBudget(generatedSubtasks, defaultTotalBudget)

        val plan = AgentTaskPlan(
            conversationId = conversationId,
            goal = goal,
            totalTimeBudgetSec = defaultTotalBudget,
            remainingTimeBudgetSec = defaultTotalBudget,
            subtasks = generatedSubtasks.toMutableList(),
            currentSubTaskIndex = 0,
            status = PlanStatus.PLANNING
        )

        _currentPlan.value = plan
        return plan
    }

    /**
     * تقسيم الوقت المتاح على المهام الفرعية بنسب تعتمد على وزن كل مهمة
     */
    private fun distributeTimeBudget(subtasks: List<SubTask>, totalBudgetSec: Int) {
        if (subtasks.isEmpty()) return
        val count = subtasks.size
        // المهام الأولى (الاستكشاف والإدخال) عادة ما تحتاج وزناً أعلى من مجرد التحقق
        val weights = subtasks.mapIndexed { index, task ->
            when {
                task.title.contains("استكشاف") || task.title.contains("فتح") -> 1.2
                task.title.contains("إدخال") || task.title.contains("كتابة") -> 1.4
                task.title.contains("تنفيذ") || task.title.contains("تطبيق") -> 1.3
                task.title.contains("تحقق") || task.title.contains("تأكيد") -> 1.0
                else -> 1.0
            }
        }
        val totalWeight = weights.sum()

        var allocated = 0
        subtasks.forEachIndexed { index, task ->
            val fraction = weights[index] / totalWeight
            val seconds = max(5, (totalBudgetSec * fraction).toInt())
            task.timeBudgetSec = seconds
            allocated += seconds
        }

        // ضبط الفرق لتطابق الإجمالي
        val diff = totalBudgetSec - allocated
        if (diff != 0 && subtasks.isNotEmpty()) {
            subtasks.last().timeBudgetSec = max(5, subtasks.last().timeBudgetSec + diff)
        }
    }

    /**
     * إعادة جدولة الوقت وتوزيعه ديناميكياً إذا لم تكفِ المدة المحددة لمهمة فرعية
     */
    fun rebalanceRemainingBudget(
        plan: AgentTaskPlan,
        extraTimeNeededSec: Int
    ) {
        val remainingTasks = plan.subtasks.filterIndexed { index, _ ->
            index > plan.currentSubTaskIndex &&
            plan.subtasks[index].status != SubTaskStatus.COMPLETED
        }

        if (remainingTasks.isEmpty()) {
            // تمديد الميزانية الكلية إذا لزم الأمر
            plan.remainingTimeBudgetSec += extraTimeNeededSec
            return
        }

        // خصم أجزاء طفيفة من المهام اللاحقة بالتناسب لدعم المهمة الحالية
        val deductionPerTask = extraTimeNeededSec / remainingTasks.size
        for (task in remainingTasks) {
            val deduct = minOf(task.timeBudgetSec - 5, deductionPerTask)
            if (deduct > 0) {
                task.timeBudgetSec -= deduct
            }
        }
        _currentPlan.value = plan.copy()
    }

    /**
     * استغلال الوقت المتبقي في حلقة تحسين واختبار إضافية (Optimization Loop)
     */
    fun registerOptimizationLoop(plan: AgentTaskPlan, subTaskId: String): Boolean {
        val subtask = plan.subtasks.find { it.id == subTaskId } ?: return false
        val remainingInTask = subtask.timeBudgetSec - subtask.elapsedSec

        if (remainingInTask > 3) {
            subtask.status = SubTaskStatus.OPTIMIZING
            subtask.optimizationIterations++
            _currentPlan.value = plan.copy()
            return true
        } else {
            subtask.status = SubTaskStatus.COMPLETED
            _currentPlan.value = plan.copy()
            return false
        }
    }

    /**
     * إضافة مهمة فرعية جديدة تم اكتشافها ديناميكياً أثناء التنفيذ
     */
    fun addDiscoveredSubtask(
        plan: AgentTaskPlan,
        title: String,
        description: String,
        budgetSec: Int = 15
    ) {
        val newSubtask = SubTask(
            index = plan.subtasks.size + 1,
            title = title,
            description = description,
            timeBudgetSec = budgetSec,
            isNewDiscovered = true
        )
        plan.subtasks.add(newSubtask)
        plan.totalTimeBudgetSec += budgetSec
        plan.remainingTimeBudgetSec += budgetSec
        _currentPlan.value = plan.copy()
    }

    /**
     * تأجيل أو حذف مهمة فرعية غير ضرورية
     */
    fun deferOrRemoveSubtask(plan: AgentTaskPlan, subTaskId: String, remove: Boolean = false) {
        val target = plan.subtasks.find { it.id == subTaskId } ?: return
        if (remove) {
            plan.subtasks.remove(target)
            // إعادة توزيع وقتها على المهام الباقية
            val active = plan.subtasks.filter { it.status == SubTaskStatus.PENDING }
            if (active.isNotEmpty()) {
                val share = target.timeBudgetSec / active.size
                active.forEach { it.timeBudgetSec += share }
            }
        } else {
            target.status = SubTaskStatus.DEFERRED
        }
        _currentPlan.value = plan.copy()
    }

    /**
     * توليد ذكي لقائمة المهام الفرعية بناءً على نص الأمر
     */
    private fun generateSubtasksForGoal(goal: String): List<SubTask> {
        val lower = goal.lowercase()
        return when {
            lower.contains("بحث") || lower.contains("search") -> listOf(
                SubTask(1, "فتح تطبيق البحث أو المتصفح", "التعرف على أيقونة البحث وفتحها", 15),
                SubTask(2, "تحديد شريط الإدخال وكتابة النص", "إدخال مصطلح البحث المطلوب وتأكيد الإرسال", 20),
                SubTask(3, "تصفح النتائج والتحقق البصري", "قراءة النتيجة الأولى وفحص المحتوى بالرؤية", 25)
            )
            lower.contains("رسالة") || lower.contains("message") || lower.contains("واتساب") || lower.contains("whatsapp") -> listOf(
                SubTask(1, "الوصول إلى تطبيق المراسلة", "استعراض الشاشة والضغط على التطبيق المعني", 15),
                SubTask(2, "اختيار جهة الاتصال المستهدفة", "البحث عن الاسم أو النقر على الدردشة", 20),
                SubTask(3, "كتابة نص الرسالة وإرسالها", "إدخال الرسالة بدقة والنقر على زر الإرسال", 20),
                SubTask(4, "التحقق البصري من وصول الرسالة", "التأكد من علامات الإرسال والاطمئنان للحالة", 15)
            )
            lower.contains("صورة") || lower.contains("كاميرا") || lower.contains("معرض") || lower.contains("photo") -> listOf(
                SubTask(1, "فتح معرض الصور أو الكاميرا", "تحديد مكان التطبيق والدخول إليه", 15),
                SubTask(2, "استعراض والتقاط/اختيار الصورة", "النقر على الصورة المطلوبة بدقة الوسم SoM", 25),
                SubTask(3, "التأكد البصري وإتمام الإجراء", "فحص الصورة عبر نموذج الرؤية والتأكد من تمام المهمة", 20)
            )
            lower.contains("إعدادات") || lower.contains("بلوتوث") || lower.contains("واي فاي") || lower.contains("wifi") -> listOf(
                SubTask(1, "سحب شريط الإشعارات أو فتح الإعدادات", "الوصول لقسم الضبط المستهدف", 15),
                SubTask(2, "تحديد الخيار المطلوب وتغيير الحالة", "الضغط بدقة على زر التبديل", 20),
                SubTask(3, "التحقق البصري من تفعيل الحالة", "قراءة الرمز بعد التغيير للتأكد من نجاح الإجراء", 15)
            )
            else -> listOf(
                SubTask(1, "الاستكشاف البصري للشاشة الحالية", "تحديد عناصر واجهة المستخدم وشبكة الإحداثيات", 20),
                SubTask(2, "تنفيذ الأوامر الحركية المتسلسلة", "النقر والإدخال والتفاعل مع الأزرار المطلوبة", 30),
                SubTask(3, "التحقق البصري النهائي والتقرير", "التقاط الشاشة والتأكد من مطابقة النتيجة للهدف", 20)
            )
        }
    }

    private fun calculateDefaultBudget(goal: String, subtaskCount: Int): Int {
        // استخراج أي أرقام تشير لدقائق أو ثوانٍ في التلقين
        val regexSeconds = "(\\d+)\\s*(ثانية|ثوان|sec|seconds)".toRegex(RegexOption.IGNORE_CASE)
        val matchSec = regexSeconds.find(goal)
        if (matchSec != null) {
            val num = matchSec.groupValues[1].toIntOrNull()
            if (num != null && num > 5) return num
        }

        val regexMinutes = "(\\d+)\\s*(دقيقة|دقائق|min|minutes)".toRegex(RegexOption.IGNORE_CASE)
        val matchMin = regexMinutes.find(goal)
        if (matchMin != null) {
            val num = matchMin.groupValues[1].toIntOrNull()
            if (num != null && num > 0) return num * 60
        }

        return max(45, subtaskCount * 22)
    }
}
