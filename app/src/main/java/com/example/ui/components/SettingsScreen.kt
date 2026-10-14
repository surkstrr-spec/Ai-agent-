package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.LocalGgufConfig
import com.example.data.model.ModelProvider
import com.example.data.model.ModelSettings
import com.example.data.model.SkillItem
import com.example.data.model.ThemeMode
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: ModelSettings,
    currentTheme: ThemeMode,
    skills: List<SkillItem>,
    onThemeChange: (ThemeMode) -> Unit,
    onSaveSettings: (ModelSettings) -> Unit,
    onToggleSkill: (skillId: String, isEnabled: Boolean) -> Unit,
    onAddSkill: (name: String, desc: String, sig: String) -> Unit,
    onBack: () -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }
    var selectedProvider by remember { mutableStateOf(currentSettings.provider) }

    // حقول النموذج المحلي
    var localModelName by remember { mutableStateOf(currentSettings.localGguf.modelName) }
    var mainModelPath by remember { mutableStateOf(currentSettings.localGguf.mainModelPath) }
    var mmprojPath by remember { mutableStateOf(currentSettings.localGguf.mmprojPath) }
    var isEmbeddedMmproj by remember { mutableStateOf(currentSettings.localGguf.isEmbeddedMmproj) }

    // مهارة جديدة
    var newSkillName by remember { mutableStateOf("") }
    var newSkillDesc by remember { mutableStateOf("") }
    var showAddSkillForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الوكيل والنماذج", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val updated = settings.copy(
                                provider = selectedProvider,
                                localGguf = LocalGgufConfig(
                                    mainModelPath = mainModelPath,
                                    mmprojPath = if (isEmbeddedMmproj) "" else mmprojPath,
                                    isEmbeddedMmproj = isEmbeddedMmproj,
                                    modelName = localModelName
                                )
                            )
                            onSaveSettings(updated)
                            onBack()
                        },
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "حفظ الإعدادات",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. التحكم بالثيمات (Theme Management)
            SettingsCard(title = "التحكم بالثيمات (Theme)", icon = Icons.Default.Palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = currentTheme == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(mode.labelAr, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).testTag("theme_chip_${mode.name}")
                        )
                    }
                }
            }

            // 2. إلزامية المعالجة البصرية (Strict Mandatory Vision Enforcement)
            SettingsCard(
                title = "إلزامية المعالجة البصرية (Strict Vision)",
                icon = Icons.Default.Visibility
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الرؤية البصرية الإلزامية (100% Screenshots)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "مفعلة بشكل صارم ودائم لكل مهمة وخطوة دون استثناء",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = { /* Locked by policy */ },
                            enabled = false,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StatusSuccess,
                                checkedTrackColor = StatusSuccess.copy(alpha = 0.3f)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusSuccess.copy(alpha = 0.08f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "ملاحظة هندسية: يُمنع منعاً باتاً استثناء الرؤية أو إيقافها، فالتقاط لقطات الشاشة وتحليلها بصرياً شرط إجباري في كل مهمة وفي كل خطوة لتفادي الأخطاء وضمان دقة الإحداثيات.",
                            fontSize = 11.sp,
                            color = StatusSuccess,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 3. تعليمات النظام (System Message / System Prompt Customization)
            SettingsCard(title = "تعليمات النظام (System Prompt)", icon = Icons.Default.Description) {
                OutlinedTextField(
                    value = settings.systemPrompt,
                    onValueChange = { settings = settings.copy(systemPrompt = it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("system_prompt_input"),
                    label = { Text("برومبت النظام الأساسي") },
                    maxLines = 6
                )
            }

            // 4. إعدادات الـ API الخارجي ومزود النماذج
            SettingsCard(title = "مزود الذكاء الاصطناعي و الـ API", icon = Icons.Default.Cloud) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر المزود:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModelProvider.values().forEach { prov ->
                            FilterChip(
                                selected = selectedProvider == prov,
                                onClick = { selectedProvider = prov },
                                label = { Text(prov.name, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (selectedProvider != ModelProvider.LOCAL) {
                        OutlinedTextField(
                            value = settings.apiKey,
                            onValueChange = { settings = settings.copy(apiKey = it) },
                            label = { Text("API Key") },
                            placeholder = { Text("أدخل مفتاح الـ API...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = settings.baseUrl,
                            onValueChange = { settings = settings.copy(baseUrl = it) },
                            label = { Text("Base URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = settings.remoteModelId,
                            onValueChange = { settings = settings.copy(remoteModelId = it) },
                            label = { Text("Model ID (مثال: gemini-2.5-flash / gpt-4o)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // 5. إضافة نموذج محلي (Local Model Setup)
            SettingsCard(title = "إضافة نموذج محلي (Local GGUF + mmproj)", icon = Icons.Default.Memory) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = localModelName,
                        onValueChange = { localModelName = it },
                        label = { Text("اسم النموذج (Model Name)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("local_model_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = mainModelPath,
                        onValueChange = { mainModelPath = it },
                        label = { Text("النموذج الرئيسي (Main Model GGUF)") },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // زر تنبيه "النموذج مدمج الرؤية" (Embedded mmproj Toggle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "النموذج مدمج الرؤية (Embedded mmproj)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تفعيل هذا الخيار يعني أن ملف الـ GGUF يمتلك قدرات الرؤية مدمجة تلقائياً ولا يتطلب ملف mmproj منفصل.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEmbeddedMmproj,
                            onCheckedChange = { isEmbeddedMmproj = it },
                            modifier = Modifier.testTag("embedded_mmproj_switch")
                        )
                    }

                    // خانة mmproj تظهر فقط إذا لم يكن مدمجاً
                    AnimatedVisibility(visible = !isEmbeddedMmproj) {
                        OutlinedTextField(
                            value = mmprojPath,
                            onValueChange = { mmprojPath = it },
                            label = { Text("نموذج الرؤية (Projector / mmproj GGUF)") },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            selectedProvider = ModelProvider.LOCAL
                            val updated = settings.copy(
                                provider = ModelProvider.LOCAL,
                                localGguf = LocalGgufConfig(
                                    mainModelPath = mainModelPath,
                                    mmprojPath = if (isEmbeddedMmproj) "" else mmprojPath,
                                    isEmbeddedMmproj = isEmbeddedMmproj,
                                    modelName = localModelName.ifBlank { "Local-Vision-GGUF" }
                                )
                            )
                            onSaveSettings(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_local_model_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة وحفظ النموذج المحلي (+ Add)")
                    }
                }
            }

            // 6. قسم المهارات (Skill Kit Manager)
            SettingsCard(title = "قسم المهارات (Skill Kit Manager)", icon = Icons.Default.Construction) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "المهارات والأدوات المسجلة لوكيل الأتمتة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    skills.forEach { skill ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(skill.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(skill.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = skill.isEnabled,
                                onCheckedChange = { onToggleSkill(skill.id, it) }
                            )
                        }
                    }

                    if (!showAddSkillForm) {
                        Button(
                            onClick = { showAddSkillForm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل مهارة جديدة (+ Add Skill)")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("تسجيل مهارة أو أداة جديدة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = newSkillName,
                                onValueChange = { newSkillName = it },
                                label = { Text("اسم المهارة") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newSkillDesc,
                                onValueChange = { newSkillDesc = it },
                                label = { Text("وصف المهارة والمدخلات") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (newSkillName.isNotBlank()) {
                                            onAddSkill(newSkillName, newSkillDesc, "{}")
                                            newSkillName = ""
                                            newSkillDesc = ""
                                            showAddSkillForm = false
                                        }
                                    }
                                ) {
                                    Text("حفظ المهارة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
