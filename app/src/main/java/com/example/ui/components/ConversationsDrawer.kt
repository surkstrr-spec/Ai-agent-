package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsDrawer(
    conversations: List<Conversation>,
    selectedConversationId: String,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onRenameConversation: (id: String, newTitle: String) -> Unit,
    onGlobalRollback: (id: String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var renameDialogTarget by remember { mutableStateOf<Conversation?>(null) }
    var rollbackConfirmTarget by remember { mutableStateOf<Conversation?>(null) }

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    SurfaceDrawerContent(
        searchQuery = searchQuery,
        onSearchChange = { searchQuery = it },
        filteredConversations = filteredConversations,
        selectedConversationId = selectedConversationId,
        onSelectConversation = onSelectConversation,
        onNewChat = onNewChat,
        onPromptRename = { renameDialogTarget = it },
        onPromptRollback = { rollbackConfirmTarget = it },
        onClose = onCloseDrawer,
        modifier = modifier
    )

    // مربع حوار تغيير اسم المحادثة
    renameDialogTarget?.let { conv ->
        var editedTitle by remember { mutableStateOf(conv.title) }
        AlertDialog(
            onDismissRequest = { renameDialogTarget = null },
            title = { Text("تغيير اسم المحادثة") },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_chat_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedTitle.isNotBlank()) {
                            onRenameConversation(conv.id, editedTitle.trim())
                        }
                        renameDialogTarget = null
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // مربع حوار تأكيد التراجع الشامل (Global Rollback)
    rollbackConfirmTarget?.let { conv ->
        AlertDialog(
            onDismissRequest = { rollbackConfirmTarget = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("تأكيد التراجع الشامل (Global Rollback)") },
            text = {
                Text(
                    text = "سيقوم النظام بقراءة سجل التغييرات للوراء بالتتابع العكسي وتطبيق خطوات التراجع إلغاءً لكل ما أحدثه الذكاء الاصطناعي في محادثة '${conv.title}' لإعادة الجهاز لحالته الأصلية تماماً.\n\nهل ترغب بالمتابعة؟",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGlobalRollback(conv.id)
                        rollbackConfirmTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_global_rollback_button")
                ) {
                    Text("تنفيذ التراجع الشامل")
                }
            },
            dismissButton = {
                TextButton(onClick = { rollbackConfirmTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun SurfaceDrawerContent(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filteredConversations: List<Conversation>,
    selectedConversationId: String,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onPromptRename: (Conversation) -> Unit,
    onPromptRollback: (Conversation) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversations_drawer_card"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // شريط العنوان وزر الإغلاق
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المحادثات والإيجنت",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق الدرج")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // زر محادثة جديدة (+ New Chat) في الأعلى
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_chat_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("محادثة جديدة (+ New Chat)")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // شريط بحث (Search Bar) في أعلى القائمة للبحث داخل المحادثات
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("بحث في المحادثات...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversations_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // قائمة المحادثات مع زر Rename وزر Global Rollback
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredConversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد محادثات تطابق البحث",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                items(filteredConversations, key = { it.id }) { conv ->
                    val isSelected = conv.id == selectedConversationId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable { onSelectConversation(conv.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = conv.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }

                        // الأزرار المخصصة: زر تغيير الاسم وزر التراجع الشامل
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // زر تغيير الاسم (Rename)
                            IconButton(
                                onClick = { onPromptRename(conv) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("rename_button_${conv.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تغيير اسم المحادثة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // زر التراجع الشامل (Global Rollback)
                            IconButton(
                                onClick = { onPromptRollback(conv) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("global_rollback_button_${conv.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "التراجع الشامل عن تغييرات المحادثة",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
