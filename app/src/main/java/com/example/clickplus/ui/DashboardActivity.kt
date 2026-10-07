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
import android.provider.ContactsContract
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
import androidx.compose.ui.unit.IntOffset
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
    val tapCountX by prefs.tapCountXFlow.collectAsState(initial = 50)
    val tapCountY by prefs.tapCountYFlow.collectAsState(initial = 65)
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
                    onToggleEnabled = { item, enabled ->
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            prefs.saveMappings(mappings.map { if (it.id == item.id) it.copy(enabled = enabled) else it })
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
                    tapCountX = tapCountX,
                    tapCountY = tapCountY,
                    currentMode = mode,
                    themeMode = themeMode,
                    onBack = { route = DashboardRoute.Home },
                    onTimeout = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapTimeout(v) } },
                    onActionDelay = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveActionDelay(v) } },
                    onShowTapCount = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveShowTapCount(v) } },
                    onTapCountX = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapCountX(v) } },
                    onTapCountY = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapCountY(v) } },
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
    onToggleEnabled: (KeyActionConfig, Boolean) -> Unit,
    onStatus: () -> Unit,
    onLogs: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val mode = AdvancedRuleRepository.currentMode(context)
    val accessibility = isAccessibilityEnabled(context)
    val activeProfile = AdvancedRuleRepository.activeProfileId(context)
    val repo = remember { AdvancedRuleRepository(context) }
    val activeProfileName = remember(activeProfile) {
        repo.profiles().firstOrNull { it.id == activeProfile }?.name ?: "כללי"
    }
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
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            HelpIconButton(
                                "הסבר על המסך הראשי",
                                "כאן נמצאות הפעולות שהגדרת. אפשר לערוך פעולה, למחוק אותה או להפעיל ולהשבית אותה ישירות מהרשימה."
                            )
                            IconButton(onClick = onStatus) {
                                Icon(Icons.Outlined.Tune, "סטטוס השירות")
                            }
                            IconButton(onClick = onSettings) {
                                Icon(Icons.Outlined.Settings, "הגדרות")
                            }
                        }
                    }
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            Modifier.size(48.dp),
                            CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(11.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("קליק פלוס פעיל", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (mode == AppMode.FULL && accessibility) {
                                    "מצב מלא · זיהוי כניסות לאפליקציות פעיל"
                                } else if (mode == AppMode.FULL) {
                                    "מצב מלא · שירות הנגישות אינו פעיל"
                                } else {
                                    "מצב בסיסי · ללא שירות נגישות"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text("פרופיל פעיל: " + activeProfileName, style = MaterialTheme.typography.bodySmall)
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
                    val profileName = repo.profiles().firstOrNull {
                        it.id == repo.getRuleMetadata(item.id).profileId
                    }?.name ?: "כללי"
                    RuleCard(
                        item = item,
                        profileName = profileName,
                        onEdit = { onEdit(item.id) },
                        onDelete = { deleteTarget = item },
                        onEnabledChange = { onToggleEnabled(item, it) },
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
    profileName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name.ifBlank { "פעולה" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.pressSummary() + " · " + item.actionSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = item.enabled,
                    onCheckedChange = onEnabledChange,
                )
                IconButton(onClick = onEdit, Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Edit, "עריכה")
                }
                IconButton(onClick = onDelete, Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Delete, "מחיקה")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistSummaryChip("פרופיל: " + profileName)
                AssistSummaryChip(if (item.enabled) "מופעלת" else "מושבתת")
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
            contentPadding = PaddingValues(20.dp, 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    Modifier.size(72.dp),
                    CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
            item {
                Text(
                    "הגדרה ראשונית",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    "אפשר להיכנס ולהגדיר את האפליקציה עכשיו. הרשאה נפתחת רק כאשר תכונה מסוימת באמת זקוקה לה.",
                    textAlign = TextAlign.Center,
                )
            }
            item {
                SettingCard(
                    "הרשאות ומה הן מאפשרות",
                    "אין צורך לאשר את כולן מראש.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("שירות נגישות – זיהוי כניסות לאפליקציות וביצוע פעולות מערכת.")
                        Text("התראות – הצגת הודעת השירות כאשר נדרשת.")
                        Text("טלפון – רק לפעולות הקשורות לשיחות.")
                        Text("נתוני שימוש – רק אם משתמשים במצב בסיסי שזקוק לזיהוי אפליקציה פעילה.")
                    }
                }
            }
            item {
                HelpIconButton(
                    "למה יש הרשאות?",
                    "ההרשאות אינן מטרה בפני עצמן. כל אחת מהן משמשת רק לתכונה שמצריכה אותה, והאפליקציה נשארת זמינה גם בלי להעניק את כולן מיד."
                )
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
    val activeProfileId = AdvancedRuleRepository.activeProfileId(context)
    val repo = remember { AdvancedRuleRepository(context) }
    val activeProfile = repo.profiles().firstOrNull { it.id == activeProfileId }
    val activeRules = mappings.count {
        it.enabled && repo.getRuleMetadata(it.id).profileId == activeProfileId
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                "סטטוס השירות",
                onBack,
                "כאן מוצגים מצב העבודה, הפרופיל הפעיל וההרשאות. את מצב העבודה משנים בהגדרות.",
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingCard(
                    "מצב עבודה",
                    if (mode == AppMode.FULL) {
                        "מצב מלא משתמש בשירות נגישות כדי לזהות כניסות לאפליקציות ולבצע יכולות מתקדמות."
                    } else {
                        "מצב בסיסי אינו משתמש בשירות נגישות ולכן זמין רק ליכולות המוגבלות יותר של האפליקציה."
                    },
                ) {
                    Text(
                        if (mode == AppMode.FULL) "מצב מלא" else "מצב בסיסי ללא נגישות",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (mode == AppMode.FULL) {
                            if (service) "זיהוי כניסות לאפליקציות: פעיל" else "זיהוי כניסות לאפליקציות: ממתין להפעלת שירות הנגישות"
                        } else {
                            "זיהוי כניסות לאפליקציות באמצעות נגישות: לא פעיל במצב זה"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item { StatusCard("שירות נגישות", service, if (service) "פעיל" else "כבוי") }
            item { StatusCard("התראות", notification, if (notification) "ההרשאה פעילה" else "ההרשאה אינה פעילה") }
            item {
                StatusCard(
                    "שימוש בנתוני שימוש",
                    usage,
                    if (usage) "הגישה פעילה" else "הגישה אינה פעילה · נדרשת רק בתרחישים מסוימים במצב בסיסי",
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
                SettingCard(
                    "פרופיל ClickPlus פעיל",
                    "אלה פרופילים פנימיים של קליק פלוס, ולא פרופילי משתמש של Android.",
                ) {
                    Text(
                        activeProfile?.name ?: "כללי",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("פעולות מופעלות בפרופיל: " + activeRules)
                }
            }
            item {
                SettingCard("סיכום", "נתונים שימושיים על ההפעלה הנוכחית.") {
                    Text("סה״כ פעולות: " + mappings.size)
                    Text("פעולות מופעלות: " + mappings.count { it.enabled })
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
    var activeId by remember { mutableStateOf(AdvancedRuleRepository.activeProfileId(context)) }
    var newProfileDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    fun saveProfileList(next: List<ClickPlusProfile>) {
        profiles = next
        repo.saveProfiles(next)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("פרופילים של קליק פלוס") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowForward, "חזרה")
                    }
                },
                actions = {
                    HelpIconButton(
                        "הסבר על פרופילים",
                        "פרופילים הם קבוצות פנימיות של פעולות בתוך קליק פלוס. רק פעולות ששייכות לפרופיל הפעיל יכולות להופעל אוטומטית."
                    )
                    IconButton(onClick = { newName = ""; newProfileDialog = true }) {
                        Icon(Icons.Outlined.Add, "פרופיל חדש")
                    }
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
                SettingCard(
                    "הפרופיל הפעיל",
                    "בחירה של פרופיל בתוך ClickPlus. שינוי כאן אינו משנה שום פרופיל במכשיר.",
                ) {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        profiles.forEach { profile ->
                            ChoiceChip(
                                selected = activeId == profile.id,
                                onClick = {
                                    activeId = profile.id
                                    AdvancedRuleRepository.setActiveProfileId(context, profile.id)
                                },
                                label = profile.name,
                                modifier = Modifier.widthIn(min = 110.dp),
                            )
                        }
                    }
                }
            }
            items(profiles, key = { it.id }) { profile ->
                val count = mappings.count { repo.getRuleMetadata(it.id).profileId == profile.id }
                OutlinedCard(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.name, fontWeight = FontWeight.Bold)
                            Text("$count פעולות משויכות", style = MaterialTheme.typography.bodySmall)
                            if (profile.id == activeId) {
                                Text("פעיל עכשיו", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Switch(
                            checked = profile.enabled,
                            onCheckedChange = {
                                saveProfileList(profiles.map { p ->
                                    if (p.id == profile.id) p.copy(enabled = it) else p
                                })
                            },
                        )
                        if (profile.id != "default") {
                            IconButton(onClick = {
                                val wasActive = activeId == profile.id
                                val next = profiles.filterNot { it.id == profile.id }
                                saveProfileList(next)
                                if (wasActive) {
                                    activeId = "default"
                                    AdvancedRuleRepository.setActiveProfileId(context, "default")
                                }
                            }) {
                                Icon(Icons.Outlined.Delete, "מחיקה")
                            }
                        }
                    }
                }
            }
        }
    }

    if (newProfileDialog) {
        AlertDialog(
            onDismissRequest = { newProfileDialog = false },
            title = { Text("פרופיל ClickPlus חדש") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("שם הפרופיל") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotBlank()) {
                        val next = profiles + ClickPlusProfile(name = name)
                        saveProfileList(next)
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
    tapCountX: Int,
    tapCountY: Int,
    currentMode: AppMode,
    themeMode: String,
    onBack: () -> Unit,
    onTimeout: (Long) -> Unit,
    onActionDelay: (Long) -> Unit,
    onShowTapCount: (Boolean) -> Unit,
    onTapCountX: (Int) -> Unit,
    onTapCountY: (Int) -> Unit,
    onMode: (AppMode) -> Unit,
    onTheme: (String) -> Unit,
    onBackup: () -> Unit,
) {
    val context = LocalContext.current
    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }

    Scaffold(
        topBar = {
            SimpleTopBar(
                "הגדרות",
                onBack,
                "הגדרות כלליות, מיקום חיווי, מצב העבודה, מראה וגיבוי.",
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingCard(
                    "מצב עבודה",
                    "מצב מלא משתמש בשירות נגישות ומאפשר זיהוי כניסות לאפליקציות ויכולות מתקדמות. מצב בסיסי פועל ללא שירות נגישות ונותן רק את היכולות המתאימות למגבלה זו.",
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceChip(
                            selected = selectedMode == AppMode.FULL,
                            onClick = { selectedMode = AppMode.FULL; onMode(AppMode.FULL) },
                            label = "מצב מלא",
                            modifier = Modifier.weight(1f),
                        )
                        ChoiceChip(
                            selected = selectedMode == AppMode.BASIC,
                            onClick = { selectedMode = AppMode.BASIC; onMode(AppMode.BASIC) },
                            label = "מצב בסיסי",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                SettingCard(
                    "זמן חלון לחיצות",
                    "כמה זמן ממתינים בין אירועי כניסה לפני שמכריעים איזה רצף הופעל.",
                ) {
                    Text(timeout.toString() + "ms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = timeout.toFloat(),
                        onValueChange = { onTimeout(it.toLong()) },
                        valueRange = 300f..1500f,
                        steps = 11,
                    )
                }
            }
            item {
                SettingCard(
                    "השהיה לפני ביצוע",
                    "המתנה קצרה לאחר זיהוי הפעולה ולפני הביצוע בפועל. אפשר להגדיר גם השהיה שונה לכל פעולה בעורך.",
                ) {
                    Text(actionDelay.toString() + "ms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = actionDelay.toFloat(),
                        onValueChange = { onActionDelay(it.toLong()) },
                        valueRange = 0f..5000f,
                        steps = 9,
                    )
                }
            }
            item {
                SettingCard(
                    "חיווי מונה הלחיצות",
                    "כאשר החיווי פעיל, מספר האירועים המזוהים מוצג מעל המסך. המיקום ניתן להגדרה בכל מקום במסך, גם אופקית וגם אנכית.",
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("הצג חיווי", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(checked = showTapCount, onCheckedChange = onShowTapCount)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("מיקום אופקי: " + tapCountX + "% משמאל")
                    Slider(
                        value = tapCountX.toFloat(),
                        onValueChange = { onTapCountX(it.toInt()) },
                        valueRange = 0f..100f,
                    )
                    Text("מיקום אנכי: " + tapCountY + "% מלמעלה")
                    Slider(
                        value = tapCountY.toFloat(),
                        onValueChange = { onTapCountY(it.toInt()) },
                        valueRange = 0f..100f,
                    )
                    PositionPreview(tapCountX, tapCountY)
                }
            }
            item {
                SettingCard("מראה", "בחירת ערכת הצבעים של האפליקציה.") {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
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
                SettingCard(
                    "הרשאות וגישה",
                    "כאן נמצאות רק כניסות להגדרות Android שנדרשות לתכונות המתקדמות. אין כאן קישור מיותר להגדרות שאינן קשורות ישירות לתכונות האפליקציה.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            Modifier.fillMaxWidth(),
                        ) { Text("הגדרות שירות נגישות") }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            Modifier.fillMaxWidth(),
                        ) { Text("גישת נתוני שימוש") }
                        Text(
                            "שירות נגישות נדרש במצב מלא. גישת נתוני שימוש נדרשת רק כאשר בוחרים תכונה שתלויה בזיהוי אפליקציה פעילה במצב בסיסי.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                SettingCard("גיבוי והעברה", "שמירה, שחזור ושיתוף של פעולות, פרופילים והגדרות.") {
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
                    if (it.has("tapCountX")) basePrefs.saveTapCountX(it.optInt("tapCountX", 50))
                    if (it.has("tapCountY")) basePrefs.saveTapCountY(it.optInt("tapCountY", 65))
                    else if (it.has("tapCountPosition")) basePrefs.saveTapCountPosition(it.optInt("tapCountPosition", 35))
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
                                            .put("tapCountX", AppPreferencesRepository.tapCountXSnapshot(context))
                                            .put("tapCountY", AppPreferencesRepository.tapCountYSnapshot(context))
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
    var selectedSystemCategory by remember(existing?.id) {
        mutableStateOf(
            existing?.let {
                SystemActionPreset.entries.firstOrNull { preset -> preset.id == it.systemActionId }?.categoryHebrew
            }
        )
    }
    var appDialog by remember { mutableStateOf(false) }
    var triggerAppDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var learning by remember { mutableStateOf(false) }
    var learningStage by remember { mutableIntStateOf(1) }
    var showDelete by remember { mutableStateOf(false) }

    val contactPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                    val number = if (numberIndex >= 0) cursor.getString(numberIndex).orEmpty() else ""
                    draft = draft.copy(
                        contactName = name,
                        contactNumber = number,
                        actionParameter = number,
                    )
                }
            }
        }
    }

    val contactPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactPicker.launch(
                Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                )
            )
        }
    }

    fun chooseContact() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            contactPicker.launch(
                Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                )
            )
        } else {
            contactPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    fun beginLearning(packageName: String) {
        if (!isAccessibilityEnabled(context)) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        if (packageName.isBlank()) return
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

                // Consume the capture before updating state so stage 1 cannot repeat.
                runtimePrefs.edit().putBoolean("tap_capture_ready", false).apply()

                if (capturedX >= 0f && capturedY >= 0f) {
                    if (stage == 1) {
                        draft = draft.copy(
                            screenTapPackage = pkg,
                            screenTapAppName = appName,
                            screenTapXRatio = capturedX,
                            screenTapYRatio = capturedY,
                        )
                        metadata = metadata.copy(
                            portraitX = capturedX,
                            portraitY = capturedY,
                            landscapeX = capturedX,
                            landscapeY = capturedY,
                        )
                        if (draft.actionType == ActionType.MULTI_POINT_TAP) {
                            learningStage = 2
                            learning = true
                            runtimePrefs.edit()
                                .putBoolean("tap_learning", true)
                                .putBoolean("tap_learning_multi", true)
                                .putInt("tap_learning_stage", 2)
                                .putString("tap_learning_package", pkg)
                                .apply()
                        } else {
                            learning = false
                            learningStage = 1
                        }
                    } else {
                        draft = draft.copy(
                            screenTapSecondXRatio = capturedX,
                            screenTapSecondYRatio = capturedY,
                        )
                        learning = false
                        learningStage = 1
                    }
                }
            }
            delay(120)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runtimePrefs.edit()
                .putBoolean("tap_learning", false)
                .putBoolean("tap_capture_ready", false)
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
        metadata = metadata.copy(
            portraitX = -1f,
            portraitY = -1f,
            landscapeX = -1f,
            landscapeY = -1f,
        )
        beginLearning(appPackage)
    }

    fun saveCurrent() {
        validationMessage = ""
        if (!actionTypeChosen) {
            validationMessage = "יש לבחור סוג פעולה."
            return
        }
        if (draft.triggerPackage.isBlank()) {
            validationMessage = "יש לבחור את האפליקציה שבה תזוהה הכניסה."
            return
        }
        if (draft.name.isBlank()) {
            draft = draft.copy(name = "כניסה ל-" + draft.triggerAppName.ifBlank { "אפליקציה" })
        }
        val safe = draft.copy(
            triggerType = TriggerType.APP_ENTRY,
            screenTapToleranceRatio = maxOf(metadata.toleranceXRatio, metadata.toleranceYRatio),
        )
        onSave(safe, metadata)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "הוספת פעולה" else "עריכת פעולה") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "חזרה") }
                },
                actions = {
                    HelpIconButton(
                        "איך מגדירים פעולה?",
                        "מגדירים מה יזוהה בעת כניסה לאפליקציה, בוחרים את הפעולה שתתבצע, ובמידת הצורך מלמדים נקודת לחיצה בתוך אפליקציית היעד.",
                    )
                    if (existing != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.Delete, "מחיקה")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = ::saveCurrent,
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
                EditorSectionCard("1", "פרטי הפעולה", "שם הפעולה, תיאור קצר ופרטי ההפעלה.") {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("שם הפעולה") },
                    )
                    Text(
                        "הפעלה או השבתה מתבצעות ישירות ברשימת הפעולות במסך הראשי.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                EditorSectionCard(
                    "2",
                    "מתי להפעיל?",
                    "ClickPlus מזהה כניסה לאפליקציה שנבחרה. אין כאן טריגר המבוסס על לחיצות בתוך אפליקציה.",
                ) {
                    OutlinedButton(
                        onClick = { triggerAppDialog = true },
                        Modifier.fillMaxWidth().heightIn(min = 46.dp),
                    ) {
                        Icon(Icons.Outlined.Apps, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            draft.triggerAppName.ifBlank { "בחירת האפליקציה שבה תזוהה הכניסה" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (draft.triggerPackage.isBlank()) {
                        Text(
                            "יש לבחור אפליקציה. רק כניסה לאפליקציה הזאת תפעיל את הפעולה.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            "הזיהוי מתרחש בעת מעבר לאפליקציה הזאת, ולא בעקבות לחיצה רגילה בתוך האפליקציה.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("כמה כניסות רצופות ייחשבו לרצף?", fontWeight = FontWeight.Medium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        (1..10).forEach { count ->
                            ChoiceChip(
                                selected = draft.pressCount == count,
                                onClick = { draft = draft.copy(pressCount = count) },
                                label = count.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                EditorSectionCard("3", "תנאי הפעלה", "התנאי נבדק בזמן זיהוי הכניסה לאפליקציה.") {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
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
                                modifier = Modifier.widthIn(min = 125.dp),
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
                            Text(
                                draft.contextConditionName.ifBlank { "בחירת אפליקציה להשוואה" }
                            )
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
                            onValueChange = {
                                draft = draft.copy(
                                    contextConditionValue = it.toInt().coerceIn(1, 30).toString()
                                )
                            },
                            valueRange = 1f..30f,
                            steps = 28,
                        )
                    }
                }
            }

            item {
                EditorSectionCard(
                    "4",
                    "מה לבצע?",
                    "בחר סוג פעולה. לאחר מכן תיפתח רק הקבוצה הרלוונטית של האפשרויות.",
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.SYSTEM,
                            onClick = {
                                actionTypeChosen = true
                                draft = draft.copy(actionType = ActionType.SYSTEM)
                                if (selectedSystemCategory == null) {
                                    selectedSystemCategory = "ניווט"
                                    draft = draft.copy(systemActionId = SystemActionPreset.HOME.id)
                                }
                            },
                            label = "פעולת מכשיר",
                            modifier = Modifier.weight(1f),
                        )
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.APP,
                            onClick = {
                                actionTypeChosen = true
                                selectedSystemCategory = null
                                draft = draft.copy(actionType = ActionType.APP, systemActionId = "")
                            },
                            label = "פתיחת אפליקציה",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.APP_TAP,
                            onClick = {
                                actionTypeChosen = true
                                selectedSystemCategory = null
                                draft = draft.copy(actionType = ActionType.APP_TAP, systemActionId = "")
                            },
                            label = "פתיחה + לחיצה",
                            modifier = Modifier.weight(1f),
                        )
                        ChoiceChip(
                            selected = actionTypeChosen && draft.actionType == ActionType.MULTI_POINT_TAP,
                            onClick = {
                                actionTypeChosen = true
                                selectedSystemCategory = null
                                draft = draft.copy(actionType = ActionType.MULTI_POINT_TAP, systemActionId = "")
                            },
                            label = "שתי לחיצות",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (!actionTypeChosen) {
                        Text(
                            "בחר קודם סוג פעולה. האפשרויות המפורטות יופיעו רק לאחר הבחירה.",
                            Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        when (draft.actionType) {
                            ActionType.SYSTEM -> {
                                Spacer(Modifier.height(6.dp))
                                SystemActionPicker(
                                    selectedId = draft.systemActionId,
                                    selectedCategory = selectedSystemCategory,
                                    onCategorySelected = { selectedSystemCategory = it },
                                    onActionSelected = {
                                        draft = draft.copy(systemActionId = it)
                                    },
                                )
                                when (draft.systemActionId) {
                                    SystemActionPreset.DIAL_NUMBER.id -> {
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = draft.actionParameter,
                                            onValueChange = {
                                                draft = draft.copy(
                                                    actionParameter = it.filter { ch ->
                                                        ch.isDigit() || ch == '+' || ch == '-' || ch == ' '
                                                    }
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            label = { Text("מספר לחיוג") },
                                        )
                                    }
                                    SystemActionPreset.DIAL_CONTACT.id -> {
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = ::chooseContact,
                                            Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(Icons.Outlined.Apps, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                draft.contactName.ifBlank {
                                                    "בחירת איש קשר"
                                                }
                                            )
                                        }
                                        if (draft.contactNumber.isNotBlank()) {
                                            Text(
                                                draft.contactNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                    SystemActionPreset.BRIGHTNESS_SET.id -> {
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = draft.actionParameter,
                                            onValueChange = {
                                                draft = draft.copy(
                                                    actionParameter = it.filter(Char::isDigit).take(3)
                                                )
                                            },
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
                                        ) {
                                            Text("אישור שינוי בהירות ב-Android")
                                        }
                                    }
                                    SystemActionPreset.PROFILE_NEXT.id,
                                    SystemActionPreset.PROFILE_PREVIOUS.id,
                                    SystemActionPreset.PROFILE_DEFAULT.id -> {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "הפעולה תשנה את הפרופיל הפעיל בתוך קליק פלוס.",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                            ActionType.APP -> {
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { appDialog = true },
                                    Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.Apps, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        draft.targetAppName.ifBlank { "בחירת אפליקציית יעד" }
                                    )
                                }
                            }
                            ActionType.APP_TAP,
                            ActionType.MULTI_POINT_TAP -> {
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { appDialog = true },
                                    Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.LocationOn, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (draft.screenTapPackage.isBlank()) {
                                            "בחירת אפליקציה ולימוד מיקום"
                                        } else {
                                            "לימוד מחדש של מיקום הלחיצה"
                                        }
                                    )
                                }
                                if (draft.screenTapPackage.isNotBlank()) {
                                    Spacer(Modifier.height(7.dp))
                                    OutlinedCard(
                                        Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(draft.screenTapAppName.ifBlank { "אפליקציית היעד" }, fontWeight = FontWeight.Bold)
                                            Text(
                                                "נקודה ראשונה: X " + (x * 100).toInt() + "% · Y " + (y * 100).toInt() + "%",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            PointEditor(
                                                x,
                                                y,
                                                metadata.toleranceXRatio,
                                                metadata.toleranceYRatio,
                                            ) { nx, ny ->
                                                draft = draft.copy(screenTapXRatio = nx, screenTapYRatio = ny)
                                                metadata = if (!metadata.useOrientationSpecificPosition) {
                                                    metadata.copy(
                                                        portraitX = nx,
                                                        portraitY = ny,
                                                        landscapeX = nx,
                                                        landscapeY = ny,
                                                    )
                                                } else {
                                                    val landscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                                    if (landscape) {
                                                        metadata.copy(landscapeX = nx, landscapeY = ny)
                                                    } else {
                                                        metadata.copy(portraitX = nx, portraitY = ny)
                                                    }
                                                }
                                            }
                                            if (draft.actionType == ActionType.MULTI_POINT_TAP) {
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    if (draft.screenTapSecondXRatio >= 0f)
                                                        "נקודה שנייה: X " + (x2 * 100).toInt() + "% · Y " + (y2 * 100).toInt() + "%"
                                                    else
                                                        "הנקודה השנייה עדיין לא נלמדה.",
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                if (draft.screenTapSecondXRatio >= 0f) {
                                                    PointEditor(
                                                        x2,
                                                        y2,
                                                        metadata.toleranceXRatio,
                                                        metadata.toleranceYRatio,
                                                    ) { nx, ny ->
                                                        draft = draft.copy(
                                                            screenTapSecondXRatio = nx,
                                                            screenTapSecondYRatio = ny,
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "השהיה בין שתי הלחיצות: " +
                                                        String.format(Locale.getDefault(), "%.1f", draft.screenTapIntervalMs / 1000f) +
                                                        " שניות",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Slider(
                                                    value = draft.screenTapIntervalMs.toFloat(),
                                                    onValueChange = {
                                                        draft = draft.copy(
                                                            screenTapIntervalMs = it.toLong().coerceIn(500L, 10_000L)
                                                        )
                                                    },
                                                    valueRange = 500f..10_000f,
                                                    steps = 19,
                                                )
                                                Text(
                                                    "טווח אפשרי: חצי שנייה עד 10 שניות.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                            if (learning) {
                                                Surface(
                                                    Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                ) {
                                                    Text(
                                                        if (learningStage == 1) {
                                                            "מצב לימוד פעיל: הקש בנקודה הראשונה באפליקציה שנפתחה."
                                                        } else {
                                                            "מצב לימוד פעיל: הנקודה הראשונה נשמרה. עכשיו הקש בנקודה השנייה."
                                                        },
                                                        Modifier.padding(11.dp),
                                                        fontWeight = FontWeight.Medium,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (validationMessage.isNotBlank()) {
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            validationMessage,
                            Modifier.padding(11.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            item {
                EditorSectionCard(
                    "5",
                    "הגדרות מתקדמות",
                    "דיוק, ניסיונות, תזמון, פרופיל והתאמה לכיוון המסך.",
                ) {
                    EditorSliderRow(
                        "זמן חסימה בין הפעלות",
                        (metadata.cooldownMs / 1000f).let {
                            String.format(Locale.getDefault(), "%.1f שניות", it)
                        },
                    ) {
                        Slider(
                            value = metadata.cooldownMs.coerceIn(0L, 60_000L).toFloat(),
                            onValueChange = {
                                metadata = metadata.copy(
                                    cooldownMs = it.toLong().coerceIn(0L, 60_000L)
                                )
                            },
                            valueRange = 0f..60_000f,
                            steps = 59,
                        )
                    }
                    EditorSliderRow(
                        "השהיה ייעודית לפעולה",
                        (metadata.delayMs / 1000f).let {
                            String.format(Locale.getDefault(), "%.1f שניות", it)
                        },
                    ) {
                        Slider(
                            value = metadata.delayMs.coerceIn(0L, 10_000L).toFloat(),
                            onValueChange = {
                                metadata = metadata.copy(
                                    delayMs = it.toLong().coerceIn(0L, 10_000L)
                                )
                            },
                            valueRange = 0f..10_000f,
                            steps = 19,
                        )
                    }
                    EditorSliderRow("רמת עדיפות", metadata.priority.toString()) {
                        Slider(
                            value = metadata.priority.coerceIn(0, 10).toFloat(),
                            onValueChange = {
                                metadata = metadata.copy(priority = it.toInt().coerceIn(0, 10))
                            },
                            valueRange = 0f..10f,
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
                    if (draft.actionType == ActionType.APP_TAP || draft.actionType == ActionType.MULTI_POINT_TAP) {
                        EditorSliderRow(
                            "רוחב אזור ההתאמה",
                            (metadata.toleranceXRatio * 100f).toInt().toString() + "%",
                        ) {
                            Slider(
                                value = metadata.toleranceXRatio.coerceIn(0.01f, 0.25f),
                                onValueChange = {
                                    metadata = metadata.copy(toleranceXRatio = it.coerceIn(0.01f, 0.25f))
                                },
                                valueRange = 0.01f..0.25f,
                            )
                        }
                        EditorSliderRow(
                            "גובה אזור ההתאמה",
                            (metadata.toleranceYRatio * 100f).toInt().toString() + "%",
                        ) {
                            Slider(
                                value = metadata.toleranceYRatio.coerceIn(0.01f, 0.25f),
                                onValueChange = {
                                    metadata = metadata.copy(toleranceYRatio = it.coerceIn(0.01f, 0.25f))
                                },
                                valueRange = 0.01f..0.25f,
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("התאמה נפרדת לאורך ולרוחב", fontWeight = FontWeight.Medium)
                                Text(
                                    "מאפשרת נקודה שונה בכל כיוון מסך.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(
                                checked = metadata.useOrientationSpecificPosition,
                                onCheckedChange = { enabled ->
                                    metadata = metadata.copy(
                                        useOrientationSpecificPosition = enabled,
                                        portraitX = if (metadata.portraitX >= 0f) metadata.portraitX else x,
                                        portraitY = if (metadata.portraitY >= 0f) metadata.portraitY else y,
                                        landscapeX = if (metadata.landscapeX >= 0f) metadata.landscapeX else x,
                                        landscapeY = if (metadata.landscapeY >= 0f) metadata.landscapeY else y,
                                    )
                                },
                            )
                        }
                        if (metadata.useOrientationSpecificPosition) {
                            Spacer(Modifier.height(6.dp))
                            Text("אנכי", fontWeight = FontWeight.Bold)
                            PointEditor(
                                metadata.portraitX.takeIf { it >= 0f } ?: x,
                                metadata.portraitY.takeIf { it >= 0f } ?: y,
                                metadata.toleranceXRatio,
                                metadata.toleranceYRatio,
                            ) { nx, ny ->
                                metadata = metadata.copy(portraitX = nx, portraitY = ny)
                            }
                            Text("אופקי", fontWeight = FontWeight.Bold)
                            PointEditor(
                                metadata.landscapeX.takeIf { it >= 0f } ?: x,
                                metadata.landscapeY.takeIf { it >= 0f } ?: y,
                                metadata.toleranceXRatio,
                                metadata.toleranceYRatio,
                            ) { nx, ny ->
                                metadata = metadata.copy(landscapeX = nx, landscapeY = ny)
                            }
                        }
                    }
                    ProfileSelector(
                        repo.profiles(),
                        metadata.profileId
                    ) { selected ->
                        metadata = metadata.copy(profileId = selected)
                    }
                    OutlinedButton(
                        onClick = {
                            val performer = when (AdvancedRuleRepository.currentMode(context)) {
                                AppMode.BASIC -> BasicActionPerformer(context)
                                AppMode.FULL -> KeyInterceptorAccessibilityService.instance?.let { ActionExecutor(it) }
                                    ?: UnavailableActionPerformer("מצב מלא נבחר, אבל שירות הנגישות אינו פעיל.")
                            }
                            RuleExecutionCoordinator(context, performer).execute(
                                draft,
                                test = true,
                                reason = "בדיקה ידנית",
                            )
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

    if (triggerAppDialog) {
        AppPickerDialog(
            title = "בחירת אפליקציה לזיהוי כניסה",
            onDismiss = { triggerAppDialog = false },
            onSelect = { app ->
                triggerAppDialog = false
                draft = draft.copy(
                    triggerPackage = app.packageName,
                    triggerAppName = app.label,
                )
                validationMessage = ""
            },
        )
    }

    if (appDialog) {
        AppPickerDialog(
            title = if (draft.actionType == ActionType.APP) "בחירת אפליקציית יעד" else "בחירת אפליקציה ללימוד",
            onDismiss = { appDialog = false },
            onSelect = { app ->
                appDialog = false
                when {
                    draft.actionType == ActionType.APP -> {
                        draft = draft.copy(
                            targetPackage = app.packageName,
                            targetAppName = app.label,
                        )
                    }
                    draft.actionType == ActionType.APP_TAP ||
                        draft.actionType == ActionType.MULTI_POINT_TAP -> {
                        chooseLocationApp(app.packageName, app.label)
                    }
                    else -> {
                        draft = draft.copy(
                            contextConditionValue = app.packageName,
                            contextConditionName = app.label,
                        )
                    }
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
                TextButton(onClick = {
                    showDelete = false
                    onDelete(existing)
                }) { Text("מחיקה") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("ביטול") }
            },
        )
    }
}

@Composable
private fun PositionPreview(xPercent: Int, yPercent: Int) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    androidx.compose.foundation.Canvas(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(surfaceVariant, RoundedCornerShape(16.dp))
    ) {
        val x = size.width * xPercent.coerceIn(0, 100) / 100f
        val y = size.height * yPercent.coerceIn(0, 100) / 100f
        drawLine(
            outline.copy(alpha = 0.35f),
            Offset(x, 0f),
            Offset(x, size.height),
            strokeWidth = 1.5f,
        )
        drawLine(
            outline.copy(alpha = 0.35f),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.5f,
        )
        drawCircle(
            primary.copy(alpha = 0.18f),
            radius = 22.dp.toPx(),
            center = Offset(x, y),
        )
        drawCircle(
            primary,
            radius = 9.dp.toPx(),
            center = Offset(x, y),
        )
    }
}

@Composable
private fun SystemActionPicker(
    selectedId: String,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    onActionSelected: (String) -> Unit,
) {
    val categories = SystemActionPreset.entries.map { it.categoryHebrew }.distinct()
    val selected = selectedCategory
    if (selected == null) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("בחר קטגוריה", fontWeight = FontWeight.Bold)
            categories.forEach { category ->
                OutlinedCard(
                    Modifier.fillMaxWidth().clickable { onCategorySelected(category) },
                    shape = RoundedCornerShape(13.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Tune, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(category, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    } else {
        val actions = SystemActionPreset.entries.filter { it.categoryHebrew == selected }
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selected, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = { onCategorySelected(null) }) {
                    Text("כל הקטגוריות")
                }
            }
            actions.forEach { action ->
                ChoiceChip(
                    selected = selectedId == action.id,
                    onClick = { onActionSelected(action.id) },
                    label = action.titleHebrew,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EditorSectionCard(
    number: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    Modifier.size(34.dp),
                    CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            number,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun EditorSliderRow(
    title: String,
    valueText: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontWeight = FontWeight.Medium)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    valueText,
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        content()
    }
}

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
        Text("פרופיל ClickPlus לפעולה", fontWeight = FontWeight.Medium)
        Text(
            "הפעולה תופעל רק כשהפרופיל הזה הוא הפרופיל הפעיל.",
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            profiles.forEach { profile ->
                ChoiceChip(
                    selected = selectedId == profile.id,
                    onClick = { onSelect(profile.id) },
                    label = profile.name,
                    modifier = Modifier.widthIn(min = 100.dp),
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
                    outlineColor.copy(alpha = 0.35f),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))),
                )
                drawRect(
                    primaryColor.copy(alpha = 0.15f),
                    topLeft = Offset(
                        ((safeX - safeTx).coerceAtLeast(0f)) * size.width,
                        ((safeY - safeTy).coerceAtLeast(0f)) * size.height,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        (safeTx * 2f).coerceAtMost(1f) * size.width,
                        (safeTy * 2f).coerceAtMost(1f) * size.height,
                    ),
                )
                drawCircle(primaryColor.copy(alpha = 0.18f), radius = 30f, center = Offset(px, py))
                drawCircle(primaryColor, radius = 11f, center = Offset(px, py))
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
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            borderColor,
        ),
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    label,
                    color = labelColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
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
