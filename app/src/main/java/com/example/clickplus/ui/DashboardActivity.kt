@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.example.clickplus.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AppMode
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.ClickPlusProfile
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.RuleAdvancedMetadata
import com.example.clickplus.data.SystemActionPreset
import com.example.clickplus.data.TriggerType
import com.example.clickplus.service.ActionExecutor
import com.example.clickplus.service.BasicActionPerformer
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import com.example.clickplus.service.RuleExecutionCoordinator
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private sealed interface DashboardRoute {
    data object Home : DashboardRoute
    data object Status : DashboardRoute
    data object Logs : DashboardRoute
    data object Profiles : DashboardRoute
    data object Settings : DashboardRoute
    data object Backup : DashboardRoute
    data class Editor(val id: String?) : DashboardRoute
}

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 90)
        }
        setContent { ClickPlusDashboard() }
    }
}

@Composable
private fun ClickPlusDashboard() {
    val context = LocalContext.current
    val prefs = remember { AppPreferencesRepository(context.applicationContext) }
    val advanced = remember { AdvancedRuleRepository(context.applicationContext) }
    val mappings by prefs.mappingsFlow.collectAsState(initial = emptyList())
    val timeout by prefs.tapTimeoutFlow.collectAsState(initial = 650L)
    val showTapCount by prefs.showTapCountFlow.collectAsState(initial = false)
    var route by remember { mutableStateOf<DashboardRoute>(DashboardRoute.Home) }
    var themeMode by remember { mutableStateOf(advanced.themeMode()) }

    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        "dynamic" -> if (Build.VERSION.SDK_INT >= 31) androidx.compose.foundation.isSystemInDarkTheme() else false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val scheme = when (themeMode) {
        "dynamic" -> if (Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (dark) darkColorScheme() else lightColorScheme()
        "dark" -> darkColorScheme()
        "light" -> lightColorScheme()
        else -> if (dark) darkColorScheme() else lightColorScheme()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme) {
            when (val current = route) {
            DashboardRoute.Home -> HomeDashboard(
                mappings = mappings,
                onAdd = { route = DashboardRoute.Editor(null) },
                onEdit = { route = DashboardRoute.Editor(it) },
                onStatus = { route = DashboardRoute.Status },
                onLogs = { route = DashboardRoute.Logs },
                onProfiles = { route = DashboardRoute.Profiles },
                onSettings = { route = DashboardRoute.Settings },
                onTest = { route = DashboardRoute.Logs },
            )
            DashboardRoute.Status -> StatusScreen(
                mappings = mappings,
                onBack = { route = DashboardRoute.Home },
                onMode = { mode -> AdvancedRuleRepository.setMode(context, mode) },
            )
            DashboardRoute.Logs -> LogsScreen(onBack = { route = DashboardRoute.Home })
            DashboardRoute.Profiles -> ProfilesScreen(
                mappings = mappings,
                onBack = { route = DashboardRoute.Home },
            )
            DashboardRoute.Settings -> SettingsScreen(
                timeout = timeout,
                showTapCount = showTapCount,
                themeMode = themeMode,
                onBack = { route = DashboardRoute.Home },
                onTimeout = { value -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapTimeout(value) } },
                onShowTapCount = { value -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveShowTapCount(value) } },
                onTheme = {
                    themeMode = it
                    advanced.saveThemeMode(it)
                },
                onBackup = { route = DashboardRoute.Backup },
            )
            DashboardRoute.Backup -> BackupScreen(
                mappings = mappings,
                timeout = timeout,
                showTapCount = showTapCount,
                onBack = { route = DashboardRoute.Settings },
            )
            is DashboardRoute.Editor -> EditorScreen(
                existing = mappings.firstOrNull { it.id == current.id },
                onBack = { route = DashboardRoute.Home },
                onSave = { item, metadata ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val next = if (mappings.any { it.id == item.id }) {
                            mappings.map { if (it.id == item.id) item else it }
                        } else mappings + item
                        prefs.saveMappings(next)
                        advanced.saveRuleMetadata(item.id, metadata)
                        route = DashboardRoute.Home
                    }
                },
                onDelete = { item ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        prefs.saveMappings(mappings.filterNot { it.id == item.id })
                        advanced.removeRuleMetadata(item.id)
                        route = DashboardRoute.Home
                    }
                },
            )
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    mappings: List<KeyActionConfig>,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onStatus: () -> Unit,
    onLogs: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
    onTest: () -> Unit,
) {
    val context = LocalContext.current
    val mode = AdvancedRuleRepository.currentMode(context)
    var query by remember { mutableStateOf("") }
    val active = mappings.count { it.enabled }
    val apps = mappings.mapNotNull {
        when {
            it.triggerType == TriggerType.SCREEN_TAP -> it.screenTapPackage
            it.actionType == ActionType.APP -> it.targetPackage
            it.contextConditionType == ContextConditionType.APP -> it.contextConditionValue
            else -> null
        }
    }.filter { it.isNotBlank() }.distinct().size
    val filtered = mappings.filter {
        val q = query.trim()
        q.isBlank() || listOf(
            it.name, it.actionSummary(), it.contextSummary(), it.targetAppName, it.screenTapAppName
        ).any { value -> value.contains(q, true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("קליק פלוס", fontWeight = FontWeight.Bold)
                        Text("מרכז שליטה", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = onStatus) { Icon(Icons.Outlined.Tune, "מצב השירות") }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "הגדרות") }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onAdd, Modifier.weight(1f).height(54.dp)) {
                        Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("הוספת פעולה")
                    }
                    OutlinedButton(onClick = onTest, Modifier.weight(1f).height(54.dp)) {
                        Icon(Icons.Outlined.History, null); Spacer(Modifier.width(8.dp)); Text("יומן")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(Modifier.size(52.dp), CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Outlined.CheckCircle, "פעיל", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ClickPlus פעילה", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (mode == AppMode.FULL) "מצב מלא · נגישות פעילה" else "מצב בסיסי · ללא נגישות",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("פעולות פעילות", active.toString(), Modifier.weight(1f))
                    StatCard("אפליקציות", apps.toString(), Modifier.weight(1f))
                    StatCard("סה״כ כללים", mappings.size.toString(), Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    label = { Text("חיפוש וסינון") },
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(onClick = onProfiles, label = { Text("פרופילים") }, leadingIcon = { Icon(Icons.Outlined.Apps, null) })
                    AssistChip(onClick = onStatus, label = { Text("מצב השירות") }, leadingIcon = { Icon(Icons.Outlined.Tune, null) })
                    AssistChip(onClick = onLogs, label = { Text("יומן פעילות") }, leadingIcon = { Icon(Icons.Outlined.History, null) })
                    AssistChip(onClick = onSettings, label = { Text("הגדרות") }, leadingIcon = { Icon(Icons.Outlined.Settings, null) })
                }
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(onAdd)
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    RuleCard(item = item, onEdit = { onEdit(item.id) })
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RuleCard(item: KeyActionConfig, onEdit: () -> Unit) {
    val meta = AdvancedRuleRepository(LocalContext.current).getRuleMetadata(item.id)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name.ifBlank { item.triggerType.titleHebrew }, fontWeight = FontWeight.Bold)
                    Text(item.pressSummary() + " · " + item.contextSummary(), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "עריכה") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(item.actionSummary()) })
                AssistChip(onClick = {}, label = { Text("עדיפות " + meta.priority) })
                if (item.enabled) {
                    AssistChip(onClick = {}, label = { Text("פעיל") }, leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) })
                } else {
                    AssistChip(onClick = {}, label = { Text("מושהה") })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Add, null, Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            Text("עדיין אין פעולות", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("צור את הכלל הראשון שלך ובנה פעולה לפי כניסה או מיקום לחיצה.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd) { Text("יצירת הפעולה הראשונה") }
        }
    }
}

@Composable
private fun StatusScreen(
    mappings: List<KeyActionConfig>,
    onBack: () -> Unit,
    onMode: (AppMode) -> Unit,
) {
    val context = LocalContext.current
    val mode = AdvancedRuleRepository.currentMode(context)
    val service = isAccessibilityEnabled(context)
    val usage = hasUsageAccess(context)
    val screenRules = mappings.count { it.triggerType == TriggerType.SCREEN_TAP && it.enabled }
    Scaffold(topBar = { SimpleTopBar("מצב השירות", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusCard(
                    title = "שירות נגישות",
                    ok = service,
                    detail = if (service) "מחובר ומוכן ללחיצות במיקום המסך" else "נדרש רק במצב מלא",
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                )
            }
            item {
                StatusCard(
                    title = "שימוש בנתוני שימוש",
                    ok = usage,
                    detail = if (usage) "אפשר לזהות את האפליקציה האחרונה גם במצב בסיסי" else "מומלץ עבור תנאי אפליקציה במצב בסיסי",
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                )
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("מצב עבודה", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = mode == AppMode.FULL,
                                onClick = { onMode(AppMode.FULL) },
                                label = { Text("מלא") },
                            )
                            FilterChip(
                                selected = mode == AppMode.BASIC,
                                onClick = { onMode(AppMode.BASIC) },
                                label = { Text("Basic ללא נגישות") },
                            )
                        }
                        Text(
                            if (mode == AppMode.FULL) {
                                "כל היכולות זמינות, כולל זיהוי מיקום לחיצה ופעולות מערכת."
                            } else {
                                "הפעלות כניסה, פתיחת אפליקציות ופעולות מדיה/ווליום הנתמכות יכולות לפעול ללא שירות נגישות. זיהוי מיקום לחיצה ופעולות מערכת גלובליות לא זמינים."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("בריאות המערכת", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("פעולות פעילות: " + mappings.count { it.enabled })
                        Text("כללי מיקום: " + screenRules)
                        Text("מצב השירות: " + if (mode == AppMode.FULL && service || mode == AppMode.BASIC) "תקין" else "דורש הרשאה")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, ok: Boolean, detail: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(AdvancedRuleRepository.logs(context)) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("יומן פעילות") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "חזרה") } },
                actions = {
                    TextButton(onClick = {
                        AdvancedRuleRepository.clearLogs(context)
                        items = emptyList()
                    }) { Text("ניקוי") }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("אין אירועים להצגה עדיין.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items) { log ->
                    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(log.message)
                            if (log.appPackage.isNotBlank()) {
                                Text(log.appPackage, style = MaterialTheme.typography.labelSmall)
                            }
                            if (log.xRatio >= 0f && log.yRatio >= 0f) {
                                Text(
                                    "מיקום: X " + (log.xRatio * 100f).toInt() + "% · Y " + (log.yRatio * 100f).toInt() + "%",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilesScreen(mappings: List<KeyActionConfig>, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AdvancedRuleRepository(context) }
    var profiles by remember { mutableStateOf(repo.profiles()) }
    var dialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("פרופילים ומצבים") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "חזרה") } },
                actions = { IconButton(onClick = { newName = ""; dialog = true }) { Icon(Icons.Outlined.Add, "פרופיל חדש") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(profiles, key = { it.id }) { profile ->
                val count = mappings.count {
                    AdvancedRuleRepository(context).getRuleMetadata(it.id).profileId == profile.id
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.name, fontWeight = FontWeight.Bold)
                            Text(count.toString() + " כללים", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = profile.enabled,
                            onCheckedChange = {
                                profiles = profiles.map { p -> if (p.id == profile.id) p.copy(enabled = it) else p }
                                repo.saveProfiles(profiles)
                            },
                        )
                        if (profile.id != "default") {
                            IconButton(onClick = {
                                profiles = profiles.filterNot { it.id == profile.id }
                                repo.saveProfiles(profiles)
                            }) { Icon(Icons.Outlined.Delete, "מחיקה") }
                        }
                    }
                }
            }
        }
    }

    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("פרופיל חדש") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("שם הפרופיל") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotBlank()) {
                        profiles = profiles + ClickPlusProfile(name = name)
                        repo.saveProfiles(profiles)
                    }
                    dialog = false
                }) { Text("הוספה") }
            },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("ביטול") } },
        )
    }
}

@Composable
private fun SettingsScreen(
    timeout: Long,
    showTapCount: Boolean,
    themeMode: String,
    onBack: () -> Unit,
    onTimeout: (Long) -> Unit,
    onShowTapCount: (Boolean) -> Unit,
    onTheme: (String) -> Unit,
    onBackup: () -> Unit,
) {
    Scaffold(topBar = { SimpleTopBar("הגדרות", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("זמן חלון לחיצות", "כמה זמן יש בין כניסות/לחיצות חוזרות.") {
                    Text(timeout.toString() + "ms", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = timeout.toFloat(),
                        onValueChange = { onTimeout(it.toLong()) },
                        valueRange = 300f..1500f,
                        steps = 11,
                    )
                }
            }
            item {
                SettingCard("הצגת מונה לחיצות", "הצג מספר לחיצות זמנית מעל המסך.") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("מונה פעיל", Modifier.weight(1f))
                        Switch(checked = showTapCount, onCheckedChange = onShowTapCount)
                    }
                }
            }
            item {
                SettingCard("מראה", "ערכת צבעים ועדכון אוטומטי לפי המכשיר.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("מערכת", "system", themeMode, onTheme, Icons.Outlined.BrightnessAuto)
                        ThemeChip("בהיר", "light", themeMode, onTheme, Icons.Outlined.LightMode)
                        ThemeChip("כהה", "dark", themeMode, onTheme, Icons.Outlined.DarkMode)
                        if (Build.VERSION.SDK_INT >= 31) {
                            ThemeChip("דינמי", "dynamic", themeMode, onTheme, Icons.Outlined.BrightnessAuto)
                        }
                    }
                }
            }
            item {
                SettingCard("גיבוי והעברה", "ייצוא, ייבוא ואיפוס של כל ההגדרות.") {
                    Button(onClick = onBackup, Modifier.fillMaxWidth()) { Text("פתח גיבוי") }
                }
            }
            item {
                SettingCard("גרסה", "ClickPlus 2.0 · בנויה ל־Android 16") {
                    Text("Target API 36", fontWeight = FontWeight.Bold)
                    Text("מצב רקע פעיל תמיד.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, value: String, selectedValue: String, onTheme: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FilterChip(
        selected = selectedValue == value,
        onClick = { onTheme(value) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
private fun BackupScreen(
    mappings: List<KeyActionConfig>,
    timeout: Long,
    showTapCount: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val advanced = remember { AdvancedRuleRepository(context) }
    val basePrefs = remember { AppPreferencesRepository(context) }
    var message by remember { mutableStateOf("") }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selected ->
            scope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(selected)?.bufferedReader()?.use { it.readText() }
                        ?: error("קובץ ריק")
                    val root = JSONObject(json)
                    val array = root.optJSONArray("mappings") ?: JSONArray()
                    val imported = buildList {
                        for (i in 0 until array.length()) {
                            array.optJSONObject(i)?.let { add(KeyActionConfig.fromJson(it)) }
                        }
                    }
                    basePrefs.saveMappings(imported)
                    root.optJSONObject("settings")?.let {
                        if (it.has("tapTimeoutMs")) basePrefs.saveTapTimeout(it.optLong("tapTimeoutMs", 650L))
                        if (it.has("showTapCount")) basePrefs.saveShowTapCount(it.optBoolean("showTapCount", false))
                    }
                    advanced.importBundle(root)
                    message = "הגיבוי יובא בהצלחה."
                }.onFailure { message = "הייבוא נכשל: " + (it.message ?: "שגיאה") }
            }
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { selected ->
            val json = advanced.exportJson(
                JSONArray().apply { mappings.forEach { put(it.toJson()) } }.toString(),
                JSONObject().put("tapTimeoutMs", timeout).put("showTapCount", showTapCount)
            )
            runCatching {
                context.contentResolver.openOutputStream(selected)?.bufferedWriter()?.use { it.write(json) }
                message = "הגיבוי נשמר בהצלחה."
            }.onFailure { message = "שמירת הגיבוי נכשלה." }
        }
    }

    Scaffold(topBar = { SimpleTopBar("גיבוי והעברה", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("ייצוא ושיתוף", "שמור או שתף את כל הפעולות, הפרופילים וההגדרות.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("clickplus-backup.json") },
                            Modifier.weight(1f),
                        ) { Text("ייצוא") }
                        OutlinedButton(
                            onClick = {
                                val json = advanced.exportJson(
                                    JSONArray().apply { mappings.forEach { put(it.toJson()) } }.toString(),
                                    JSONObject().put("tapTimeoutMs", timeout).put("showTapCount", showTapCount)
                                )
                                runCatching {
                                    val file = java.io.File(context.cacheDir, "clickplus-backup.json")
                                    file.writeText(json)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        context.packageName + ".fileprovider",
                                        file,
                                    )
                                    context.startActivity(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }.let { Intent.createChooser(it, "שיתוף גיבוי ClickPlus") }
                                    )
                                    message = "נפתח מסך השיתוף."
                                }.onFailure {
                                    message = "השיתוף נכשל: " + (it.message ?: "שגיאה")
                                }
                            },
                            Modifier.weight(1f),
                        ) { Text("שיתוף") }
                    }
                }
            }
            item {
                SettingCard("ייבוא", "החלפת ההגדרות הנוכחיות בתוכן מקובץ גיבוי.") {
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        Modifier.fillMaxWidth(),
                    ) { Text("ייבוא גיבוי") }
                }
            }
            item {
                SettingCard("איפוס", "מחיקת כל הפעולות והמטא־נתונים של ClickPlus.") {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                basePrefs.saveMappings(emptyList())
                                advanced.clearAllRuleMetadata()
                                advanced.saveProfiles(listOf(ClickPlusProfile("default", "כללי", true)))
                                message = "ההגדרות אופסו."
                            }
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("איפוס פעולות") }
                }
            }
            if (message.isNotBlank()) {
                item { AssistChip(onClick = {}, label = { Text(message) }, leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) }) }
            }
        }
    }
}

@Composable
private fun EditorScreen(
    existing: KeyActionConfig?,
    onBack: () -> Unit,
    onSave: (KeyActionConfig, RuleAdvancedMetadata) -> Unit,
    onDelete: (KeyActionConfig) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AdvancedRuleRepository(context) }
    var draft by remember(existing?.id) {
        mutableStateOf(existing ?: KeyActionConfig(
            name = "פעולה חדשה",
            triggerType = TriggerType.APP_ENTRY,
            enabled = true,
        ))
    }
    var metadata by remember(existing?.id) {
        mutableStateOf(repo.getRuleMetadata(existing?.id ?: ""))
    }
    var appDialog by remember { mutableStateOf(false) }
    var screenAppDialog by remember { mutableStateOf(false) }
    var learning by remember { mutableStateOf(false) }
    var orientation by remember { mutableStateOf(if (context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait") }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(draft.id) {
        while (true) {
            val p = context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            if (p.getBoolean("tap_capture_ready", false)) {
                val x = p.getFloat("tap_capture_x_ratio", -1f)
                val y = p.getFloat("tap_capture_y_ratio", -1f)
                val pkg = p.getString("tap_capture_package", "").orEmpty()
                val name = p.getString("tap_capture_app_name", "").orEmpty()
                if (x >= 0f && y >= 0f) {
                    draft = draft.copy(
                        triggerType = TriggerType.SCREEN_TAP,
                        screenTapPackage = pkg,
                        screenTapAppName = name,
                        screenTapXRatio = x,
                        screenTapYRatio = y,
                    )
                    metadata = if (orientation == "landscape") {
                        metadata.copy(landscapeX = x, landscapeY = y)
                    } else {
                        metadata.copy(portraitX = x, portraitY = y)
                    }
                }
                p.edit().putBoolean("tap_capture_ready", false).apply()
                learning = false
            }
            kotlinx.coroutines.delay(250)
        }
    }

    val x = if (orientation == "landscape" && metadata.landscapeX >= 0f) metadata.landscapeX else if (metadata.portraitX >= 0f) metadata.portraitX else draft.screenTapXRatio.coerceAtLeast(0f)
    val y = if (orientation == "landscape" && metadata.landscapeY >= 0f) metadata.landscapeY else if (metadata.portraitY >= 0f) metadata.portraitY else draft.screenTapYRatio.coerceAtLeast(0f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "הוספת פעולה" else "עריכת פעולה") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "חזרה") } },
                actions = {
                    if (existing != null) IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, "מחיקה") }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        if (draft.name.isBlank()) draft = draft.copy(name = draft.triggerType.titleHebrew)
                        onSave(
                            draft.copy(
                                screenTapXRatio = if (draft.screenTapXRatio >= 0f) draft.screenTapXRatio else x,
                                screenTapYRatio = if (draft.screenTapYRatio >= 0f) draft.screenTapYRatio else y,
                                screenTapToleranceRatio = maxOf(metadata.toleranceXRatio, metadata.toleranceYRatio),
                            ),
                            metadata,
                        )
                    },
                    Modifier.fillMaxWidth().padding(12.dp).height(54.dp),
                ) {
                    Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(8.dp)); Text("שמירת פעולה")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("פרטי הכלל", "שם ברור יעזור למצוא את הפעולה ביומן ובדשבורד.") {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("שם פעולה") },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("פעולה פעילה", Modifier.weight(1f))
                        Switch(checked = draft.enabled, onCheckedChange = { draft = draft.copy(enabled = it) })
                    }
                }
            }
            item {
                SettingCard("מתי להפעיל?", "בחר סוג טריגר ואת מספר הכניסות/לחיצות.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = draft.triggerType == TriggerType.APP_ENTRY, onClick = { draft = draft.copy(triggerType = TriggerType.APP_ENTRY) }, label = { Text("כניסה") })
                        FilterChip(selected = draft.triggerType == TriggerType.SCREEN_TAP, onClick = { draft = draft.copy(triggerType = TriggerType.SCREEN_TAP) }, label = { Text("מיקום מסך") })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { n ->
                            FilterChip(selected = draft.pressCount == n, onClick = { draft = draft.copy(pressCount = n) }, label = { Text(n.toString()) })
                        }
                    }
                }
            }
            if (draft.triggerType == TriggerType.APP_ENTRY) {
                item {
                    SettingCard("באיזה מצב?", "התאמה לאפליקציה/מצב לפני הכניסה ל־ClickPlus.") {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ContextConditionType.entries.forEach { type ->
                                FilterChip(
                                    selected = draft.contextConditionType == type,
                                    onClick = { draft = draft.copy(contextConditionType = type) },
                                    label = { Text(type.titleHebrew) },
                                )
                            }
                        }
                        if (draft.contextConditionType == ContextConditionType.APP || draft.contextConditionType == ContextConditionType.RADIO) {
                            Button(onClick = { appDialog = true }, Modifier.fillMaxWidth()) {
                                Text(
                                    if (draft.contextConditionName.isBlank()) "בחירת אפליקציה"
                                    else draft.contextConditionName
                                )
                            }
                            Text("הבדיקה מתבצעת מול האפליקציה שהייתה פתוחה לפני ClickPlus.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                item {
                    SettingCard("לימוד מיקום", "פתח את האפליקציה, לחץ על היעד ושמור את המיקום. לאחר מכן ניתן לדייק אותו ידנית.") {
                        Button(
                            onClick = {
                                if (!isAccessibilityEnabled(context)) {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                } else if (draft.screenTapPackage.isBlank()) {
                                    screenAppDialog = true
                                } else {
                                    val p = context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                                    p.edit().putBoolean("tap_learning", true).putString("tap_learning_package", draft.screenTapPackage).apply()
                                    context.packageManager.getLaunchIntentForPackage(draft.screenTapPackage)?.let {
                                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(it)
                                        learning = true
                                    }
                                }
                            },
                            Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.LocationOn, null); Spacer(Modifier.width(8.dp))
                            Text(if (draft.screenTapPackage.isBlank()) "בחירת אפליקציה ולימוד" else "למד מיקום לחיצה")
                        }
                        if (learning) Text("שכבת לימוד פעילה: לחץ על היעד באפליקציה שנפתחה.", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = orientation == "portrait", onClick = { orientation = "portrait" }, label = { Text("אנכי") })
                            FilterChip(selected = orientation == "landscape", onClick = { orientation = "landscape" }, label = { Text("אופקי") })
                        }
                        if (draft.screenTapPackage.isNotBlank()) {
                            Text(draft.screenTapAppName, fontWeight = FontWeight.Bold)
                            Text("X " + (x * 100f).toInt() + "% · Y " + (y * 100f).toInt() + "%")
                            PointEditor(
                                x = x,
                                y = y,
                                toleranceX = metadata.toleranceXRatio,
                                toleranceY = metadata.toleranceYRatio,
                                onChange = { nx, ny ->
                                    draft = draft.copy(screenTapXRatio = nx, screenTapYRatio = ny)
                                    metadata = if (orientation == "landscape") metadata.copy(landscapeX = nx, landscapeY = ny)
                                    else metadata.copy(portraitX = nx, portraitY = ny)
                                },
                            )
                        }
                    }
                }
            }
            item {
                SettingCard("מה לבצע?", "הפעולה עצמה לאחר התאמת הכלל.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = draft.actionType == ActionType.SYSTEM, onClick = { draft = draft.copy(actionType = ActionType.SYSTEM) }, label = { Text("מערכת") })
                        FilterChip(selected = draft.actionType == ActionType.APP, onClick = { draft = draft.copy(actionType = ActionType.APP) }, label = { Text("אפליקציה") })
                    }
                    if (draft.actionType == ActionType.SYSTEM) {
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(220.dp)) {
                            SystemActionPreset.entries.forEach { item ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { draft = draft.copy(systemActionId = item.id) }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FilterChip(selected = draft.systemActionId == item.id, onClick = { draft = draft.copy(systemActionId = item.id) }, label = { Text(item.titleHebrew) })
                                }
                            }
                        }
                    } else {
                        Button(onClick = { appDialog = true }, Modifier.fillMaxWidth()) {
                            Text(if (draft.targetAppName.isBlank()) "בחירת אפליקציית יעד" else draft.targetAppName)
                        }
                    }
                }
            }
            item {
                SettingCard("אפשרויות מתקדמות", "התנהגות מדויקת יותר לכלל.") {
                    Text("עדיפות: " + metadata.priority)
                    Slider(value = metadata.priority.toFloat(), onValueChange = { metadata = metadata.copy(priority = it.toInt()) }, valueRange = 0f..10f, steps = 9)
                    Text("Cooldown: " + metadata.cooldownMs + "ms")
                    Slider(value = metadata.cooldownMs.toFloat(), onValueChange = { metadata = metadata.copy(cooldownMs = it.toLong()) }, valueRange = 0f..10000f, steps = 9)
                    Text("השהיה לפני פעולה: " + metadata.delayMs + "ms")
                    Slider(value = metadata.delayMs.toFloat(), onValueChange = { metadata = metadata.copy(delayMs = it.toLong()) }, valueRange = 0f..5000f, steps = 9)
                    Text("ניסיונות: " + metadata.retries)
                    Slider(value = metadata.retries.toFloat(), onValueChange = { metadata = metadata.copy(retries = it.toInt().coerceIn(1,3)) }, valueRange = 1f..3f, steps = 1)
                    if (draft.triggerType == TriggerType.SCREEN_TAP) {
                        Text("רוחב אזור התאמה: " + (metadata.toleranceXRatio * 100f).toInt() + "%")
                        Slider(value = metadata.toleranceXRatio, onValueChange = { metadata = metadata.copy(toleranceXRatio = it) }, valueRange = 0.01f..0.25f)
                        Text("גובה אזור התאמה: " + (metadata.toleranceYRatio * 100f).toInt() + "%")
                        Slider(value = metadata.toleranceYRatio, onValueChange = { metadata = metadata.copy(toleranceYRatio = it) }, valueRange = 0.01f..0.25f)
                    }
                    ProfileSelector(
                        profiles = repo.profiles(),
                        selectedId = metadata.profileId,
                        onSelect = { metadata = metadata.copy(profileId = it) },
                    )
                    OutlinedButton(
                        onClick = {
                            val performer = if (isAccessibilityEnabled(context)) {
                                KeyInterceptorAccessibilityService.instance?.let { ActionExecutor(it) }
                            } else null
                            if (performer != null) {
                                RuleExecutionCoordinator(context, performer).execute(draft, test = true, reason = "בדיקה ידנית")
                            } else {
                                RuleExecutionCoordinator(context, BasicActionPerformer(context)).execute(draft, test = true, reason = "בדיקה במצב בסיסי")
                            }
                        },
                        Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("בדיקת פעולה")
                    }
                }
            }
        }
    }

    if (appDialog) {
        AppPickerDialog(
            title = "בחירת אפליקציה",
            onDismiss = { appDialog = false },
            onSelect = { app ->
                if (draft.contextConditionType == ContextConditionType.APP || draft.contextConditionType == ContextConditionType.RADIO) {
                    draft = draft.copy(contextConditionValue = app.packageName, contextConditionName = app.label)
                } else {
                    draft = draft.copy(targetPackage = app.packageName, targetAppName = app.label)
                }
                appDialog = false
            },
        )
    }
    if (screenAppDialog) {
        AppPickerDialog(
            title = "אפליקציית יעד ללימוד",
            onDismiss = { screenAppDialog = false },
            onSelect = { app ->
                draft = draft.copy(screenTapPackage = app.packageName, screenTapAppName = app.label)
                screenAppDialog = false
            },
        )
    }
    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("למחוק את הפעולה?") },
            text = { Text("הכלל וההגדרות המתקדמות שלו יוסרו.") },
            confirmButton = { TextButton(onClick = { showDelete = false; onDelete(existing) }) { Text("מחיקה") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("ביטול") } },
        )
    }
}

private data class InstalledApp(val packageName: String, val label: String)

@Composable
private fun AppPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (InstalledApp) -> Unit,
) {
    val context = LocalContext.current
    val apps = remember {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { info ->
                val launchIntent = context.packageManager.getLaunchIntentForPackage(info.packageName)
                    ?: return@mapNotNull null
                val label = runCatching {
                    context.packageManager.getApplicationLabel(info).toString()
                }.getOrDefault(info.packageName)
                if (launchIntent != null) InstalledApp(info.packageName, label) else null
            }
            .filterNot { it.packageName == context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
    var query by remember { mutableStateOf("") }
    val filtered = apps.filter {
        it.label.contains(query, true) || it.packageName.contains(query, true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("חיפוש אפליקציה") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
            )
            LazyColumn(
                Modifier.fillMaxWidth().height(460.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onSelect(app) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            Modifier.size(42.dp),
                            CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Icon(Icons.Outlined.Apps, null, Modifier.padding(9.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Bold)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSelector(profiles: List<ClickPlusProfile>, selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("פרופיל")
        profiles.forEach { profile ->
            FilterChip(
                selected = selectedId == profile.id,
                onClick = { onSelect(profile.id) },
                label = { Text(profile.name) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PointEditor(
    x: Float,
    y: Float,
    toleranceX: Float,
    toleranceY: Float,
    onChange: (Float, Float) -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("תצוגה מקדימה · גרור את הנקודה למיקום המדויק")
        Box(
            Modifier.fillMaxWidth().height(260.dp).background(
                surfaceColor,
                RoundedCornerShape(18.dp),
            ),
        ) {
            Canvas(
                Modifier.fillMaxSize().padding(14.dp).pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                        val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                        onChange(nx, ny)
                        change.consume()
                    }
                }
            ) {
                val px = x * size.width
                val py = y * size.height
                drawRect(
                    color = outlineColor.copy(alpha = 0.35f),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
                )
                drawRect(
                    color = primaryColor.copy(alpha = 0.16f),
                    topLeft = Offset((x - toleranceX).coerceAtLeast(0f) * size.width, (y - toleranceY).coerceAtLeast(0f) * size.height),
                    size = androidx.compose.ui.geometry.Size(
                        ((toleranceX * 2f).coerceAtMost(1f)) * size.width,
                        ((toleranceY * 2f).coerceAtMost(1f)) * size.height,
                    ),
                )
                drawCircle(primaryColor.copy(alpha = 0.18f), radius = 32f, center = Offset(px, py))
                drawCircle(primaryColor, radius = 11f, center = Offset(px, py))
            }
        }
    }
}

@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "חזרה") } },
    )
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.split(':').any { it.contains(context.packageName, true) }
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    return runCatching {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
}
