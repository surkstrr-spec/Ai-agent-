package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.AuraAccessibilityService
import com.example.service.AuraFloatingService
import com.example.ui.AuraViewModel
import com.example.ui.components.BottomInputBar
import com.example.ui.components.ChatArea
import com.example.ui.components.ConversationsDrawer
import com.example.ui.components.CoordinatesGridOverlay
import com.example.ui.components.PauseResumeDialog
import com.example.ui.components.SetOfMarkOverlay
import com.example.ui.components.SettingsScreen
import com.example.ui.components.TaskTreeLiveView
import com.example.ui.components.TopControlBar
import com.example.ui.theme.AuraAgentTheme
import com.example.ui.theme.StatusWarning

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: AuraViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            AuraAgentTheme(themeMode = themeMode) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: AuraViewModel) {
    val context = LocalContext.current
    val overlayConfig by viewModel.overlayConfig.collectAsState()
    val isAgentMode by viewModel.isAgentMode.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val attachedUris by viewModel.attachedUris.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val skills by viewModel.skills.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPauseResumeDialog by remember { mutableStateOf(false) }
    var showAddSubtaskDialog by remember { mutableStateOf(false) }

    // لاقط الصور والوسائط (Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.addAttachment(it.toString()) }
    }

    // التنقل الداخلي لشاشة الإعدادات
    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            currentSettings = settings,
            currentTheme = currentTheme,
            skills = skills,
            onThemeChange = { viewModel.setThemeMode(it) },
            onSaveSettings = { viewModel.updateSettings(it) },
            onToggleSkill = { id, enabled -> viewModel.toggleSkill(id, enabled) },
            onAddSkill = { name, desc, sig -> viewModel.addSkill(name, desc, sig) },
            onBack = { showSettings = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                TopControlBar(
                    overlayConfig = overlayConfig,
                    isAgentMode = isAgentMode,
                    onToggleGrid = { viewModel.toggleGrid() },
                    onToggleSom = { viewModel.toggleSom() },
                    onToggleHideAll = { viewModel.toggleHideAll() },
                    onOpenDrawer = { showDrawer = true },
                    onOpenSettings = { showSettings = true }
                )
            }
        },
        bottomBar = {
            BottomInputBar(
                inputText = inputText,
                onInputChange = { viewModel.setInputText(it) },
                isAgentMode = isAgentMode,
                onModeChange = { viewModel.setAgentMode(it) },
                isExecuting = isExecuting,
                attachedUris = attachedUris,
                onAttachClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onRemoveAttachment = { viewModel.removeAttachment(it) },
                onSend = { viewModel.sendCommand() },
                onPauseClick = {
                    viewModel.pauseExecution()
                    showPauseResumeDialog = true
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // تنبيه خدمة الوصول والنافذة العائمة إن لم تكن مفعلة
                ServiceStatusBanner(
                    onEnableAccessibility = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onStartFloatingOverlay = {
                        viewModel.startFloatingService(context)
                    }
                )

                // شجرة التخطيط الزمني الذكي النشطة في وضع الإيجنت
                activePlan?.let { plan ->
                    Spacer(modifier = Modifier.height(4.dp))
                    TaskTreeLiveView(
                        plan = plan,
                        onAddSubtask = { showAddSubtaskDialog = true },
                        onDeferSubtask = { subtaskId -> viewModel.deferSubtask(subtaskId) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // منطقة رسائل المحادثة وسجل الأفعال
                ChatArea(
                    messages = messages,
                    modifier = Modifier.weight(1f)
                )
            }

            // التراكب البصري 1: شبكة الإحداثيات (Coordinates Grid)
            if (overlayConfig.showCoordinatesGrid && !overlayConfig.hideAllOverlays) {
                CoordinatesGridOverlay()
            }

            // التراكب البصري 2: وسم العناصر بالأرقام (Set-of-Mark SoM)
            if (overlayConfig.showSetOfMarks && !overlayConfig.hideAllOverlays) {
                SetOfMarkOverlay()
            }

            // درج المحادثات المنسدل (Conversations Drawer)
            if (showDrawer) {
                BackHandler { showDrawer = false }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ConversationsDrawer(
                        conversations = conversations,
                        selectedConversationId = currentConvId,
                        onSelectConversation = { id ->
                            viewModel.selectConversation(id)
                            showDrawer = false
                        },
                        onNewChat = {
                            viewModel.startNewChat()
                            showDrawer = false
                        },
                        onRenameConversation = { id, title ->
                            viewModel.renameConversation(id, title)
                        },
                        onGlobalRollback = { id ->
                            viewModel.performGlobalRollback(id)
                            showDrawer = false
                        },
                        onCloseDrawer = { showDrawer = false }
                    )
                }
            }
        }
    }

    // مربع حوار الإيقاف المؤقت وخيارات الاستئناف (Pause & Resume Dialog)
    if (showPauseResumeDialog) {
        PauseResumeDialog(
            onDismiss = { showPauseResumeDialog = false },
            onResumeDirect = {
                viewModel.resumeExecutionDirect()
                showPauseResumeDialog = false
            },
            onResumeWithPrompt = { newPrompt ->
                viewModel.resumeExecutionWithPrompt(newPrompt)
                showPauseResumeDialog = false
            }
        )
    }

    // مربع حوار إضافة مهمة فرعية جديدة تم اكتشافها
    if (showAddSubtaskDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubtaskDialog = false },
            title = { Text("إضافة مهمة فرعية جديدة") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("عنوان المهمة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("تفاصيل التنفيذ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.addSubtaskToActivePlan(newTitle, newDesc)
                        }
                        showAddSubtaskDialog = false
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubtaskDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ServiceStatusBanner(
    onEnableAccessibility: () -> Unit,
    onStartFloatingOverlay: () -> Unit
) {
    val isAccessibilityActive = AuraAccessibilityService.isServiceRunning
    val isFloatingActive = AuraFloatingService.isRunning

    if (!isAccessibilityActive || !isFloatingActive) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isAccessibilityActive) "خدمة الأتمتة بحاجة لتفعيل في الإعدادات"
                        else "الأيقونة العائمة جاهزة للتشغيل فوق التطبيقات",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isAccessibilityActive) {
                        Button(
                            onClick = onEnableAccessibility,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("تفعيل الوصول", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onStartFloatingOverlay,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (isFloatingActive) "العائم نشط" else "تشغيل العائم", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
