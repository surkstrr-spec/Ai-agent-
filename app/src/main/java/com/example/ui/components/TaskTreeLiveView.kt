package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentTaskPlan
import com.example.data.model.SubTask
import com.example.data.model.SubTaskStatus
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun TaskTreeLiveView(
    plan: AgentTaskPlan,
    onAddSubtask: () -> Unit,
    onDeferSubtask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_tree_live_view"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // شريط العنوان والميزانية الزمنية الديناميكية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "التخطيط الزمني الديناميكي",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الوقت المتبقي: ${plan.remainingTimeBudgetSec}s / ${plan.totalTimeBudgetSec}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onAddSubtask,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("add_subtask_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة مهمة فرعية جديدة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // شريط تقدم الوقت الإجمالي
            val progress = if (plan.totalTimeBudgetSec > 0) {
                (plan.totalTimeBudgetSec - plan.remainingTimeBudgetSec).toFloat() / plan.totalTimeBudgetSec.toFloat()
            } else 0f

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // قائمة المهام الفرعية (Sub-tasks)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                plan.subtasks.forEach { subtask ->
                    SubTaskItemRow(
                        subtask = subtask,
                        onDefer = { onDeferSubtask(subtask.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubTaskItemRow(
    subtask: SubTask,
    onDefer: () -> Unit
) {
    val (statusColor, statusIcon) = when (subtask.status) {
        SubTaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Pending
        SubTaskStatus.REASONING -> MaterialTheme.colorScheme.primary to Icons.Default.HourglassTop
        SubTaskStatus.EXECUTING -> MaterialTheme.colorScheme.primary to Icons.Default.PlayArrow
        SubTaskStatus.VERIFYING -> MaterialTheme.colorScheme.secondary to Icons.Default.Visibility
        SubTaskStatus.OPTIMIZING -> StatusWarning to Icons.Default.AutoAwesome
        SubTaskStatus.ASKING_INFO -> StatusWarning to Icons.Default.QuestionMark
        SubTaskStatus.COMPLETED -> StatusSuccess to Icons.Default.CheckCircle
        SubTaskStatus.DEFERRED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) to Icons.Default.Pending
        SubTaskStatus.FAILED -> MaterialTheme.colorScheme.error to Icons.Default.Pending
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${subtask.index}. ${subtask.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ميزانية المهمة الفرعية وحالتها
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${subtask.status.titleAr} (${subtask.timeBudgetSec}s)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            // إشعار حلقة التحسين إذا كانت نشطة
            if (subtask.status == SubTaskStatus.OPTIMIZING) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "انتهت مبكراً: استغلال الوقت في حلقة التحسين والفحص (دورة ${subtask.optimizationIterations})",
                        fontSize = 10.sp,
                        color = StatusWarning,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // فحص التحقق البصري
            if (subtask.verificationResult.isNotBlank()) {
                Text(
                    text = "التحقق البصري: ${subtask.verificationResult}",
                    fontSize = 10.sp,
                    color = StatusSuccess,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
