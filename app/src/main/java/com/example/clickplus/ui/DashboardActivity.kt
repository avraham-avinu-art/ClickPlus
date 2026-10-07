@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.clickplus.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.example.clickplus.service.UnavailableActionPerformer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private var runtimeRequestActive = false
    private var showPermissionIntro by mutableStateOf(false)
    private var notificationRequestAttempted = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        runtimeRequestActive = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        showPermissionIntro = !prefs.getBoolean("permission_intro_completed", false)
        prefs.edit()
            .putBoolean("first_ui_opened", true)
            .putBoolean("background_only", false)
            .apply()

        setContent {
            ClickPlusDashboard(
                showPermissionIntro = showPermissionIntro,
                onBeginPermissionSetup = {
                    prefs.edit()
                        .putBoolean("permission_intro_completed", true)
                        .putBoolean("permission_bootstrap_done", true)
                        .apply()
                    showPermissionIntro = false
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            this@DashboardActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED &&
                        !notificationRequestAttempted
                    ) {
                        notificationRequestAttempted = true
                        runtimeRequestActive = true
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onLaterPermissionSetup = {
                    prefs.edit()
                        .putBoolean("permission_intro_completed", true)
                        .putBoolean("permission_bootstrap_done", true)
                        .apply()
                    showPermissionIntro = false
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
            .edit()
            .putBoolean("background_only", false)
            .apply()
    }
}

@Composable
private fun ClickPlusDashboard(
    showPermissionIntro: Boolean,
    onBeginPermissionSetup: () -> Unit,
    onLaterPermissionSetup: () -> Unit,
) {
    val context = LocalContext.current
    if (showPermissionIntro) {
        PermissionIntroScreen(onBeginPermissionSetup, onLaterPermissionSetup)
        return
    }

    val prefs = remember { AppPreferencesRepository(context.applicationContext) }
    val advanced = remember { AdvancedRuleRepository(context.applicationContext) }
    val mappings by prefs.mappingsFlow.collectAsState(initial = emptyList())
    val timeout by prefs.tapTimeoutFlow.collectAsState(initial = 1200L)
    val actionDelay by prefs.actionDelayFlow.collectAsState(initial = 0L)
    val showTapCount by prefs.showTapCountFlow.collectAsState(initial = false)
    val tapCountPosition by prefs.tapCountPositionFlow.collectAsState(initial = 35)
    var route by remember { mutableStateOf<DashboardRoute>(DashboardRoute.Home) }
    var themeMode by remember { mutableStateOf(advanced.themeMode()) }
    var mode by remember { mutableStateOf(AdvancedRuleRepository.currentMode(context)) }

    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        "dynamic" -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val scheme = when (themeMode) {
        "dark" -> androidx.compose.material3.darkColorScheme()
        "light" -> androidx.compose.material3.lightColorScheme()
        "dynamic" -> if (Build.VERSION.SDK_INT >= 31) {
            if (dark) androidx.compose.material3.dynamicDarkColorScheme(context)
            else androidx.compose.material3.dynamicLightColorScheme(context)
        } else if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
        else -> if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(colorScheme = scheme) {
            when (val current = route) {
                DashboardRoute.Home -> HomeDashboard(
                    mappings = mappings,
                    onAdd = { route = DashboardRoute.Editor(null) },
                    onEdit = { route = DashboardRoute.Editor(it) },
                    onDelete = { item ->
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            prefs.saveMappings(mappings.filterNot { it.id == item.id })
                            advanced.removeRuleMetadata(item.id)
                        }
                    },
                    onStatus = { route = DashboardRoute.Status },
                    onLogs = { route = DashboardRoute.Logs },
                    onProfiles = { route = DashboardRoute.Profiles },
                    onSettings = { route = DashboardRoute.Settings },
                )
                DashboardRoute.Status -> StatusScreen(
                    mappings = mappings,
                    onBack = { route = DashboardRoute.Home },
                )
                DashboardRoute.Logs -> LogsScreen(onBack = { route = DashboardRoute.Home })
                DashboardRoute.Profiles -> ProfilesScreen(
                    mappings = mappings,
                    onBack = { route = DashboardRoute.Home },
                )
                DashboardRoute.Settings -> SettingsScreen(
                    timeout = timeout,
                    actionDelay = actionDelay,
                    showTapCount = showTapCount,
                    tapCountPosition = tapCountPosition,
                    currentMode = mode,
                    themeMode = themeMode,
                    onBack = { route = DashboardRoute.Home },
                    onTimeout = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapTimeout(v) } },
                    onActionDelay = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveActionDelay(v) } },
                    onShowTapCount = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveShowTapCount(v) } },
                    onTapCountPosition = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapCountPosition(v) } },
                    onMode = { v -> mode = v; AdvancedRuleRepository.setMode(context, v) },
                    onTheme = { v -> themeMode = v; advanced.saveThemeMode(v) },
                    onBackup = { route = DashboardRoute.Backup },
                )
                DashboardRoute.Backup -> BackupScreen(
                    mappings = mappings,
                    timeout = timeout,
                    showTapCount = showTapCount,
                    actionDelay = actionDelay,
                    onBack = { route = DashboardRoute.Settings },
                )
                is DashboardRoute.Editor -> EditorScreen(
                    existing = mappings.firstOrNull { it.id == current.id },
                    onBack = { route = DashboardRoute.Home },
                    onSave = { item, metadata ->
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            val next = if (mappings.any { it.id == item.id }) {
                                mappings.map { if (it.id == item.id) item else it }
                            } else {
                                mappings + item
                            }
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
    onDelete: (KeyActionConfig) -> Unit,
    onStatus: () -> Unit,
    onLogs: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val mode = AdvancedRuleRepository.currentMode(context)
    val accessibility = isAccessibilityEnabled(context)
    var query by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<KeyActionConfig?>(null) }

    val filtered = mappings.filter {
        val q = query.trim()
        q.isBlank() || listOf(
            it.name,
            it.pressSummary(),
            it.actionSummary(),
            it.contextSummary(),
        ).any { value -> value.contains(q, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("קליק פלוס", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, "הגדרות")
                    }
                    HelpIconButton(
                        "הסבר על המסך הראשי",
                        "כאן נמצאות הפעולות שהגדרת. לחיצה על פעולה פותחת אותה לעריכה. בתחתית הרשימה נמצאים כפתורי הוספת פעולה ויומן.",
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth().clickable(onClick = onStatus),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(50.dp), CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("קליק פלוס פעיל", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (mode == AppMode.FULL && accessibility) "מצב מלא · שירות נגישות פעיל"
                                else if (mode == AppMode.FULL) "מצב מלא · שירות נגישות לא פעיל"
                                else "מצב בסיסי ללא נגישות",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    label = { Text("חיפוש פעולות") },
                )
            }
            if (filtered.isEmpty()) {
                item { EmptyState(onAdd) }
            } else {
                items(filtered, key = { it.id }) { item ->
                    RuleCard(
                        item = item,
                        onEdit = { onEdit(item.id) },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAdd, Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("הוספת פעולה")
                    }
                    OutlinedButton(onClick = onLogs, Modifier.weight(1f)) {
                        Icon(Icons.Outlined.History, null)
                        Spacer(Modifier.width(6.dp))
                        Text("יומן")
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("למחוק את הפעולה?") },
            text = { Text("הפעולה וההגדרות שלה יוסרו.") },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; onDelete(target) }) { Text("מחיקה") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("ביטול") } },
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Add, null, Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            Text("עדיין אין פעולות", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "צור את הפעולה הראשונה שלך לפי כניסה או לפי לחיצה אוטומטית.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd) { Text("יצירת הפעולה הראשונה") }
        }
    }
}

@Composable
private fun RuleCard(
    item: KeyActionConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name.ifBlank { "פעולה" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        item.pressSummary() + " · " + item.contextSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onEdit, Modifier.size(40.dp)) { Icon(Icons.Outlined.Edit, "עריכה") }
                IconButton(onClick = onDelete, Modifier.size(40.dp)) { Icon(Icons.Outlined.Delete, "מחיקה") }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssistSummaryChip(item.actionSummary())
                AssistSummaryChip(if (item.enabled) "פעיל" else "מושהה")
            }
        }
    }
}

@Composable
private fun AssistSummaryChip(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PermissionIntroScreen(
    onBegin: () -> Unit,
    onLater: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    HelpIconButton(
                        "למה יש הרשאות?",
                        "שירות הנגישות נדרש לזיהוי לחיצות ולביצוע פעולות. גישה לנתוני שימוש היא אפשרית רק במצב בסיסי כאשר צריך לזהות אפליקציה פעילה. הרשאת טלפון נדרשת רק למצבי שיחה. אפשר להיכנס לאפליקציה גם בלי לתת אותן עכשיו.",
                    )
                }
            }
            item {
                Surface(Modifier.size(72.dp), CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp))
                }
            }
            item {
                Text("הגדרה ראשונית", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            item {
                Text(
                    "אפשר להיכנס ולהגדיר את האפליקציה עכשיו. הרשאות נוספות ניתנות בנפרד לפי הצורך.",
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("הרשאות ומה הן מאפשרות", fontWeight = FontWeight.Bold)
                        Text("• שירות נגישות – זיהוי לחיצות אוטומטיות באפליקציות אחרות ופעולות מערכת.")
                        Text("• נתוני שימוש – זיהוי אפליקציה פעילה רק כשנדרש במצב בסיסי.")
                        Text("• התראות – הצגת הודעת השירות.")
                        Text("• טלפון – רק למצבים ופעולות הקשורים לשיחות.")
                    }
                }
            }
            item {
                Button(onClick = onBegin, Modifier.fillMaxWidth().height(52.dp)) {
                    Text("הענקת הרשאות עכשיו")
                }
            }
            item {
                OutlinedButton(onClick = onLater, Modifier.fillMaxWidth().height(50.dp)) {
                    Text("מאוחר יותר – כניסה לאפליקציה")
                }
            }
        }
    }
}

@Composable
private fun StatusScreen(
    mappings: List<KeyActionConfig>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val mode = AdvancedRuleRepository.currentMode(context)
    val service = isAccessibilityEnabled(context)
    val usage = hasUsageAccess(context)
    val notification = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val phone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    Scaffold(topBar = { SimpleTopBar("מצב השירות", onBack, "כאן מוצגים רק הסטטוסים: מצב העבודה וההרשאות. אין כאן שינוי הגדרות.") }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                StatusCard("מצב עבודה", true, if (mode == AppMode.FULL) "מצב מלא" else "מצב בסיסי ללא נגישות")
            }
            item { StatusCard("שירות נגישות", service, if (service) "פעיל" else "כבוי") }
            item { StatusCard("התראות", notification, if (notification) "ההרשאה פעילה" else "ההרשאה אינה פעילה") }
            item {
                StatusCard(
                    "שימוש בנתוני שימוש",
                    usage,
                    if (usage) "הגישה פעילה" else "הגישה אינה פעילה · אינה נדרשת להפעלה רגילה במצב מלא",
                )
            }
            item {
                StatusCard(
                    "הרשאת טלפון",
                    phone,
                    if (phone) "פעילה" else "לא ניתנה · נדרשת רק למצבי/פעולות שיחה",
                )
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("סטטוס המערכת", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("פעולות פעילות: " + mappings.count { it.enabled })
                        Text("כללי לחיצה במיקום: " + mappings.count {
                            it.enabled && (it.triggerType == TriggerType.SCREEN_TAP || it.triggerType == TriggerType.APP_TAP)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, ok: Boolean, detail: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(AdvancedRuleRepository.logs(context)) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) logs = AdvancedRuleRepository.logs(context)
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("יומן פעילות") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "חזרה") } },
                actions = {
                    HelpIconButton(
                        "הסבר על יומן הפעילות",
                        "כל רשומה כוללת תאריך, שעה, שם הפעולה, תוצאת הביצוע והסבר במקרה של כישלון.",
                    )
                    IconButton(onClick = { AdvancedRuleRepository.clearLogs(context); logs = emptyList() }) {
                        Icon(Icons.Outlined.Delete, "ניקוי")
                    }
                },
            )
        },
    ) { padding ->
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("אין אירועים להצגה עדיין.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(logs, key = { it.id }) { log ->
                    val (status, bg, fg) = when (log.success) {
                        true -> Triple("הצליח", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        false -> Triple("נכשל", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                        null -> Triple("ממתין", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Surface(shape = RoundedCornerShape(9.dp), color = bg) {
                                    Text(status, color = fg, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(log.timestamp)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(log.actionLabel.ifBlank { log.message }, fontWeight = FontWeight.Bold)
                            if (log.message.isNotBlank() && log.message != log.actionLabel) Text(log.message, style = MaterialTheme.typography.bodyMedium)
                            if (log.success == false) Text("סיבת הכישלון: " + log.detail.ifBlank { "לא נמסר הסבר." })
                            else if (log.detail.isNotBlank()) Text(log.detail, style = MaterialTheme.typography.bodySmall)
                            if (log.appPackage.isNotBlank()) Text(log.appPackage, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilesScreen(
    mappings: List<KeyActionConfig>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { AdvancedRuleRepository(context) }
    var profiles by remember { mutableStateOf(repo.profiles()) }
    var newProfileDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val activeId = AdvancedRuleRepository.activeProfileId(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("פרופילים ומצבים") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "חזרה") } },
                actions = {
                    HelpIconButton(
                        "הסבר על פרופילים ומצבים",
                        "פרופיל מרכז קבוצה של פעולות. פעולת 'פרופיל הבא' או 'פרופיל קודם' יכולה להחליף את הפרופיל הפעיל.",
                    )
                    IconButton(onClick = { newName = ""; newProfileDialog = true }) { Icon(Icons.Outlined.Add, "פרופיל חדש") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("הפרופיל הפעיל", fontWeight = FontWeight.Bold)
                        Text(profiles.firstOrNull { it.id == activeId }?.name ?: "ברירת מחדל")
                    }
                }
            }
            items(profiles, key = { it.id }) { profile ->
                val count = mappings.count { repo.getRuleMetadata(it.id).profileId == profile.id }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.name, fontWeight = FontWeight.Bold)
                            Text(count.toString() + " פעולות", style = MaterialTheme.typography.bodySmall)
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

    if (newProfileDialog) {
        AlertDialog(
            onDismissRequest = { newProfileDialog = false },
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
                    newProfileDialog = false
                }) { Text("הוספה") }
            },
            dismissButton = { TextButton(onClick = { newProfileDialog = false }) { Text("ביטול") } },
        )
    }
}

@Composable
private fun SettingsScreen(
    timeout: Long,
    actionDelay: Long,
    showTapCount: Boolean,
    tapCountPosition: Int,
    currentMode: AppMode,
    themeMode: String,
    onBack: () -> Unit,
    onTimeout: (Long) -> Unit,
    onActionDelay: (Long) -> Unit,
    onShowTapCount: (Boolean) -> Unit,
    onTapCountPosition: (Int) -> Unit,
    onMode: (AppMode) -> Unit,
    onTheme: (String) -> Unit,
    onBackup: () -> Unit,
) {
    val context = LocalContext.current
    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }

    Scaffold(topBar = { SimpleTopBar("הגדרות", onBack, "כאן משנים את מצב העבודה, חלון הלחיצות, השהיה, מונה הלחיצות, המראה והגיבוי.") }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("מצב עבודה", "בחירת מצב העבודה נעשית כאן בלבד.") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip(
                            selected = selectedMode == AppMode.FULL,
                            onClick = { selectedMode = AppMode.FULL; onMode(AppMode.FULL) },
                            label = "מצב מלא",
                        )
                        ChoiceChip(
                            selected = selectedMode == AppMode.BASIC,
                            onClick = { selectedMode = AppMode.BASIC; onMode(AppMode.BASIC) },
                            label = "מצב בסיסי ללא נגישות",
                        )
                    }
                }
            }
            item {
                SettingCard(
                    "זמן חלון לחיצות",
                    "הזמן המקסימלי בין לחיצה ללחיצה בתוך רצף. כשהחלון נסגר, הרצף נבדק.",
                ) {
                    Text(timeout.toString() + "ms", style = MaterialTheme.typography.titleMedium)
                    Slider(value = timeout.toFloat(), onValueChange = { onTimeout(it.toLong()) }, valueRange = 300f..1500f, steps = 11)
                }
            }
            item {
                SettingCard(
                    "השהיה לפני פעולה",
                    "המתנה לאחר שהרצף כבר זוהה ולפני שהפעולה עצמה מתבצעת.",
                ) {
                    Text(actionDelay.toString() + "ms", style = MaterialTheme.typography.titleMedium)
                    Slider(value = actionDelay.toFloat(), onValueChange = { onActionDelay(it.toLong()) }, valueRange = 0f..5000f, steps = 9)
                }
            }
            item {
                SettingCard(
                    "מונה לחיצות אוטומטיות",
                    "מציג את מספר הלחיצות האוטומטיות מעל אפליקציות אחרות במצב מלא כאשר שירות הנגישות פעיל.",
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("הצג מונה", Modifier.weight(1f))
                        Switch(checked = showTapCount, onCheckedChange = onShowTapCount)
                    }
                    Text("מיקום: " + tapCountPosition.toString() + "% מתחתית המסך", style = MaterialTheme.typography.bodySmall)
                    Slider(value = tapCountPosition.toFloat(), onValueChange = { onTapCountPosition(it.toInt()) }, valueRange = 5f..90f, steps = 84)
                }
            }
            item {
                SettingCard("מראה", "הגדרות בהיר, כהה, מערכת ודינמי.") {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ThemeChip("מערכת", "system", themeMode, onTheme, Icons.Outlined.BrightnessAuto)
                        ThemeChip("בהיר", "light", themeMode, onTheme, Icons.Outlined.LightMode)
                        ThemeChip("כהה", "dark", themeMode, onTheme, Icons.Outlined.DarkMode)
                        if (Build.VERSION.SDK_INT >= 31) ThemeChip("דינמי", "dynamic", themeMode, onTheme, Icons.Outlined.BrightnessAuto)
                    }
                }
            }
            item {
                SettingCard("הרשאות וגישה", "ההרשאות הנוספות אינן נדרשות כדי להיכנס לאפליקציה.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:" + context.packageName)
                                    }
                                )
                            },
                            Modifier.fillMaxWidth(),
                        ) { Text("פתיחת פרטי האפליקציה ב-Android") }
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            Modifier.fillMaxWidth(),
                        ) { Text("הגדרות שירות נגישות") }
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                            Modifier.fillMaxWidth(),
                        ) { Text("גישת נתוני שימוש") }
                        Text(
                            "Android לא מאפשר לאפליקציה להוסיף כפתור מותאם אישית נוסף בתוך דף 'פרטי האפליקציה'. לכן הכפתור הראשון פותח ישירות את אותו דף.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                SettingCard("גיבוי והעברה", "שמירה, שחזור ושיתוף של הפעולות וההגדרות.") {
                    Button(onClick = onBackup, Modifier.fillMaxWidth()) { Text("פתח גיבוי") }
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    value: String,
    selectedValue: String,
    onTheme: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    FilterChip(
        selected = selectedValue == value,
        onClick = { onTheme(value) },
        modifier = Modifier.height(40.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, null, Modifier.size(17.dp)) },
    )
}

@Composable
private fun SettingCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
    actionDelay: Long,
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
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val root = JSONObject(
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("קובץ ריק")
                )
                val array = root.optJSONArray("mappings") ?: JSONArray()
                val imported = buildList {
                    for (i in 0 until array.length()) {
                        array.optJSONObject(i)?.let { add(KeyActionConfig.fromJson(it)) }
                    }
                }
                basePrefs.saveMappings(imported)
                root.optJSONObject("settings")?.let {
                    if (it.has("tapTimeoutMs")) basePrefs.saveTapTimeout(it.optLong("tapTimeoutMs", 1200L))
                    if (it.has("actionDelayMs")) basePrefs.saveActionDelay(it.optLong("actionDelayMs", 0L))
                    if (it.has("showTapCount")) basePrefs.saveShowTapCount(it.optBoolean("showTapCount", false))
                    if (it.has("tapCountPosition")) basePrefs.saveTapCountPosition(it.optInt("tapCountPosition", 35))
                }
                advanced.importBundle(root)
                message = "הגיבוי יובא בהצלחה."
            }.onFailure { message = "הייבוא נכשל: " + (it.message ?: "שגיאה") }
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = advanced.exportJson(
            JSONArray().apply { mappings.forEach { put(it.toJson()) } }.toString(),
            JSONObject()
                .put("tapTimeoutMs", timeout)
                .put("actionDelayMs", actionDelay)
                .put("showTapCount", showTapCount)
                .put("tapCountPosition", AppPreferencesRepository.tapCountPositionSnapshot(context)),
        )
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            message = "הגיבוי נשמר."
        }.onFailure { message = "הייצוא נכשל: " + (it.message ?: "שגיאה") }
    }

    Scaffold(topBar = { SimpleTopBar("גיבוי והעברה", onBack, "כאן אפשר לייצא, לייבא ולאפס את הפעולות וההגדרות.") }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("ייצוא ושיתוף", "שמור את הפעולות, הפרופילים וההגדרות.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("clickplus-backup.json") }, Modifier.weight(1f)) { Text("ייצוא") }
                        OutlinedButton(
                            onClick = {
                                val file = java.io.File(context.cacheDir, "clickplus-backup.json")
                                file.writeText(
                                    advanced.exportJson(
                                        JSONArray().apply { mappings.forEach { put(it.toJson()) } }.toString(),
                                        JSONObject()
                                            .put("tapTimeoutMs", timeout)
                                            .put("actionDelayMs", actionDelay)
                                            .put("showTapCount", showTapCount)
                                            .put("tapCountPosition", AppPreferencesRepository.tapCountPositionSnapshot(context)),
                                    )
                                )
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        },
                                        "שיתוף גיבוי",
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            Modifier.weight(1f),
                        ) { Text("שיתוף") }
                    }
                }
            }
            item {
                SettingCard("ייבוא", "החלפת הפעולות הנוכחיות בתוכן מהגיבוי.") {
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, Modifier.fillMaxWidth()) { Text("ייבוא גיבוי") }
                }
            }
            item {
                SettingCard("איפוס", "מחיקת כל הפעולות והמטא־נתונים.") {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                basePrefs.saveMappings(emptyList())
                                advanced.clearAllRuleMetadata()
                                advanced.saveProfiles(listOf(ClickPlusProfile("default", "כללי", true)))
                                message = "הפעולות אופסו."
                            }
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("איפוס פעולות") }
                }
            }
            if (message.isNotBlank()) item {
                Text(message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
    val repo = remember { AdvancedRuleRepository(context) }
    val runtimePrefs = remember { context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE) }
    var draft by remember(existing?.id) {
        mutableStateOf(existing ?: KeyActionConfig(name = "פעולה חדשה"))
    }
    var metadata by remember(existing?.id) {
        mutableStateOf(repo.getRuleMetadata(existing?.id ?: ""))
    }
    var actionTypeChosen by remember(existing?.id) { mutableStateOf(existing != null) }
    var appDialog by remember { mutableStateOf(false) }
    var learning by remember { mutableStateOf(false) }
    var learningStage by remember { mutableIntStateOf(1) }
    var showDelete by remember { mutableStateOf(false) }

    fun beginLearning(packageName: String) {
        if (!isAccessibilityEnabled(context)) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        learning = true
        learningStage = 1
        runtimePrefs.edit()
            .putBoolean("tap_learning", true)
            .putBoolean("tap_learning_multi", draft.actionType == ActionType.MULTI_POINT_TAP)
            .putInt("tap_learning_stage", 1)
            .putString("tap_learning_package", packageName)
            .putBoolean("tap_capture_ready", false)
            .apply()
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(it)
        }
    }

    LaunchedEffect(draft.id) {
        while (true) {
            if (runtimePrefs.getBoolean("tap_capture_ready", false)) {
                val capturedX = runtimePrefs.getFloat("tap_capture_x_ratio", -1f)
                val capturedY = runtimePrefs.getFloat("tap_capture_y_ratio", -1f)
                val stage = runtimePrefs.getInt("tap_capture_stage", 1)
                val pkg = runtimePrefs.getString("tap_capture_package", "").orEmpty()
                val appName = runtimePrefs.getString("tap_capture_app_name", "").orEmpty()
                if (capturedX >= 0f && capturedY >= 0f) {
                    if (stage == 1) {
                        draft = draft.copy(
                            screenTapPackage = pkg,
                            screenTapAppName = appName,
                            screenTapXRatio = capturedX,
                            screenTapYRatio = capturedY,
                        )
                        metadata = metadata.copy(portraitX = capturedX, portraitY = capturedY)
                        if (draft.actionType == ActionType.MULTI_POINT_TAP) {
                            learningStage = 2
                            learning = true
                            runtimePrefs.edit()
                                .putBoolean("tap_capture_ready", false)
                                .putBoolean("tap_learning", true)
                                .putBoolean("tap_learning_multi", true)
                                .putInt("tap_learning_stage", 2)
                                .putString("tap_learning_package", pkg)
                                .apply()
                            continue
                        }
                    } else {
                        draft = draft.copy(
                            screenTapSecondXRatio = capturedX,
                            screenTapSecondYRatio = capturedY,
                        )
                    }
                    runtimePrefs.edit()
                        .putBoolean("tap_capture_ready", false)
                        .remove("tap_capture_stage")
                        .apply()
                    if (stage == 2) {
                        learning = false
                        learningStage = 1
                    }
                }
            }
            delay(180)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runtimePrefs.edit()
                .putBoolean("tap_learning", false)
                .remove("tap_learning_stage")
                .remove("tap_learning_multi")
                .apply()
        }
    }

    val x = draft.screenTapXRatio.takeIf { it >= 0f }?.coerceIn(0f, 1f) ?: 0.5f
    val y = draft.screenTapYRatio.takeIf { it >= 0f }?.coerceIn(0f, 1f) ?: 0.5f
    val x2 = draft.screenTapSecondXRatio.takeIf { it >= 0f }?.coerceIn(0f, 1f) ?: 0.72f
    val y2 = draft.screenTapSecondYRatio.takeIf { it >= 0f }?.coerceIn(0f, 1f) ?: 0.52f

    fun chooseLocationApp(appPackage: String, appName: String) {
        draft = draft.copy(
            screenTapPackage = appPackage,
            screenTapAppName = appName,
            screenTapXRatio = -1f,
            screenTapYRatio = -1f,
            screenTapSecondXRatio = -1f,
            screenTapSecondYRatio = -1f,
        )
        beginLearning(appPackage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "הוספת פעולה" else "עריכת פעולה") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "חזרה") } },
                actions = {
                    HelpIconButton(
                        "הסבר על הוספת פעולה",
                        "בחר תחילה את סוג הטריגר ואת מספר הלחיצות. לאחר מכן בחר את סוג הפעולה. רק אז יופיעו האפשרויות הבאות. בלימוד מיקום, בחירת האפליקציה פותחת אותה מיד ומתחילה לימוד.",
                    )
                    if (existing != null) IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, "מחיקה") }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        if (!actionTypeChosen) return@Button
                        if (draft.name.isBlank()) draft = draft.copy(name = draft.triggerType.titleHebrew)
                        onSave(
                            draft.copy(
                                screenTapToleranceRatio = maxOf(metadata.toleranceXRatio, metadata.toleranceYRatio),
                            ),
                            metadata,
                        )
                    },
                    Modifier.fillMaxWidth().padding(10.dp).height(50.dp),
                ) {
                    Icon(Icons.Outlined.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("שמירת פעולה", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                EditorSectionCard("1", "פרטי הפעולה", "שם הפעולה שיופיע ברשימה וביומן.") {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("שם הפעולה") },
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("הפעולה מופעלת", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(checked = draft.enabled, onCheckedChange = { draft = draft.copy(enabled = it) })
                    }
                }
            }
            item {
                EditorSectionCard("2", "מתי להפעיל?", "בחר את סוג הטריגר ואת מספר הלחיצות הרצופות.") {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        TriggerType.entries.forEach { trigger ->
                            ChoiceChip(
                                selected = draft.triggerType == trigger,
                                onClick = {
                                    draft = draft.copy(triggerType = trigger)
                                    learning = false
                                    runtimePrefs.edit().putBoolean("tap_learning", false).apply()
                                },
                                label = trigger.titleHebrew,
                                modifier = Modifier.widthIn(min = 105.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("מספר הלחיצות הרצופות", fontWeight = FontWeight.Medium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (1..10).forEach { count ->
                            ChoiceChip(
                                selected = draft.pressCount == count,
                                onClick = { draft = draft.copy(pressCount = count) },
                                label = count.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (draft.triggerType == TriggerType.APP_TAP) {
                        Text(
                            "פועל רק כשהאפליקציה שנבחרה פתוחה בחזית. אינו פותח אותה ואינו מבטל את פעולותיה הרגילות.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                EditorSectionCard("3", "באיזה מצב?", "הבחירה נבדקת לפי המצב בזמן זיהוי הטריגר.") {
                    if (draft.triggerType == TriggerType.APP_ENTRY) {
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            maxItemsInEachRow = 2,
                        ) {
                            ContextConditionType.entries.forEach { condition ->
                                ChoiceChip(
                                    selected = draft.contextConditionType == condition,
                                    onClick = {
                                        draft = draft.copy(
                                            contextConditionType = condition,
                                            contextConditionValue = if (
                                                condition == ContextConditionType.VOLUME_LEVEL &&
                                                draft.contextConditionValue.isBlank()
                                            ) "15" else draft.contextConditionValue,
                                        )
                                    },
                                    label = condition.titleHebrew,
                                    modifier = Modifier.widthIn(min = 135.dp),
                                )
                            }
                        }
                        if (draft.contextConditionType == ContextConditionType.APP ||
                            draft.contextConditionType == ContextConditionType.RADIO
                        ) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { appDialog = true }, Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Apps, null)
                                Spacer(Modifier.width(8.dp))
                                Text(draft.contextConditionName.ifBlank { "בחירת אפליקציה" })
                            }
                        }
                        if (draft.contextConditionType == ContextConditionType.VOLUME_LEVEL) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "עוצמת שמע: " + draft.contextConditionValue.ifBlank { "15" } + " מתוך 30",
                                fontWeight = FontWeight.Medium,
                            )
                            Slider(
                                value = draft.contextConditionValue.toFloatOrNull()?.coerceIn(1f, 30f) ?: 15f,
                                onValueChange = { draft = draft.copy(contextConditionValue = it.toInt().coerceIn(1, 30).toString()) },
                                valueRange = 1f..30f,
                                steps = 28,
                            )
                        }
                    } else {
                        if (draft.screenTapPackage.isBlank()) {
                            OutlinedButton(onClick = { appDialog = true }, Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Apps, null)
                                Spacer(Modifier.width(8.dp))
                                Text("בחירת אפליקציה → לימוד מיקום")
                            }
                            Text("הבחירה תעביר מיד לאפליקציה ותפעיל לימוד.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(draft.screenTapAppName.ifBlank { "האפליקציה שנבחרה" }, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (draft.screenTapXRatio >= 0f)
                                            "נקודה ראשונה: X " + (x * 100).toInt() + "% · Y " + (y * 100).toInt() + "%"
                                        else "הנקודה הראשונה עדיין לא נלמדה",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                ChoiceChip(
                                    selected = false,
                                    onClick = { beginLearning(draft.screenTapPackage) },
                                    label = "לימוד מחדש",
                                    modifier = Modifier.widthIn(min = 100.dp),
                                )
                            }
                            PointEditor(x, y, metadata.toleranceXRatio, metadata.toleranceYRatio) { nx, ny ->
                                draft = draft.copy(screenTapXRatio = nx, screenTapYRatio = ny)
                                metadata = metadata.copy(portraitX = nx, portraitY = ny)
                            }
                            if (draft.actionType == ActionType.MULTI_POINT_TAP) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (draft.screenTapSecondXRatio >= 0f)
                                        "נקודה שנייה: X " + (x2 * 100).toInt() + "% · Y " + (y2 * 100).toInt() + "%"
                                    else "הנקודה השנייה עדיין לא נלמדה",
                                    fontWeight = FontWeight.Medium,
                                )
                                if (draft.screenTapSecondXRatio >= 0f) {
                                    PointEditor(x2, y2, metadata.toleranceXRatio, metadata.toleranceYRatio) { nx, ny ->
                                        draft = draft.copy(screenTapSecondXRatio = nx, screenTapSecondYRatio = ny)
                                    }
                                }
                                Text("השהיה בין שתי הלחיצות: " + (draft.screenTapIntervalMs / 1000f) + " שניות", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = draft.screenTapIntervalMs.toFloat(),
                                    onValueChange = { draft = draft.copy(screenTapIntervalMs = it.toLong().coerceIn(500L, 10_000L)) },
                                    valueRange = 500f..10_000f,
                                    steps = 19,
                                )
                            }
                            if (learning) {
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        if (learningStage == 1) "מצב לימוד פעיל: גע בנקודה הראשונה."
                                        else "מצב לימוד פעיל: גע בנקודה השנייה.",
                                        Modifier.padding(12.dp),
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                EditorSectionCard("4", "מה לבצע?", "בחר קודם סוג פעולה; האפשרויות הבאות ייפתחו רק לאחר הבחירה.") {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.SYSTEM,
                            onClick = { actionTypeChosen = true; draft = draft.copy(actionType = ActionType.SYSTEM) },
                            label = "פעולת מכשיר",
                            modifier = Modifier.widthIn(min = 135.dp),
                        )
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.APP,
                            onClick = { actionTypeChosen = true; draft = draft.copy(actionType = ActionType.APP, systemActionId = "") },
                            label = "פתיחת אפליקציה",
                            modifier = Modifier.widthIn(min = 135.dp),
                        )
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.APP_TAP,
                            onClick = { actionTypeChosen = true; draft = draft.copy(actionType = ActionType.APP_TAP, systemActionId = "") },
                            label = "פתיחה + לחיצה",
                            modifier = Modifier.widthIn(min = 135.dp),
                        )
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.MULTI_POINT_TAP,
                            onClick = {
                                actionTypeChosen = true
                                draft = draft.copy(actionType = ActionType.MULTI_POINT_TAP, systemActionId = "")
                            },
                            label = "שתי לחיצות אוטומטיות",
                            modifier = Modifier.widthIn(min = 180.dp),
                        )
                    }
                    if (!actionTypeChosen) {
                        Spacer(Modifier.height(10.dp))
                        Text("בחר סוג פעולה כדי לפתוח את ההגדרה הבאה.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Spacer(Modifier.height(12.dp))
                        when (draft.actionType) {
                            ActionType.SYSTEM -> {
                                val grouped = SystemActionPreset.entries.groupBy { it.categoryHebrew }
                                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                                    grouped.forEach { (category, actions) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(category, fontWeight = FontWeight.Bold)
                                            FlowRow(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                                maxItemsInEachRow = 3,
                                            ) {
                                                actions.forEach { action ->
                                                    ChoiceChip(
                                                        selected = draft.systemActionId == action.id,
                                                        onClick = { draft = draft.copy(systemActionId = action.id) },
                                                        label = action.titleHebrew,
                                                        modifier = Modifier.widthIn(min = 103.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (draft.systemActionId == SystemActionPreset.DIAL_NUMBER.id) {
                                    OutlinedTextField(
                                        value = draft.actionParameter,
                                        onValueChange = { draft = draft.copy(actionParameter = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '-' || ch == ' ' }) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = { Text("מספר לחיוג") },
                                    )
                                }
                                if (draft.systemActionId == SystemActionPreset.BRIGHTNESS_SET.id) {
                                    OutlinedTextField(
                                        value = draft.actionParameter,
                                        onValueChange = { draft = draft.copy(actionParameter = it.filter(Char::isDigit).take(3)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = { Text("בהירות באחוזים · 1–100") },
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                    Uri.parse("package:" + context.packageName),
                                                )
                                            )
                                        },
                                        Modifier.fillMaxWidth(),
                                    ) { Text("אישור שינוי בהירות ב-Android") }
                                }
                            }
                            ActionType.APP -> {
                                OutlinedButton(onClick = { appDialog = true }, Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.Apps, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(draft.targetAppName.ifBlank { "בחירת אפליקציית יעד" })
                                }
                            }
                            ActionType.APP_TAP,
                            ActionType.MULTI_POINT_TAP -> {
                                OutlinedButton(onClick = { appDialog = true }, Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.LocationOn, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (draft.screenTapPackage.isBlank()) "בחירת אפליקציה ולימוד מיקום"
                                        else "לימוד מחדש של מקום הלחיצה",
                                    )
                                }
                                if (draft.screenTapPackage.isNotBlank() && draft.triggerType == TriggerType.APP_ENTRY) {
                                    PointEditor(x, y, metadata.toleranceXRatio, metadata.toleranceYRatio) { nx, ny ->
                                        draft = draft.copy(screenTapXRatio = nx, screenTapYRatio = ny)
                                    }
                                    if (draft.actionType == ActionType.MULTI_POINT_TAP) {
                                        Text(
                                            if (draft.screenTapSecondXRatio >= 0f)
                                                "נקודה שנייה: X " + (x2 * 100).toInt() + "% · Y " + (y2 * 100).toInt() + "%"
                                            else "הנקודה השנייה עדיין לא נלמדה",
                                            fontWeight = FontWeight.Medium,
                                        )
                                        if (draft.screenTapSecondXRatio >= 0f) {
                                            PointEditor(x2, y2, metadata.toleranceXRatio, metadata.toleranceYRatio) { nx, ny ->
                                                draft = draft.copy(screenTapSecondXRatio = nx, screenTapSecondYRatio = ny)
                                            }
                                        }
                                        Text("השהיה בין שתי הלחיצות: " + (draft.screenTapIntervalMs / 1000f) + " שניות", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = draft.screenTapIntervalMs.toFloat(),
                                            onValueChange = { draft = draft.copy(screenTapIntervalMs = it.toLong().coerceIn(500L, 10_000L)) },
                                            valueRange = 500f..10_000f,
                                            steps = 19,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                EditorSectionCard("5", "הגדרות מתקדמות", "הגנה, ניסיונות ואזור ההתאמה.") {
                    EditorSliderRow("זמן חסימה בין הפעלות", metadata.cooldownMs.toString() + "ms") {
                        Slider(
                            value = metadata.cooldownMs.coerceIn(0L, 10_000L).toFloat(),
                            onValueChange = { metadata = metadata.copy(cooldownMs = it.toLong().coerceIn(0L, 60_000L)) },
                            valueRange = 0f..10_000f,
                            steps = 9,
                        )
                    }
                    EditorSliderRow("מספר ניסיונות", metadata.retries.toString()) {
                        Slider(
                            value = metadata.retries.coerceIn(1, 3).toFloat(),
                            onValueChange = { metadata = metadata.copy(retries = it.toInt().coerceIn(1, 3)) },
                            valueRange = 1f..3f,
                            steps = 1,
                        )
                    }
                    if (draft.triggerType == TriggerType.SCREEN_TAP || draft.triggerType == TriggerType.APP_TAP) {
                        EditorSliderRow("רוחב אזור התאמה", (metadata.toleranceXRatio * 100f).toInt().toString() + "%") {
                            Slider(
                                value = metadata.toleranceXRatio,
                                onValueChange = { metadata = metadata.copy(toleranceXRatio = it.coerceIn(0.01f, 0.25f)) },
                                valueRange = 0.01f..0.25f,
                            )
                        }
                        EditorSliderRow("גובה אזור התאמה", (metadata.toleranceYRatio * 100f).toInt().toString() + "%") {
                            Slider(
                                value = metadata.toleranceYRatio,
                                onValueChange = { metadata = metadata.copy(toleranceYRatio = it.coerceIn(0.01f, 0.25f)) },
                                valueRange = 0.01f..0.25f,
                            )
                        }
                    }
                    ProfileSelector(repo.profiles(), metadata.profileId) { metadata = metadata.copy(profileId = it) }
                    OutlinedButton(
                        onClick = {
                            val performer = when (AdvancedRuleRepository.currentMode(context)) {
                                AppMode.BASIC -> BasicActionPerformer(context)
                                AppMode.FULL -> KeyInterceptorAccessibilityService.instance?.let { ActionExecutor(it) }
                                    ?: UnavailableActionPerformer("מצב מלא נבחר, אבל שירות הנגישות אינו פעיל.")
                            }
                            RuleExecutionCoordinator(context, performer).execute(draft, test = true, reason = "בדיקה ידנית")
                        },
                        Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("בדיקת הפעולה")
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
                appDialog = false
                when {
                    draft.actionType == ActionType.APP && draft.triggerType == TriggerType.APP_ENTRY -> {
                        draft = draft.copy(targetPackage = app.packageName, targetAppName = app.label)
                    }
                    draft.triggerType == TriggerType.APP_ENTRY &&
                        (draft.contextConditionType == ContextConditionType.APP || draft.contextConditionType == ContextConditionType.RADIO) -> {
                        draft = draft.copy(contextConditionValue = app.packageName, contextConditionName = app.label)
                    }
                    else -> chooseLocationApp(app.packageName, app.label)
                }
            },
        )
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("למחוק את הפעולה?") },
            text = { Text("הכלל וההגדרות שלו יוסרו.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete(existing) }) { Text("מחיקה") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("ביטול") } },
        )
    }
}

@Composable
private fun EditorSectionCard(
    number: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(number, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            androidx.compose.material3.HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun EditorSliderRow(title: String, valueText: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@Composable
private fun AppPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (InstalledApp) -> Unit,
) {
    val context = LocalContext.current
    val apps = remember {
        runCatching {
            context.packageManager.getInstalledApplications(0)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .mapNotNull { info ->
                    runCatching {
                        context.packageManager.getLaunchIntentForPackage(info.packageName) ?: return@runCatching null
                        InstalledApp(
                            packageName = info.packageName,
                            label = context.packageManager.getApplicationLabel(info).toString(),
                            icon = runCatching { info.loadIcon(context.packageManager) }.getOrNull(),
                        )
                    }.getOrNull()
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.ROOT) }
                .toList()
        }.getOrDefault(emptyList())
    }
    var query by remember { mutableStateOf("") }
    val filtered = apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Modifier.fillMaxWidth().height(450.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(app) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(Modifier.size(42.dp), CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            val image = remember(app.packageName) {
                                app.icon?.let { runCatching { it.toBitmap(42, 42).asImageBitmap() }.getOrNull() }
                            }
                            if (image != null) {
                                Image(image, app.label, Modifier.fillMaxSize().padding(7.dp))
                            } else {
                                Icon(Icons.Outlined.Apps, null, Modifier.padding(9.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Bold)
                            Text(app.packageName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSelector(
    profiles: List<ClickPlusProfile>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("פרופיל", fontWeight = FontWeight.Medium)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            profiles.forEach { profile ->
                ChoiceChip(
                    selected = selectedId == profile.id,
                    onClick = { onSelect(profile.id) },
                    label = profile.name,
                    modifier = Modifier.widthIn(min = 90.dp),
                )
            }
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
    val safeX = x.coerceIn(0f, 1f)
    val safeY = y.coerceIn(0f, 1f)
    val safeTx = toleranceX.coerceIn(0.01f, 0.25f)
    val safeTy = toleranceY.coerceIn(0.01f, 0.25f)
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("תצוגה מקדימה · גרור את הנקודה למיקום המדויק", style = MaterialTheme.typography.bodySmall)
        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
        ) {
            androidx.compose.foundation.Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .pointerInput(safeX, safeY) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val px = safeX * size.width
                            val py = safeY * size.height
                            val dx = down.position.x - px
                            val dy = down.position.y - py
                            if (dx * dx + dy * dy > 40.dp.toPx() * 40.dp.toPx()) return@awaitEachGesture
                            down.consume()
                            drag(down.id) { change ->
                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                onChange(nx, ny)
                                change.consume()
                            }
                        }
                    }
            ) {
                val px = safeX * size.width
                val py = safeY * size.height
                drawRect(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))),
                )
                drawRect(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    topLeft = Offset(
                        ((safeX - safeTx).coerceAtLeast(0f)) * size.width,
                        ((safeY - safeTy).coerceAtLeast(0f)) * size.height,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        (safeTx * 2f).coerceAtMost(1f) * size.width,
                        (safeTy * 2f).coerceAtMost(1f) * size.height,
                    ),
                )
                drawCircle(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), radius = 30f, center = Offset(px, py))
                drawCircle(MaterialTheme.colorScheme.primary, radius = 11f, center = Offset(px, py))
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(40.dp),
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) },
    )
}

@Composable
private fun SimpleTopBar(
    title: String,
    onBack: () -> Unit,
    helpText: String = "כאן נמצא הסבר קצר על אפשרויות המסך.",
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "חזרה") } },
        actions = { HelpIconButton("הסבר על " + title, helpText) },
    )
}

@Composable
private fun HelpIconButton(title: String, text: String) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Outlined.Info, "הסבר") }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("הבנתי") } },
        )
    }
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
