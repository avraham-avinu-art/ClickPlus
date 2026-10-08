@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.clickplus.ui

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
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
import com.example.clickplus.service.UnavailableActionPerformer
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import com.example.clickplus.service.RuleExecutionCoordinator
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    private var showPermissionIntro by mutableStateOf(false)
    private var permissionRefreshKey by mutableIntStateOf(0)

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefreshKey++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val runtimePrefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        showPermissionIntro = !runtimePrefs.getBoolean("permission_intro_completed", false)
        runtimePrefs.edit()
            .putBoolean("first_ui_opened", true)
            .putBoolean("background_only", false)
            .apply()

        setContent {
            ClickPlusDashboard(
                showPermissionIntro = showPermissionIntro,
                permissionRefreshKey = permissionRefreshKey,
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        runtimePermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                },
                onRequestPhone = {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_PHONE_STATE,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        runtimePermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
                    }
                },
                onOpenAccessibility = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onOpenUsage = {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
                onEnterApp = {
                    runtimePrefs.edit()
                        .putBoolean("permission_intro_completed", true)
                        .putBoolean("permission_bootstrap_done", true)
                        .putBoolean("background_only", false)
                        .apply()
                    showPermissionIntro = false
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRefreshKey++
    }
}

@Composable
private fun ClickPlusDashboard(
    showPermissionIntro: Boolean,
    permissionRefreshKey: Int,
    onRequestNotification: () -> Unit,
    onRequestPhone: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsage: () -> Unit,
    onEnterApp: () -> Unit,
) {
    if (showPermissionIntro) {
        PermissionIntroScreen(
            refreshKey = permissionRefreshKey,
            onRequestNotification = onRequestNotification,
            onRequestPhone = onRequestPhone,
            onOpenAccessibility = onOpenAccessibility,
            onOpenUsage = onOpenUsage,
            onEnterApp = onEnterApp,
        )
        return
    }

    val context = LocalContext.current
    val prefs = remember { AppPreferencesRepository(context.applicationContext) }
    val advanced = remember { AdvancedRuleRepository(context.applicationContext) }
    val mappings by prefs.mappingsFlow.collectAsState(initial = emptyList())
    val timeout by prefs.tapTimeoutFlow.collectAsState(initial = 1200L)
    val actionDelay by prefs.actionDelayFlow.collectAsState(initial = 0L)
    val showTapCount by prefs.showTapCountFlow.collectAsState(initial = false)
    var route by remember { mutableStateOf<DashboardRoute>(DashboardRoute.Home) }
    var themeMode by remember { mutableStateOf(advanced.themeMode()) }
    var workMode by remember { mutableStateOf(AdvancedRuleRepository.currentMode(context)) }

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
                onDelete = { item ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        prefs.saveMappings(mappings.filterNot { it.id == item.id })
                        advanced.removeRuleMetadata(item.id)
                    }
                },
                onToggle = { item ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        prefs.saveMappings(
                            mappings.map { current ->
                                if (current.id == item.id) current.copy(enabled = !current.enabled) else current
                            },
                        )
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
                onMode = { mode -> AdvancedRuleRepository.setMode(context, mode) },
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
                themeMode = themeMode,
                workMode = workMode,
                onBack = { route = DashboardRoute.Home },
                onTimeout = { value -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapTimeout(value) } },
                onActionDelay = { value -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveActionDelay(value) } },
                onShowTapCount = { value -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveShowTapCount(value) } },
                onTheme = {
                    themeMode = it
                    advanced.saveThemeMode(it)
                },
                onMode = {
                    workMode = it
                    AdvancedRuleRepository.setMode(context, it)
                },
                onRequestNotification = {
                    requestRuntimePermission(context, Manifest.permission.POST_NOTIFICATIONS)
                },
                onRequestPhone = {
                    requestRuntimePermission(context, Manifest.permission.READ_PHONE_STATE)
                },
                onOpenAccessibility = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onOpenUsage = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
                onBackup = { route = DashboardRoute.Backup },
            )
            DashboardRoute.Backup -> BackupScreen(
                mappings = mappings,
                timeout = timeout,
                actionDelay = actionDelay,
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
    onDelete: (KeyActionConfig) -> Unit,
    onToggle: (KeyActionConfig) -> Unit,
    onStatus: () -> Unit,
    onLogs: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val mode = AdvancedRuleRepository.currentMode(context)
    val accessibilityEnabled = isAccessibilityEnabled(context)
    var query by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<KeyActionConfig?>(null) }
    val filtered = mappings.filter {
        val q = query.trim()
        q.isBlank() || listOf(
            it.name,
            it.triggerSummary(),
            it.actionSummary(),
            it.contextSummary(),
            it.targetAppName,
            it.screenTapAppName,
        ).any { value -> value.contains(q, true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("קליק פלוס", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Outlined.Add, "הוספת פעולה")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, "הגדרות")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            Modifier.size(48.dp),
                            CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                "מצב",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(11.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "קליק פלוס פעיל",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                when {
                                    mode == AppMode.BASIC -> "מצב בסיסי · ללא נגישות"
                                    accessibilityEnabled -> "מצב מלא · נגישות פעילה"
                                    else -> "מצב מלא · נגישות לא פעילה"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HomeShortcutCard(
                        title = "מצב השירות",
                        icon = Icons.Outlined.Tune,
                        onClick = onStatus,
                    )
                    HomeShortcutCard(
                        title = "יומן",
                        icon = Icons.Outlined.History,
                        onClick = onLogs,
                    )
                    HomeShortcutCard(
                        title = "פרופילים",
                        icon = Icons.Outlined.Apps,
                        onClick = onProfiles,
                    )
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
                        onToggle = { onToggle(item) },
                    )
                }
            }
        }
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("למחוק את הפעולה?") },
            text = { Text("הפעולה וההגדרות המתקדמות שלה יוסרו.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    onDelete(item)
                }) { Text("מחיקה") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("ביטול") }
            },
        )
    }
}

@Composable
private fun HomeShortcutCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(132.dp)
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RuleCard(
    item: KeyActionConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    item.name.ifBlank { "פעולה" },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "טריגר: " + item.triggerSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "פעולה: " + item.actionSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = item.enabled,
                onCheckedChange = { onToggle() },
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Outlined.Delete, "מחיקה")
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
private fun PermissionIntroScreen(
    refreshKey: Int,
    onRequestNotification: () -> Unit,
    onRequestPhone: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsage: () -> Unit,
    onEnterApp: () -> Unit,
) {
    // The key forces a fresh permission-state read after returning from Android settings.
    val context = LocalContext.current
    refreshKey.hashCode()

    val notificationGranted = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    val phoneGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE,
    ) == PackageManager.PERMISSION_GRANTED
    val accessibilityGranted = isAccessibilityEnabled(context)
    val usageGranted = hasUsageAccess(context)

    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Surface(
                    Modifier.size(68.dp),
                    CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            item {
                Text(
                    "הגדרה ראשונית",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Text(
                    "אפשר לתת את ההרשאות אחת־אחת לפי הצורך. אפשר גם להיכנס לאפליקציה בלי לאשר אותן עכשיו.",
                    textAlign = TextAlign.Center,
                )
            }

            item {
                PermissionRow(
                    title = "התראות",
                    detail = "נדרשות להפעלת שירות הרקע.",
                    granted = notificationGranted,
                    available = Build.VERSION.SDK_INT >= 33,
                    buttonText = if (Build.VERSION.SDK_INT < 33) "לא נדרש" else if (notificationGranted) "מאושר" else "מתן הרשאה",
                    onClick = onRequestNotification,
                )
            }
            item {
                PermissionRow(
                    title = "מצב טלפון",
                    detail = "נדרש רק לפעולות או מצבים שקשורים לשיחות.",
                    granted = phoneGranted,
                    available = true,
                    buttonText = if (phoneGranted) "מאושר" else "מתן הרשאה",
                    onClick = onRequestPhone,
                )
            }
            item {
                PermissionRow(
                    title = "שירות נגישות",
                    detail = "נדרש ללחיצות במסך ולפעולות מערכת שדורשות נגישות.",
                    granted = accessibilityGranted,
                    available = true,
                    buttonText = if (accessibilityGranted) "פעיל" else "פתיחת הגדרות",
                    onClick = onOpenAccessibility,
                )
            }
            item {
                PermissionRow(
                    title = "נתוני שימוש",
                    detail = "נדרשים כאשר רוצים לזהות איזו אפליקציה נמצאת בחזית במצב בסיסי.",
                    granted = usageGranted,
                    available = true,
                    buttonText = if (usageGranted) "מאושר" else "פתיחת הגדרות",
                    onClick = onOpenUsage,
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onEnterApp,
                    Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("כניסה לאפליקציה", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    detail: String,
    granted: Boolean,
    available: Boolean,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            OutlinedButton(
                onClick = onClick,
                enabled = available && !granted,
                Modifier.fillMaxWidth(),
            ) {
                Text(buttonText)
            }
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var mode by remember(refreshKey) { mutableStateOf(AdvancedRuleRepository.currentMode(context)) }
    val service = remember(refreshKey) { isAccessibilityEnabled(context) }
    val usage = remember(refreshKey) { hasUsageAccess(context) }
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
                    detail = if (service) "השירות פעיל וזמין לפעולות הדורשות נגישות." else "השירות אינו פעיל. במצב מלא הוא נדרש לזיהוי לחיצות ולפעולות מערכת.",
                    actionText = "פתיחת הגדרות נגישות",
                    settingsHint = "פתח את שירותי הנגישות, מצא את קליק פלוס והפעל אותו.",
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                )
            }
            item {
                StatusCard(
                    title = "שימוש בנתוני שימוש",
                    ok = usage,
                    detail = if (usage) "הגישה פעילה וניתן לזהות איזו אפליקציה הייתה פתוחה לפני ההפעלה." else "הגישה אינה פעילה. היא נדרשת לזיהוי האפליקציה שהייתה פתוחה לפני ההפעלה.",
                    actionText = "פתיחת גישת נתוני שימוש",
                    settingsHint = "פתח את גישת נתוני השימוש, מצא את קליק פלוס ואפשר לה גישה.",
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                )
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("מצב עבודה", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ChoiceChip(
                                    selected = mode == AppMode.FULL,
                                    onClick = { mode = AppMode.FULL; onMode(AppMode.FULL) },
                                    label = "מלא",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                ChoiceChip(
                                    selected = mode == AppMode.BASIC,
                                    onClick = { mode = AppMode.BASIC; onMode(AppMode.BASIC) },
                                    label = "בסיסי ללא נגישות",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Text(
                            if (mode == AppMode.FULL) {
                                "כל היכולות זמינות, כולל זיהוי מיקום לחיצה ופעולות מערכת."
                            } else {
                                "במצב זה אפשר לזהות כניסות, לפתוח אפליקציות ולהפעיל פעולות מדיה, ווליום, הגדרות וחייגן. אי אפשר לזהות מיקום לחיצה במסך או לבצע פעולות מערכת גלובליות כמו בית, חזרה, התראות ויישומים אחרונים."
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
private fun StatusCard(
    title: String,
    ok: Boolean,
    detail: String,
    actionText: String,
    settingsHint: String,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            if (!ok) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(actionText)
                }
                Text(
                    settingsHint,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(AdvancedRuleRepository.logs(context)) }
    var confirmClear by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                items = AdvancedRuleRepository.logs(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("יומן פעילות") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowForward, "חזרה")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { confirmClear = true },
                        enabled = items.isNotEmpty(),
                    ) {
                        Text("ניקוי")
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("אין אירועים להצגה עדיין.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { log ->
                    val statusText = when (log.success) {
                        true -> "הצליח"
                        false -> "נכשל"
                        null -> "בביצוע"
                    }
                    val statusWithDetail = if (log.success == false && log.detail.isNotBlank()) {
                        statusText + " — " + log.detail
                    } else {
                        statusText
                    }
                    val trigger = log.triggerDescription.ifBlank {
                        if (log.message.contains("בדיקה ידנית")) "בדיקה ידנית" else "הפעלה"
                    }
                    val action = log.actionDescription.ifBlank {
                        log.detail.ifBlank { log.message }
                    }

                    OutlinedCard(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                "הטריגר המפעיל: " + trigger,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "פעולה: " + action,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "סטטוס: " + statusWithDetail +
                                    " · " +
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                        .format(Date(log.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("לנקות את היומן?") },
            text = { Text("כל הרשומות ביומן הפעילות יימחקו.") },
            confirmButton = {
                TextButton(onClick = {
                    AdvancedRuleRepository.clearLogs(context)
                    items = emptyList()
                    confirmClear = false
                }) {
                    Text("מחיקה")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("ביטול")
                }
            },
        )
    }
}

@Composable
private fun ProfilesScreen(mappings: List<KeyActionConfig>, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AdvancedRuleRepository(context) }
    var profiles by remember { mutableStateOf(repo.profiles()) }
    var activeProfileId by remember { mutableStateOf(repo.activeProfileId()) }
    var createDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ClickPlusProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("פרופילים") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowForward, "חזרה")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        newName = ""
                        createDialog = true
                    }) {
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
            items(profiles, key = { it.id }) { profile ->
                val count = mappings.count {
                    repo.getRuleMetadata(it.id).profileId == profile.id
                }
                val isActive = profile.id == activeProfileId

                Card(
                    Modifier.fillMaxWidth().clickable {
                        if (!isActive) {
                            activeProfileId = profile.id
                            repo.setActiveProfileId(profile.id)
                            profiles = repo.profiles()
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(profile.name, fontWeight = FontWeight.Bold)
                            Text(
                                if (count == 1) "פעולה אחת" else count.toString() + " פעולות",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Box(
                            Modifier.size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isActive) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = "פרופיל פעיל",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        Box(
                            Modifier.size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (profile.id != "default") {
                                IconButton(onClick = { deleteTarget = profile }) {
                                    Icon(Icons.Outlined.Delete, "מחיקה")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (createDialog) {
        AlertDialog(
            onDismissRequest = { createDialog = false },
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
                        val created = profiles + ClickPlusProfile(name = name, enabled = false)
                        repo.saveProfiles(created)
                        profiles = repo.profiles()
                    }
                    createDialog = false
                }) {
                    Text("הוספה")
                }
            },
            dismissButton = {
                TextButton(onClick = { createDialog = false }) {
                    Text("ביטול")
                }
            },
        )
    }

    deleteTarget?.let { target ->
        val assignedCount = mappings.count {
            repo.getRuleMetadata(it.id).profileId == target.id
        }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("למחוק את הפרופיל?") },
            text = {
                Text(
                    if (assignedCount == 0) {
                        "הפרופיל "" + target.name + "" יימחק."
                    } else {
                        "יש " + assignedCount + " פעולות שמשויכות לפרופיל "" +
                            target.name +
                            "". הפעולות יועברו לפרופיל "כללי" לפני המחיקה."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val defaultProfileId = "default"
                    mappings.filter {
                        repo.getRuleMetadata(it.id).profileId == target.id
                    }.forEach { rule ->
                        val metadata = repo.getRuleMetadata(rule.id)
                        repo.saveRuleMetadata(rule.id, metadata.copy(profileId = defaultProfileId))
                    }

                    val remaining = profiles.filterNot { it.id == target.id }
                    repo.saveProfiles(remaining)
                    if (activeProfileId == target.id) {
                        activeProfileId = defaultProfileId
                        repo.setActiveProfileId(defaultProfileId)
                    }
                    profiles = repo.profiles()
                    activeProfileId = repo.activeProfileId()
                    deleteTarget = null
                }) {
                    Text(if (assignedCount == 0) "מחיקה" else "העברה לכללי ומחיקה")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("ביטול")
                }
            },
        )
    }
}

@Composable
private fun CompactChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        label = {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

@Composable
private fun SettingsScreen(
    timeout: Long,
    actionDelay: Long,
    showTapCount: Boolean,
    themeMode: String,
    workMode: AppMode,
    onBack: () -> Unit,
    onTimeout: (Long) -> Unit,
    onActionDelay: (Long) -> Unit,
    onShowTapCount: (Boolean) -> Unit,
    onTheme: (String) -> Unit,
    onMode: (AppMode) -> Unit,
    onRequestNotification: () -> Unit,
    onRequestPhone: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsage: () -> Unit,
    onBackup: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(topBar = { SimpleTopBar("הגדרות", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("הפעלת המערכת", "הגדרות שקובעות מתי המערכת מזהה רצף ומתי היא מבצעת פעולה.") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("מצב עבודה", fontWeight = FontWeight.Bold)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChoiceChip(
                                selected = workMode == AppMode.FULL,
                                onClick = { onMode(AppMode.FULL) },
                                label = "מלא",
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                selected = workMode == AppMode.BASIC,
                                onClick = { onMode(AppMode.BASIC) },
                                label = "בסיסי ללא נגישות",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        EditorSliderRow(
                            title = "זמן חלון",
                            valueText = formatDurationMs(timeout),
                        ) {
                            Slider(
                                value = timeout.toFloat(),
                                onValueChange = { onTimeout(it.toLong()) },
                                valueRange = 300f..1500f,
                                steps = 11,
                            )
                        }

                        EditorSliderRow(
                            title = "השהיה לפני פעולה",
                            valueText = formatDurationMs(actionDelay),
                        ) {
                            Slider(
                                value = actionDelay.toFloat(),
                                onValueChange = { onActionDelay(it.toLong()) },
                                valueRange = 0f..5000f,
                                steps = 9,
                            )
                        }
                    }
                }
            }

            item {
                SettingCard("חיווי", "האם להציג מונה קצר של מספר הלחיצות.") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("הצגת מונה לחיצות", Modifier.weight(1f))
                        Switch(
                            checked = showTapCount,
                            onCheckedChange = onShowTapCount,
                        )
                    }
                }
            }

            item {
                SettingCard("מראה", "בחירת ערכת הצבעים של האפליקציה.") {
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
                SettingCard("הרשאות", "כל הרשאה ניתנת בנפרד לפי הצורך.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsPermissionButton(
                            title = "התראות",
                            granted = Build.VERSION.SDK_INT < 33 ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) == PackageManager.PERMISSION_GRANTED,
                            onClick = onRequestNotification,
                            disabled = Build.VERSION.SDK_INT < 33,
                        )
                        SettingsPermissionButton(
                            title = "מצב טלפון",
                            granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_PHONE_STATE,
                            ) == PackageManager.PERMISSION_GRANTED,
                            onClick = onRequestPhone,
                        )
                        SettingsPermissionButton(
                            title = "שירות נגישות",
                            granted = isAccessibilityEnabled(context),
                            onClick = onOpenAccessibility,
                        )
                        SettingsPermissionButton(
                            title = "נתוני שימוש",
                            granted = hasUsageAccess(context),
                            onClick = onOpenUsage,
                        )
                    }
                }
            }

            item {
                SettingCard("גיבוי", "שמירה, שחזור ואיפוס של נתוני האפליקציה.") {
                    Button(
                        onClick = onBackup,
                        Modifier.fillMaxWidth(),
                    ) {
                        Text("פתח גיבוי")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPermissionButton(
    title: String,
    granted: Boolean,
    onClick: () -> Unit,
    disabled: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !granted && !disabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(title + if (granted) " · מאושר" else "")
    }
}

@Composable
private fun ThemeChip(label: String, value: String, selectedValue: String, onTheme: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FilterChip(
        selected = selectedValue == value,
        onClick = { onTheme(value) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        label = {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        },
        leadingIcon = { Icon(icon, null) },
    )
}

@Composable
private fun SettingCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
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
    var pendingImport by remember { mutableStateOf<JSONObject?>(null) }
    var resetMode by remember { mutableStateOf<String?>(null) }

    val makeBackup = {
        advanced.exportJson(
            JSONArray().apply {
                mappings.forEach { put(it.toJson()) }
            }.toString(),
            JSONObject()
                .put("tapTimeoutMs", timeout)
                .put("actionDelayMs", actionDelay)
                .put("showTapCount", showTapCount)
                .put("mode", AdvancedRuleRepository.currentMode(context).name)
                .put("themeMode", advanced.themeMode()),
        )
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { selected ->
            scope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(selected)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("קובץ ריק")
                    pendingImport = JSONObject(json)
                }.onFailure {
                    message = "הייבוא נכשל: " + (it.message ?: "שגיאה")
                }
            }
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { selected ->
            runCatching {
                context.contentResolver.openOutputStream(selected)
                    ?.bufferedWriter()
                    ?.use { it.write(makeBackup()) }
                    ?: error("לא ניתן לכתוב לקובץ")
                message = "הגיבוי נשמר בהצלחה."
            }.onFailure {
                message = "שמירת הגיבוי נכשלה: " + (it.message ?: "שגיאה")
            }
        }
    }

    Scaffold(topBar = { SimpleTopBar("גיבוי והעברה", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("ייצוא ושיתוף", "שמור את כל הפעולות, הפרופילים וההגדרות.") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { exportLauncher.launch("clickplus-backup.json") },
                            Modifier.weight(1f),
                        ) {
                            Text("ייצוא")
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val file = java.io.File(context.cacheDir, "clickplus-backup.json")
                                    file.writeText(makeBackup())
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
                                        }.let { Intent.createChooser(it, "שיתוף גיבוי ClickPlus") },
                                    )
                                    message = "נפתח מסך השיתוף."
                                }.onFailure {
                                    message = "השיתוף נכשל: " + (it.message ?: "שגיאה")
                                }
                            },
                            Modifier.weight(1f),
                        ) {
                            Text("שיתוף")
                        }
                    }
                }
            }

            item {
                SettingCard("ייבוא", "בדוק את תוכן הגיבוי לפני שהוא מחליף את הנתונים הנוכחיים.") {
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                        Modifier.fillMaxWidth(),
                    ) {
                        Text("בחירת גיבוי לייבוא")
                    }
                }
            }

            item {
                SettingCard("איפוס פעולות", "מוחק את כל הפעולות וההגדרות המתקדמות שלהן, בלי למחוק את הפרופילים.") {
                    OutlinedButton(
                        onClick = { resetMode = "actions" },
                        Modifier.fillMaxWidth(),
                    ) {
                        Text("איפוס פעולות")
                    }
                }
            }

            item {
                SettingCard("איפוס כל האפליקציה", "מחזיר את האפליקציה למצב התחלתי ומוחק פעולות, פרופילים, יומן והגדרות.") {
                    OutlinedButton(
                        onClick = { resetMode = "all" },
                        Modifier.fillMaxWidth(),
                    ) {
                        Text("איפוס כל האפליקציה")
                    }
                }
            }

            if (message.isNotBlank()) {
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    pendingImport?.let { root ->
        val array = root.optJSONArray("mappings") ?: JSONArray()
        val profileArray = root.optJSONArray("profiles") ?: JSONArray()
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("ייבוא גיבוי") },
            text = {
                Text(
                    "הקובץ מכיל " + array.length() + " פעולות ו-" +
                        profileArray.length() + " פרופילים. הייבוא יחליף את הנתונים הקיימים.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            val imported = buildList {
                                for (i in 0 until array.length()) {
                                    array.optJSONObject(i)?.let { add(KeyActionConfig.fromJson(it)) }
                                }
                            }
                            basePrefs.saveMappings(imported)
                            root.optJSONObject("settings")?.let { settings ->
                                if (settings.has("tapTimeoutMs")) {
                                    basePrefs.saveTapTimeout(settings.optLong("tapTimeoutMs", 1200L))
                                }
                                if (settings.has("actionDelayMs")) {
                                    basePrefs.saveActionDelay(settings.optLong("actionDelayMs", 0L))
                                }
                                if (settings.has("showTapCount")) {
                                    basePrefs.saveShowTapCount(settings.optBoolean("showTapCount", false))
                                }
                            }
                            advanced.importBundle(root)
                            message = "הגיבוי יובא בהצלחה."
                            pendingImport = null
                        }.onFailure {
                            message = "הייבוא נכשל: " + (it.message ?: "שגיאה")
                        }
                    }
                }) {
                    Text("ייבוא")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text("ביטול")
                }
            },
        )
    }

    resetMode?.let { mode ->
        val fullReset = mode == "all"
        AlertDialog(
            onDismissRequest = { resetMode = null },
            title = { Text(if (fullReset) "לאפס את כל האפליקציה?" else "לאפס את הפעולות?") },
            text = {
                Text(
                    if (fullReset) {
                        "כל הפעולות, הפרופילים, היומן וההגדרות יימחקו."
                    } else {
                        "כל הפעולות וההגדרות המתקדמות שלהן יימחקו."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (fullReset) {
                            basePrefs.resetAll()
                            advanced.resetPersistentState()
                            advanced.saveProfiles(listOf(ClickPlusProfile("default", "כללי", true)))
                            AdvancedRuleRepository.setMode(context, AppMode.FULL)
                            advanced.saveThemeMode("system")
                            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                                .edit()
                                .clear()
                                .apply()
                        } else {
                            basePrefs.saveMappings(emptyList())
                            advanced.clearAllRuleMetadata()
                        }
                        message = "האיפוס הושלם."
                        resetMode = null
                    }
                }) {
                    Text("איפוס")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetMode = null }) {
                    Text("ביטול")
                }
            },
        )
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
    var orientation by remember {
        mutableStateOf(
            if (context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                "landscape"
            } else {
                "portrait"
            }
        )
    }
    var showDelete by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var tapPreviewImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(draft.triggerType, draft.actionType) {
        if (draft.triggerType == TriggerType.SCREEN_TAP && draft.actionType != ActionType.APP_TAP) {
            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("tap_learning", false)
                .apply()
        }
    }

    LaunchedEffect(draft.id) {
        var loadedScreenshotPath = ""
        while (true) {
            val p = context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            val screenshotPath = p.getString("tap_capture_screenshot_path", "").orEmpty()
            if (screenshotPath.isNotBlank() && screenshotPath != loadedScreenshotPath) {
                BitmapFactory.decodeFile(screenshotPath)?.let { bitmap ->
                    tapPreviewImage = bitmap.asImageBitmap()
                    loadedScreenshotPath = screenshotPath
                }
            }

            if (p.getBoolean("tap_capture_ready", false)) {
                val capturedX = p.getFloat("tap_capture_x_ratio", -1f)
                val capturedY = p.getFloat("tap_capture_y_ratio", -1f)
                val pkg = p.getString("tap_capture_package", "").orEmpty()
                val name = p.getString("tap_capture_app_name", "").orEmpty()
                if (capturedX >= 0f && capturedY >= 0f) {
                    draft = if (draft.actionType == ActionType.APP_TAP) {
                        draft.copy(
                            screenTapPackage = pkg,
                            screenTapAppName = name,
                            screenTapXRatio = capturedX,
                            screenTapYRatio = capturedY,
                        )
                    } else {
                        draft.copy(
                            triggerType = TriggerType.SCREEN_TAP,
                            screenTapPackage = pkg,
                            screenTapAppName = name,
                            screenTapXRatio = capturedX,
                            screenTapYRatio = capturedY,
                        )
                    }
                    metadata = if (orientation == "landscape") {
                        metadata.copy(landscapeX = capturedX, landscapeY = capturedY)
                    } else {
                        metadata.copy(portraitX = capturedX, portraitY = capturedY)
                    }
                }
                p.edit().putBoolean("tap_capture_ready", false).apply()
                learning = false
            }
            kotlinx.coroutines.delay(250)
        }
    }

    val rawX = if (orientation == "landscape" && metadata.landscapeX >= 0f) {
        metadata.landscapeX
    } else if (metadata.portraitX >= 0f) {
        metadata.portraitX
    } else {
        draft.screenTapXRatio.coerceAtLeast(0f)
    }
    val rawY = if (orientation == "landscape" && metadata.landscapeY >= 0f) {
        metadata.landscapeY
    } else if (metadata.portraitY >= 0f) {
        metadata.portraitY
    } else {
        draft.screenTapYRatio.coerceAtLeast(0f)
    }
    val x = rawX.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val y = rawY.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "הוספת פעולה" else "עריכת פעולה") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowForward, "חזרה")
                    }
                },
                actions = {
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
                    onClick = {
                        val prepared = draft.copy(
                            name = draft.name.ifBlank { draft.triggerType.titleHebrew },
                            screenTapXRatio = if (draft.screenTapXRatio >= 0f) draft.screenTapXRatio else x,
                            screenTapYRatio = if (draft.screenTapYRatio >= 0f) draft.screenTapYRatio else y,
                            screenTapToleranceRatio = maxOf(
                                metadata.toleranceXRatio,
                                metadata.toleranceYRatio,
                            ),
                        )
                        val error = validateRuleBeforeSave(context, prepared, metadata, repo)
                        if (error != null) {
                            validationMessage = error
                        } else {
                            validationMessage = ""
                            onSave(prepared, metadata)
                        }
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .height(50.dp),
                ) {
                    Icon(Icons.Outlined.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("שמירת פעולה", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                EditorSectionCard(
                    number = "1",
                    title = "פרטי הפעולה",
                    subtitle = "השם שיוצג ביומן וברשימת הפעולות.",
                ) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("שם הפעולה") },
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("הפעלת הפעולה", fontWeight = FontWeight.Medium)
                            Text(
                                if (draft.enabled) "הפעולה פעילה וזמינה להפעלה."
                                else "הפעולה שמורה אך לא תופעל.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = draft.enabled,
                            onCheckedChange = { draft = draft.copy(enabled = it) },
                        )
                    }
                }
            }

            item {
                EditorSectionCard(
                    number = "2",
                    title = "מתי להפעיל?",
                    subtitle = "בחר את סוג ההפעלה ואת מספר הכניסות או הלחיצות.",
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceChip(
                            selected = draft.triggerType == TriggerType.APP_ENTRY,
                            onClick = { draft = draft.copy(triggerType = TriggerType.APP_ENTRY) },
                            label = "כניסה לאפליקציה",
                            modifier = Modifier.weight(1f),
                        )
                        ChoiceChip(
                            selected = draft.triggerType == TriggerType.SCREEN_TAP,
                            onClick = { draft = draft.copy(triggerType = TriggerType.SCREEN_TAP) },
                            label = "לחיצה במיקום",
                            modifier = Modifier.weight(1f),
                            enabled = AdvancedRuleRepository.currentMode(context) == AppMode.FULL,
                        )
                    }
                    Text(
                        "מספר הפעלות רצופות",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..10).forEach { n ->
                            CompactChoiceChip(
                                selected = draft.pressCount == n,
                                onClick = { draft = draft.copy(pressCount = n) },
                                label = n.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                EditorSectionCard(
                    number = "3",
                    title = "באיזה מצב?",
                    subtitle = if (draft.triggerType == TriggerType.APP_ENTRY) {
                        "קבע מתי הכלל מתאים לפי האפליקציה והמצב לפני ClickPlus."
                    } else {
                        "בחר באיזו אפליקציה ובאיזה מיקום לחיצה הכלל יזוהה."
                    },
                ) {
                    if (draft.triggerType == TriggerType.APP_ENTRY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChoiceChip(
                                selected = draft.contextConditionType == ContextConditionType.ANY,
                                onClick = {
                                    draft = draft.copy(contextConditionType = ContextConditionType.ANY)
                                },
                                label = "בכל מצב",
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                selected = draft.contextConditionType != ContextConditionType.ANY,
                                onClick = {
                                    if (draft.contextConditionType == ContextConditionType.ANY) {
                                        draft = draft.copy(contextConditionType = ContextConditionType.APP)
                                    }
                                },
                                label = "לפי מצב מסוים",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (AdvancedRuleRepository.currentMode(context) == AppMode.BASIC) {
                            Text(
                                "במצב בסיסי חלק מהמצבים אינם זמינים.",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        if (draft.contextConditionType != ContextConditionType.ANY) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ContextConditionType.entries
                                    .filter { it != ContextConditionType.ANY }
                                    .forEach { type ->
                                        ChoiceChip(
                                            selected = draft.contextConditionType == type,
                                            onClick = {
                                                draft = draft.copy(contextConditionType = type)
                                            },
                                            label = type.titleHebrew,
                                            modifier = Modifier.width(132.dp),
                                            enabled = isContextConditionAvailable(
                                                AdvancedRuleRepository.currentMode(context),
                                                type,
                                            ),
                                        )
                                    }
                            }
                        }

                        if (
                            draft.contextConditionType == ContextConditionType.APP ||
                            draft.contextConditionType == ContextConditionType.RADIO
                        ) {
                            OutlinedButton(
                                onClick = { appDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Apps, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (draft.contextConditionName.isBlank()) "בחירת אפליקציה"
                                    else draft.contextConditionName,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { screenAppDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Apps, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (draft.screenTapPackage.isBlank()) "בחירת אפליקציה לזיהוי הלחיצה"
                                else "החלפת אפליקציית הזיהוי",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (draft.screenTapPackage.isBlank()) {
                            Text(
                                "אין צורך לפתוח את האפליקציה או להפעיל שכבת לימוד. בחר אפליקציה והגדר את הנקודה בתצוגה המקדימה.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        if (draft.screenTapPackage.isNotBlank()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        draft.screenTapAppName.ifBlank { "האפליקציה שנבחרה" },
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "מיקום שמור: X " + (x * 100f).toInt() + "% · Y " + (y * 100f).toInt() + "%",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                ChoiceChip(
                                    selected = false,
                                    onClick = { screenAppDialog = true },
                                    label = "החלפה",
                                    modifier = Modifier.widthIn(min = 88.dp),
                                )
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ChoiceChip(
                                    selected = orientation == "portrait",
                                    onClick = { orientation = "portrait" },
                                    label = "אנכי",
                                    modifier = Modifier.weight(1f),
                                )
                                ChoiceChip(
                                    selected = orientation == "landscape",
                                    onClick = { orientation = "landscape" },
                                    label = "אופקי",
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            PointEditor(
                                x = x,
                                y = y,
                                toleranceX = metadata.toleranceXRatio,
                                toleranceY = metadata.toleranceYRatio,
                                onChange = { nx, ny ->
                                    draft = draft.copy(
                                        screenTapXRatio = nx,
                                        screenTapYRatio = ny,
                                    )
                                    metadata = if (orientation == "landscape") {
                                        metadata.copy(
                                            landscapeX = nx,
                                            landscapeY = ny,
                                        )
                                    } else {
                                        metadata.copy(
                                            portraitX = nx,
                                            portraitY = ny,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

            item {
                EditorSectionCard(
                    number = "4",
                    title = "מה לבצע?",
                    subtitle = "בחר מה ClickPlus יעשה לאחר שהכלל הופעל.",
                ) {
                    ChoiceChip(
                        selected = draft.actionType == ActionType.SYSTEM,
                        onClick = {
                            draft = draft.copy(
                                actionType = ActionType.SYSTEM,
                                systemActionId = draft.systemActionId.ifBlank { SystemActionPreset.HOME.id },
                            )
                        },
                        label = "פעולת מערכת",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceChip(
                            selected = draft.actionType == ActionType.APP,
                            onClick = {
                                draft = draft.copy(actionType = ActionType.APP, systemActionId = "")
                            },
                            label = "פתיחת אפליקציה",
                            modifier = Modifier.weight(1f),
                        )
                        ChoiceChip(
                            selected = draft.actionType == ActionType.APP_TAP,
                            onClick = {
                                draft = draft.copy(actionType = ActionType.APP_TAP, systemActionId = "")
                            },
                            label = "פתיחה+לחיצות",
                            modifier = Modifier.weight(1f),
                            enabled = AdvancedRuleRepository.currentMode(context) == AppMode.FULL,
                        )
                    }

                    when (draft.actionType) {
                        ActionType.SYSTEM -> {
                            val groups = listOf(
                                "ניווט" to listOf(
                                    SystemActionPreset.HOME,
                                    SystemActionPreset.BACK,
                                    SystemActionPreset.RECENTS,
                                    SystemActionPreset.NOTIFICATIONS,
                                ),
                                "מדיה" to listOf(
                                    SystemActionPreset.MEDIA_PLAY_PAUSE,
                                    SystemActionPreset.MEDIA_NEXT,
                                    SystemActionPreset.MEDIA_PREVIOUS,
                                ),
                                "שמע" to listOf(
                                    SystemActionPreset.VOLUME_UP,
                                    SystemActionPreset.VOLUME_DOWN,
                                ),
                                "מערכת" to listOf(
                                    SystemActionPreset.SETTINGS,
                                    SystemActionPreset.DIALER,
                                ),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                groups.forEach { (groupTitle, actions) ->
                                    Text(
                                        groupTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        actions.forEach { action ->
                                            ChoiceChip(
                                                selected = draft.systemActionId == action.id,
                                                onClick = {
                                                    draft = draft.copy(systemActionId = action.id)
                                                },
                                                label = action.titleHebrew,
                                                modifier = Modifier.widthIn(min = 140.dp),
                                                enabled = isSystemActionAvailable(
                                                    AdvancedRuleRepository.currentMode(context),
                                                    action,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        ActionType.APP -> {
                            OutlinedButton(onClick = { appDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Apps, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (draft.targetAppName.isBlank()) "בחירת אפליקציית יעד" else draft.targetAppName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        ActionType.APP_TAP -> {
                            Text(
                                "מספר לחיצות",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                (1..10).forEach { n ->
                                    CompactChoiceChip(
                                        selected = draft.actionTapCount == n,
                                        onClick = {
                                            draft = draft.copy(actionTapCount = n)
                                        },
                                        label = n.toString(),
                                        modifier = Modifier.width(42.dp),
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isAccessibilityEnabled(context)) {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } else if (draft.screenTapPackage.isBlank()) {
                                        screenAppDialog = true
                                    } else {
                                        val p = context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                                        tapPreviewImage = null
                                        p.edit()
                                            .remove("tap_capture_screenshot_path")
                                            .putBoolean("tap_learning", true)
                                            .putString("tap_learning_package", draft.screenTapPackage)
                                            .apply()
                                        context.packageManager.getLaunchIntentForPackage(draft.screenTapPackage)?.let {
                                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(it)
                                            learning = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.LocationOn, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (draft.screenTapPackage.isBlank()) "בחירת אפליקציה ולימוד נקודת לחיצה" else "לימוד מחדש של נקודת הלחיצה",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (learning) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        "מצב לימוד פעיל: לחץ באפליקציה על היעד שבו תתבצע הלחיצה האוטומטית.",
                                        modifier = Modifier.padding(12.dp),
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            if (draft.screenTapPackage.isNotBlank()) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            draft.screenTapAppName.ifBlank { "האפליקציה שנבחרה" },
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "נקודת לחיצה: X " + (x * 100f).toInt() + "% · Y " + (y * 100f).toInt() + "%",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    ChoiceChip(
                                        selected = false,
                                        onClick = { screenAppDialog = true },
                                        label = "החלפה",
                                        modifier = Modifier.widthIn(min = 88.dp),
                                    )
                                }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ChoiceChip(
                                        selected = orientation == "portrait",
                                        onClick = { orientation = "portrait" },
                                        label = "אנכי",
                                        modifier = Modifier.weight(1f),
                                    )
                                    ChoiceChip(
                                        selected = orientation == "landscape",
                                        onClick = { orientation = "landscape" },
                                        label = "אופקי",
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                PointEditor(
                                    x = x,
                                    y = y,
                                    toleranceX = metadata.toleranceXRatio,
                                    screenshot = tapPreviewImage,
                                    screenshot = tapPreviewImage,
                                    toleranceY = metadata.toleranceYRatio,
                                    onChange = { nx, ny ->
                                        draft = draft.copy(screenTapXRatio = nx, screenTapYRatio = ny)
                                        metadata = if (orientation == "landscape") {
                                            metadata.copy(landscapeX = nx, landscapeY = ny)
                                        } else {
                                            metadata.copy(portraitX = nx, portraitY = ny)
                                        }
                                    },
                                )
                                Text(
                                    "בעת ההפעלה האפליקציה תיפתח על המסך, תזוהה בחזית, ואז ClickPlus יבצע את הלחיצה בנקודה הזו.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    if (draft.actionType == ActionType.APP && draft.targetPackage.isNotBlank()) {
                        Text(
                            "יעד: " + draft.targetAppName.ifBlank { draft.targetPackage },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    permissionRequirementFor(context, draft)?.let { requirement ->
                        PermissionRequirementCard(
                            message = requirement.first,
                            onClick = requirement.second,
                        )
                    }

                    if (AdvancedRuleRepository.currentMode(context) == AppMode.BASIC) {
                        Text(
                            "אפשרויות שאינן נתמכות במצב בסיסי מוצגות מושבתות.",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            item {
                EditorSectionCard(
                    number = "5",
                    title = "הגדרות מתקדמות",
                    subtitle = "ניסיונות, הגנה ואזור התאמה.",
                ) {
                    EditorSliderRow(
                        title = "Cooldown",
                        valueText = metadata.cooldownMs.toString() + "ms",
                    ) {
                        Slider(
                            value = metadata.cooldownMs.coerceIn(0L, 10_000L).toFloat(),
                            onValueChange = {
                                metadata = metadata.copy(
                                    cooldownMs = it.toLong().coerceIn(0L, 60_000L),
                                )
                            },
                            valueRange = 0f..10_000f,
                            steps = 9,
                        )
                    }

                    EditorSliderRow(
                        title = "מספר ניסיונות",
                        valueText = metadata.retries.toString(),
                    ) {
                        Slider(
                            value = metadata.retries.coerceIn(1, 3).toFloat(),
                            onValueChange = {
                                metadata = metadata.copy(
                                    retries = it.toInt().coerceIn(1, 3),
                                )
                            },
                            valueRange = 1f..3f,
                            steps = 1,
                        )
                    }

                    if (draft.triggerType == TriggerType.SCREEN_TAP) {
                        EditorSliderRow(
                            title = "רוחב אזור התאמה",
                            valueText = (metadata.toleranceXRatio * 100f).toInt().toString() + "%",
                        ) {
                            Slider(
                                value = metadata.toleranceXRatio.coerceIn(0.01f, 0.25f),
                                onValueChange = {
                                    metadata = metadata.copy(
                                        toleranceXRatio = it.coerceIn(0.01f, 0.25f),
                                    )
                                },
                                valueRange = 0.01f..0.25f,
                            )
                        }

                        EditorSliderRow(
                            title = "גובה אזור התאמה",
                            valueText = (metadata.toleranceYRatio * 100f).toInt().toString() + "%",
                        ) {
                            Slider(
                                value = metadata.toleranceYRatio.coerceIn(0.01f, 0.25f),
                                onValueChange = {
                                    metadata = metadata.copy(
                                        toleranceYRatio = it.coerceIn(0.01f, 0.25f),
                                    )
                                },
                                valueRange = 0.01f..0.25f,
                            )
                        }
                    }

                    ProfileSelector(
                        profiles = repo.profiles(),
                        selectedId = metadata.profileId,
                        onSelect = { metadata = metadata.copy(profileId = it) },
                    )
                }
            }

            item {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("בדיקת הפעולה")
                }
            }

            if (validationMessage.isNotBlank()) {
                item {
                    Text(
                        validationMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }

    if (appDialog) {
        AppPickerDialog(
            title = "בחירת אפליקציה",
            onDismiss = { appDialog = false },
            onSelect = { app ->
                if (draft.triggerType == TriggerType.SCREEN_TAP || draft.actionType == ActionType.APP_TAP) {
                    draft = draft.copy(
                        screenTapPackage = app.packageName,
                        screenTapAppName = app.label,
                        screenTapXRatio = -1f,
                        screenTapYRatio = -1f,
                    )
                } else if (
                    draft.triggerType == TriggerType.APP_ENTRY &&
                    (
                        draft.contextConditionType == ContextConditionType.APP ||
                            draft.contextConditionType == ContextConditionType.RADIO
                    )
                ) {
                    draft = draft.copy(
                        contextConditionValue = app.packageName,
                        contextConditionName = app.label,
                    )
                } else {
                    draft = draft.copy(
                        targetPackage = app.packageName,
                        targetAppName = app.label,
                    )
                }
                appDialog = false
            },
        )
    }

    if (screenAppDialog) {
        AppPickerDialog(
            title = if (draft.actionType == ActionType.APP_TAP) "אפליקציה לביצוע הלחיצה" else "אפליקציה לזיהוי הלחיצה",
            onDismiss = { screenAppDialog = false },
            onSelect = { app ->
                draft = draft.copy(
                    screenTapPackage = app.packageName,
                    screenTapAppName = app.label,
                    screenTapXRatio = -1f,
                    screenTapYRatio = -1f,
                )
                tapPreviewImage = null
                context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                    .edit()
                    .remove("tap_capture_screenshot_path")
                    .apply()
                screenAppDialog = false
            },
        )
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("למחוק את הפעולה?") },
            text = { Text("הכלל וההגדרות המתקדמות שלו יוסרו.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete(existing)
                    },
                ) {
                    Text("מחיקה")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("ביטול")
                }
            },
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider()
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
            Text(
                valueText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
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
                        val label = context.packageManager.getApplicationLabel(info).toString()
                        val icon = runCatching { info.loadIcon(context.packageManager) }.getOrNull()
                        InstalledApp(info.packageName, label, icon)
                    }.getOrNull()
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.ROOT) }
                .toList()
        }.getOrDefault(emptyList())
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
                            val iconBitmap = remember(app.packageName) {
                                app.icon?.let { drawable ->
                                    runCatching { drawable.toBitmap(42, 42).asImageBitmap() }.getOrNull()
                                }
                            }
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(7.dp),
                                )
                            } else {
                                Icon(Icons.Outlined.Apps, null, Modifier.padding(9.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            app.label,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
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
            ChoiceChip(
                selected = selectedId == profile.id,
                onClick = { onSelect(profile.id) },
                label = profile.name,
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
    screenshot: androidx.compose.ui.graphics.ImageBitmap? = null,
    onChange: (Float, Float) -> Unit,
) {
    val safeX = x.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val safeY = y.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val safeToleranceX = toleranceX.takeIf { it.isFinite() }?.coerceIn(0.01f, 0.25f) ?: 0.08f
    val safeToleranceY = toleranceY.takeIf { it.isFinite() }?.coerceIn(0.01f, 0.25f) ?: 0.08f
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("תצוגה מקדימה · גרור את הנקודה למיקום המדויק")
        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    surfaceColor,
                    RoundedCornerShape(18.dp),
                ),
        ) {
            if (screenshot != null) {
                Image(
                    bitmap = screenshot,
                    contentDescription = "צילום מסך של האפליקציה",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                )
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .pointerInput(safeX, safeY) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointX = safeX * size.width
                            val pointY = safeY * size.height
                            val dx = down.position.x - pointX
                            val dy = down.position.y - pointY
                            val handleRadius = 40.dp.toPx()
                            val startsOnHandle = dx * dx + dy * dy <= handleRadius * handleRadius
                            if (!startsOnHandle) return@awaitEachGesture

                            down.consume()
                            drag(down.id) { change ->
                                val nx = if (size.width > 0f) {
                                    (change.position.x / size.width).coerceIn(0f, 1f)
                                } else safeX
                                val ny = if (size.height > 0f) {
                                    (change.position.y / size.height).coerceIn(0f, 1f)
                                } else safeY
                                onChange(nx, ny)
                                change.consume()
                            }
                        }
                    }
            ) {
                val px = safeX * size.width
                val py = safeY * size.height
                drawRect(
                    color = outlineColor.copy(alpha = 0.35f),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
                )
                drawRect(
                    color = primaryColor.copy(alpha = 0.16f),
                    topLeft = Offset((safeX - safeToleranceX).coerceAtLeast(0f) * size.width, (safeY - safeToleranceY).coerceAtLeast(0f) * size.height),
                    size = androidx.compose.ui.geometry.Size(
                        ((safeToleranceX * 2f).coerceAtMost(1f)) * size.width,
                        ((safeToleranceY * 2f).coerceAtMost(1f)) * size.height,
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
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "חזרה") } },
    )
}

private fun requestRuntimePermission(context: Context, permission: String) {
    if (permission == Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < 33) return
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) return
    (context as? Activity)?.requestPermissions(arrayOf(permission), permission.hashCode() and 0x7fff)
}

private fun formatDurationMs(value: Long): String {
    val safe = value.coerceAtLeast(0L)
    if (safe == 0L) return "ללא המתנה"
    val seconds = safe / 1000f
    val rounded = (seconds * 10f).toInt() / 10f
    return if (rounded == 1f) "שנייה אחת" else rounded.toString() + " שניות"
}

private fun isContextConditionAvailable(
    mode: AppMode,
    type: ContextConditionType,
): Boolean = when (mode) {
    AppMode.FULL -> true
    AppMode.BASIC -> type == ContextConditionType.ANY || type == ContextConditionType.APP
}

private fun isSystemActionAvailable(
    mode: AppMode,
    action: SystemActionPreset,
): Boolean {
    if (mode == AppMode.FULL) return true
    return action.id in setOf(
        SystemActionPreset.MEDIA_PLAY_PAUSE.id,
        SystemActionPreset.MEDIA_NEXT.id,
        SystemActionPreset.MEDIA_PREVIOUS.id,
        SystemActionPreset.VOLUME_UP.id,
        SystemActionPreset.VOLUME_DOWN.id,
        SystemActionPreset.SETTINGS.id,
        SystemActionPreset.DIALER.id,
    )
}

private fun permissionRequirementFor(
    context: Context,
    config: KeyActionConfig,
): Pair<String, () -> Unit>? {
    if (config.triggerType == TriggerType.SCREEN_TAP ||
        config.actionType == ActionType.APP_TAP ||
        (
            config.actionType == ActionType.SYSTEM &&
                config.systemActionId in setOf(
                    SystemActionPreset.HOME.id,
                    SystemActionPreset.BACK.id,
                    SystemActionPreset.RECENTS.id,
                    SystemActionPreset.NOTIFICATIONS.id,
                )
        )
    ) {
        if (!isAccessibilityEnabled(context)) {
            return "הפעולה הזאת דורשת שירות נגישות." to {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    if (config.triggerType == TriggerType.APP_ENTRY &&
        config.contextConditionType in setOf(
            ContextConditionType.APP,
            ContextConditionType.RADIO,
        ) &&
        AdvancedRuleRepository.currentMode(context) == AppMode.BASIC &&
        !hasUsageAccess(context)
    ) {
        return "המצב שבחרת דורש הרשאת נתוני שימוש." to {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    if (config.triggerType == TriggerType.APP_ENTRY &&
        config.contextConditionType == ContextConditionType.RINGING &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return "המצב שבחרת דורש הרשאת מצב טלפון." to {
            requestRuntimePermission(context, Manifest.permission.READ_PHONE_STATE)
        }
    }

    return null
}

@Composable
private fun PermissionRequirementCard(
    message: String,
    onClick: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onClick,
                Modifier.fillMaxWidth(),
            ) {
                Text("מתן הרשאה")
            }
        }
    }
}

private fun validateRuleBeforeSave(
    context: Context,
    config: KeyActionConfig,
    metadata: RuleAdvancedMetadata,
    repo: AdvancedRuleRepository,
): String? {
    val mode = AdvancedRuleRepository.currentMode(context)
    if (config.triggerType == TriggerType.SCREEN_TAP && mode != AppMode.FULL) {
        return "טריגר לחיצה במיקום דורש מצב מלא עם שירות נגישות."
    }

    if (config.triggerType == TriggerType.APP_ENTRY) {
        if ((config.contextConditionType == ContextConditionType.APP ||
                config.contextConditionType == ContextConditionType.RADIO) &&
            config.contextConditionValue.isBlank()
        ) {
            return "צריך לבחור אפליקציה עבור המצב שנבחר."
        }
        if (mode == AppMode.BASIC &&
            config.contextConditionType in setOf(
                ContextConditionType.MUSIC,
                ContextConditionType.MUTED,
                ContextConditionType.RINGING,
                ContextConditionType.RADIO,
            )
        ) {
            return "המצב שבחרת אינו זמין במצב בסיסי ללא נגישות."
        }
        if (mode == AppMode.BASIC &&
            (config.contextConditionType == ContextConditionType.APP ||
                config.contextConditionType == ContextConditionType.RADIO) &&
            !hasUsageAccess(context)
        ) {
            return "המצב שנבחר דורש הרשאת נתוני שימוש."
        }
    }

    if (config.actionType == ActionType.APP && config.targetPackage.isBlank()) {
        return "צריך לבחור אפליקציית יעד."
    }

    if (config.actionType == ActionType.APP_TAP) {
        if (mode != AppMode.FULL) {
            return "פתיחה+לחיצות דורשת מצב מלא עם שירות נגישות."
        }
        if (config.screenTapPackage.isBlank()) {
            return "צריך לבחור אפליקציה וללמוד נקודת לחיצה."
        }
        if (config.screenTapXRatio !in 0f..1f || config.screenTapYRatio !in 0f..1f) {
            return "צריך ללמוד או להגדיר מיקום לחיצה תקין."
        }
    }

    if (config.triggerType == TriggerType.SCREEN_TAP) {
        if (config.screenTapPackage.isBlank()) return "צריך לבחור אפליקציה לזיהוי הלחיצה."
        val meta = repo.getRuleMetadata(config.id)
        val validCurrentOrientation =
            if (context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                meta.landscapeX >= 0f && meta.landscapeY >= 0f
            } else {
                meta.portraitX >= 0f && meta.portraitY >= 0f
            }
        if (!validCurrentOrientation &&
            (config.screenTapXRatio !in 0f..1f || config.screenTapYRatio !in 0f..1f)
        ) {
            return "צריך להגדיר מיקום לחיצה."
        }
    }

    if (repo.profiles().none { it.id == metadata.profileId }) {
        return "צריך לבחור פרופיל קיים."
    }

    return null
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
