@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.clickplus.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppMode
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.TriggerType
import com.example.clickplus.service.BasicForegroundMonitorService
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

    private var showPermissionIntro by mutableStateOf(false)

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
                onResetAll = { showPermissionIntro = true },
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
    onResetAll: () -> Unit,
    onLaterPermissionSetup: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var lifecycleRefresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) lifecycleRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mode, mappings, lifecycleRefresh) {
        val needsBasicMonitor = mode == AppMode.BASIC &&
            hasUsageAccess(context) &&
            mappings.any { it.enabled && it.triggerType == TriggerType.APP_ENTRY }
        val monitorIntent = Intent(context, BasicForegroundMonitorService::class.java)
        if (needsBasicMonitor) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, monitorIntent)
                } else {
                    context.startService(monitorIntent)
                }
            }.onFailure {
                android.util.Log.w("ClickPlus", "Could not start Basic mode app-entry monitor", it)
            }
        } else {
            context.stopService(monitorIntent)
        }
    }

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
                        scope.launch {
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
                    tapCountSize = tapCountSize,
                    actionDelay = actionDelay,
                    onBack = { route = DashboardRoute.Settings },
                    onResetAll = {
                        mode = AppMode.FULL
                        themeMode = "system"
                        onResetAll()
                    },
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

