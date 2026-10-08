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
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeMute
import androidx.compose.material.icons.outlined.VolumeUp
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
import com.example.clickplus.data.ActionTextFormatter
import com.example.clickplus.data.AppMode
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.ClickPlusProfile
import com.example.clickplus.data.profileActionId
import com.example.clickplus.data.profileIdFromActionId
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
    companion object {
        private const val UI_PROCESS_PID = "clickplus_ui_process_pid"
    }

    private var runtimeRequestActive = false
    private var showPermissionIntro by mutableStateOf(false)
    private var notificationRequestAttempted = false
    private var openAccessibilityAfterNotification = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        runtimeRequestActive = false
        if (openAccessibilityAfterNotification && !isAccessibilityEnabled(this)) {
            openAccessibilityAfterNotification = false
            openAccessibilitySettings()
        }
    }

    private fun openAccessibilitySettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        prefs.edit().putInt(UI_PROCESS_PID, android.os.Process.myPid()).apply()
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

                    if (!isAccessibilityEnabled(this@DashboardActivity)) {
                        val notificationNeeded = Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                this@DashboardActivity,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED &&
                            !notificationRequestAttempted

                        if (notificationNeeded) {
                            openAccessibilityAfterNotification = true
                            notificationRequestAttempted = true
                            runtimeRequestActive = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openAccessibilitySettings()
                        }
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
            .putInt(UI_PROCESS_PID, android.os.Process.myPid())
            .apply()
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        if (prefs.getInt(UI_PROCESS_PID, -1) == android.os.Process.myPid()) {
            prefs.edit().remove(UI_PROCESS_PID).apply()
        }
        super.onDestroy()
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
        PermissionIntroScreen(onEnter = onLaterPermissionSetup)
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
    val tapCountSize by prefs.tapCountSizeFlow.collectAsState(initial = 48)
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
                DashboardRoute.Logs -> LogsScreen(mappings = mappings, onBack = { route = DashboardRoute.Home })
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
                    tapCountSize = tapCountSize,
                    currentMode = mode,
                    themeMode = themeMode,
                    onBack = { route = DashboardRoute.Home },
                    onTimeout = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapTimeout(v) } },
                    onActionDelay = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveActionDelay(v) } },
                    onShowTapCount = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveShowTapCount(v) } },
                    onTapCountX = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapCountX(v) } },
                    onTapCountY = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapCountY(v) } },
                    onTapCountSize = { v -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { prefs.saveTapCountSize(v) } },
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

