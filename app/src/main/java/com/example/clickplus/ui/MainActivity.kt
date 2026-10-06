@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.clickplus.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Editor(val id: String?) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = AppPreferencesRepository(applicationContext)

        setContent {
            MaterialTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    ClickPlusScreen(prefs)
                }
            }
        }
    }
}

@Composable
private fun ClickPlusScreen(prefs: AppPreferencesRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mappings by prefs.mappingsFlow.collectAsState(initial = emptyList())
    val backgroundOnly by prefs.backgroundOnlyFlow.collectAsState(initial = false)
    val timeout by prefs.tapTimeoutFlow.collectAsState(initial = 650L)

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            mappings = mappings,
            backgroundOnly = backgroundOnly,
            serviceEnabled = isServiceEnabled(context),
            onAdd = { screen = Screen.Editor(null) },
            onEdit = { screen = Screen.Editor(it) },
            onSettings = { screen = Screen.Settings },
            onToggle = { item, enabled ->
                scope.launch {
                    prefs.saveMappings(
                        mappings.map { if (it.id == item.id) it.copy(enabled = enabled) else it },
                    )
                }
            },
        )

        Screen.Settings -> SettingsScreen(
            backgroundOnly = backgroundOnly,
            timeout = timeout,
            onBack = { screen = Screen.Home },
            onBackgroundOnly = {
                scope.launch { prefs.saveBackgroundOnly(it) }
            },
            onTimeout = {
                scope.launch { prefs.saveTapTimeout(it) }
            },
            onAccessibility = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onAppSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.packageName),
                    ),
                )
            },
        )

        is Screen.Editor -> EditorScreen(
            existing = mappings.firstOrNull { it.id == current.id },
            onBack = { screen = Screen.Home },
            onSave = { item ->
                scope.launch {
                    val next = if (mappings.any { it.id == item.id }) {
                        mappings.map { if (it.id == item.id) item else it }
                    } else {
                        mappings + item
                    }
                    prefs.saveMappings(next)
                    screen = Screen.Home
                }
            },
            onDelete = { item ->
                scope.launch {
                    prefs.saveMappings(mappings.filterNot { it.id == item.id })
                    screen = Screen.Home
                }
            },
        )
    }
}

@Composable
private fun HomeScreen(
    mappings: List<KeyActionConfig>,
    backgroundOnly: Boolean,
    serviceEnabled: Boolean,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onSettings: () -> Unit,
    onToggle: (KeyActionConfig, Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("קליק פלוס", fontWeight = FontWeight.Bold)
                        Text("פעולות לפי כניסות", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "הגדרות")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text("הוספת פעולה")
                }
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text("הגדרות")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "הפעלה לפי כניסה לאפליקציה",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (backgroundOnly) {
                                "עבודה ברקע פעילה. כל כניסה לאפליקציה נספרת כלחיצה."
                            } else {
                                "במצב רגיל האפליקציה נפתחת. בהפעלת עבודה ברקע, הכניסה לאפליקציה תשמש כטריגר."
                            },
                        )
                        Text(
                            if (serviceEnabled) "שירות הנגישות פעיל." else "שירות הנגישות עדיין לא פעיל.",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item {
                Text(
                    "הפעולות שלי (" + mappings.size + ")",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (mappings.isEmpty()) {
                item {
                    OutlinedCard {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(42.dp))
                            Text("עדיין אין פעולות", style = MaterialTheme.typography.titleMedium)
                            Text("הוסף פעולה ובחר כמה כניסות רצופות יפעילו אותה.")
                        }
                    }
                }
            } else {
                items(mappings, key = { it.id }) { item ->
                    ActionCard(
                        item = item,
                        onEdit = { onEdit(item.id) },
                        onToggle = { onToggle(item, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    item: KeyActionConfig,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    OutlinedCard(onClick = onEdit) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name.ifBlank { item.actionSummary() },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(item.pressSummary())
                }
                Switch(checked = item.enabled, onCheckedChange = onToggle)
            }
            Text("פעולה: " + item.actionSummary())
            Text("מצב: " + item.contextSummary())
        }
    }
}

@Composable
private fun SettingsScreen(
    backgroundOnly: Boolean,
    timeout: Long,
    onBack: () -> Unit,
    onBackgroundOnly: (Boolean) -> Unit,
    onTimeout: (Long) -> Unit,
    onAccessibility: () -> Unit,
    onAppSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הגדרות") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "חזרה")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("עבודה ברקע", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("עבודה שקטה ברקע", fontWeight = FontWeight.Bold)
                                Text("כשפעיל, כניסה לאפליקציה היא אות הפעלה ולא פתיחת מסך.")
                            }
                            Switch(checked = backgroundOnly, onCheckedChange = onBackgroundOnly)
                        }
                        Text("זמן לספירת כניסות: " + timeout + " אלפיות השנייה")
                        Slider(
                            value = timeout.toFloat(),
                            onValueChange = { onTimeout(it.toLong()) },
                            valueRange = 300f..1200f,
                            steps = 8,
                        )
                    }
                }
            }

            item {
                OutlinedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("שירות נגישות", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("השירות חייב להיות פעיל כדי לבצע את הפעולות ולזהות את כניסת האפליקציה.")
                        Button(onClick = onAccessibility, modifier = Modifier.fillMaxWidth()) {
                            Text("פתיחת הגדרות נגישות")
                        }
                    }
                }
            }

            item {
                OutlinedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("הגדרות נוספות", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = onAppSettings, modifier = Modifier.fillMaxWidth()) {
                            Text("הגדרות נוספות באפליקציה")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditorScreen(
    existing: KeyActionConfig?,
    onBack: () -> Unit,
    onSave: (KeyActionConfig) -> Unit,
    onDelete: (KeyActionConfig) -> Unit,
) {
    val context = LocalContext.current

    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var pressCount by remember(existing?.id) { mutableIntStateOf(existing?.pressCount ?: 1) }
    var actionType by remember(existing?.id) { mutableStateOf(existing?.actionType ?: ActionType.SYSTEM) }
    var systemAction by remember(existing?.id) {
        mutableStateOf(existing?.systemActionId ?: SystemActionPreset.MEDIA_PLAY_PAUSE.id)
    }
    var packageName by remember(existing?.id) { mutableStateOf(existing?.targetPackage.orEmpty()) }
    var appName by remember(existing?.id) { mutableStateOf(existing?.targetAppName.orEmpty()) }
    var contextType by remember(existing?.id) {
        mutableStateOf(existing?.contextConditionType ?: ContextConditionType.ANY)
    }
    var contextValue by remember(existing?.id) { mutableStateOf(existing?.contextConditionValue.orEmpty()) }
    var contextName by remember(existing?.id) { mutableStateOf(existing?.contextConditionName.orEmpty()) }
    var showApps by remember { mutableStateOf(false) }

    val phonePermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    val actionValid = actionType == ActionType.SYSTEM || packageName.isNotBlank()
    val conditionValid = when (contextType) {
        ContextConditionType.APP, ContextConditionType.RADIO -> contextValue.isNotBlank()
        ContextConditionType.RINGING -> phonePermission
        else -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "פעולה חדשה" else "עריכת פעולה") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "חזרה")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("שם הפעולה (לא חובה)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("כמה כניסות?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..10).forEach { count ->
                            FilterChip(
                                selected = pressCount == count,
                                onClick = { pressCount = count },
                                label = { Text(count.toString()) },
                            )
                        }
                    }
                    Text("כל כניסה רצופה של המולטימדיה לאפליקציה נספרת פעם אחת.")
                }
            }

            OutlinedCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("מה לבצע?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = actionType == ActionType.SYSTEM,
                            onClick = { actionType = ActionType.SYSTEM },
                            label = { Text("פעולת מערכת") },
                        )
                        FilterChip(
                            selected = actionType == ActionType.APP,
                            onClick = { actionType = ActionType.APP },
                            label = { Text("פתיחת אפליקציה") },
                        )
                    }

                    if (actionType == ActionType.SYSTEM) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SystemActionPreset.entries.forEach { option ->
                                FilterChip(
                                    selected = systemAction == option.id,
                                    onClick = { systemAction = option.id },
                                    label = { Text(option.titleHebrew) },
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showApps = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (appName.isBlank()) "בחר אפליקציה" else appName)
                        }
                    }
                }
            }

            OutlinedCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("מצב", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ContextConditionType.entries.forEach { option ->
                            FilterChip(
                                selected = contextType == option,
                                onClick = {
                                    contextType = option
                                    if (option != ContextConditionType.APP && option != ContextConditionType.RADIO) {
                                        contextValue = ""
                                        contextName = ""
                                    }
                                },
                                label = { Text(option.titleHebrew) },
                            )
                        }
                    }

                    when (contextType) {
                        ContextConditionType.APP -> {
                            OutlinedButton(onClick = { showApps = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (contextName.isBlank()) "בחר אפליקציה" else contextName)
                            }
                        }

                        ContextConditionType.RADIO -> {
                            OutlinedButton(onClick = { showApps = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (contextName.isBlank()) "בחר אפליקציית רדיו" else contextName)
                            }
                        }

                        ContextConditionType.RINGING -> {
                            if (phonePermission) {
                                Text("זיהוי צלצול פעיל.")
                            } else {
                                Text("נדרשת הרשאה לזיהוי צלצול.")
                                Button(
                                    onClick = {
                                        (context as? Activity)?.requestPermissions(
                                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                                            4201,
                                        )
                                    },
                                ) {
                                    Text("אפשר זיהוי צלצול")
                                }
                            }
                        }

                        ContextConditionType.MUSIC ->
                            Text("הפעולה תפעל כשהמכשיר מנגן מוזיקה.")

                        ContextConditionType.MUTED ->
                            Text("הפעולה תפעל כשהשמע מושתק.")

                        ContextConditionType.ANY ->
                            Text("הפעולה תשמש כברירת מחדל.")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (existing != null) {
                    OutlinedButton(
                        onClick = { onDelete(existing) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("מחיקה")
                    }
                }

                Button(
                    enabled = actionValid && conditionValid,
                    onClick = {
                        onSave(
                            KeyActionConfig(
                                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                pressCount = pressCount,
                                actionType = actionType,
                                systemActionId = systemAction,
                                targetPackage = packageName,
                                targetAppName = appName,
                                contextConditionType = contextType,
                                contextConditionValue = contextValue,
                                contextConditionName = contextName,
                                enabled = existing?.enabled ?: true,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("שמור")
                }
            }
        }
    }

    if (showApps) {
        AppPickerDialog(
            onDismiss = { showApps = false },
            onSelect = { pkg, label ->
                if (contextType == ContextConditionType.APP || contextType == ContextConditionType.RADIO) {
                    contextValue = pkg
                    contextName = label
                } else {
                    packageName = pkg
                    appName = label
                }
                showApps = false
            },
        )
    }
}

private data class InstalledApp(val packageName: String, val label: String)

@Composable
private fun AppPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val apps = remember {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    label = it.loadLabel(context.packageManager).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בחר אפליקציה") },
        text = {
            LazyColumn(
                Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    TextButton(
                        onClick = { onSelect(app.packageName, app.label) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(app.label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("סגור") }
        },
    )
}

private fun isServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    return manager.getEnabledAccessibilityServiceList(Int.MAX_VALUE).any {
        val info = it.resolveInfo?.serviceInfo
        info?.packageName == context.packageName &&
            info.name == KeyInterceptorAccessibilityService::class.java.name
    }
}
