package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PauseResumeDialog(
    onDismiss: () -> Unit,
    onResumeDirect: () -> Unit,
    onResumeWithPrompt: (newPrompt: String) -> Unit
) {
    var additionalPrompt by remember { mutableStateOf("") }
    var showPromptInput by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pause_resume_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تم إيقاف المهمة مؤقتاً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "يمكنك استئناف تنفيذ المهام كما هي، أو إضافة تلقين وتعديل للمسار واستئناف التنفيذ بناءً عليه.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showPromptInput) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = additionalPrompt,
                        onValueChange = { additionalPrompt = it },
                        placeholder = {
                            Text("أدخل التوجيه أو التعديل الإضافي...", fontSize = 13.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resume_prompt_input"),
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // أزرار الاختيار
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // خيار استئناف المهمة كما هي (Resume)
                    Button(
                        onClick = {
                            onResumeDirect()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resume_direct_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("استئناف المهمة كما هي (Resume)")
                    }

                    // خيار إضافة تلقين/تعديل جديد واستئناف المهمة بناءً عليه (Prompt & Resume)
                    if (!showPromptInput) {
                        OutlinedButton(
                            onClick = { showPromptInput = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("show_prompt_and_resume_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.EditNote, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("تعديل التلقين واستئناف (Prompt & Resume)")
                        }
                    } else {
                        Button(
                            onClick = {
                                onResumeWithPrompt(additionalPrompt)
                                onDismiss()
                            },
                            enabled = additionalPrompt.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_prompt_and_resume_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("استئناف مع التعديل الجديد")
                        }
                    }
                }
            }
        }
    }
}
