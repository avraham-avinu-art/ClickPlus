@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.clickplus.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.clickplus.data.TriggerType
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import kotlinx.coroutines.delay
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
    val timeout by prefs.tapTimeoutFlow.collectAsState(initial = 650L)
    val showTapCount by prefs.showTapCountFlow.collectAsState(initial = false)

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            mappings = mappings,
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
            timeout = timeout,
            showTapCount = showTapCount,
            onBack = { screen = Screen.Home },
            onTimeout = { scope.launch { prefs.saveTapTimeout(it) } },
            onShowTapCount = { scope.launch { prefs.saveShowTapCount(it) } },
            onAccessibility = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
                        Text("פעולות לפי כניסות ולחיצות", style = MaterialTheme.typography.labelSmall)
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 16.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "קליק פלוס פעילה תמיד",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("הפעילות נשארת זמינה ברקע. אין מצב כבוי מתוך האפליקציה.")
                        Text(
                            if (serviceEnabled) {
                                "שירות הנגישות פעיל והאפליקציה מוכנה."
                            } else {
                                "יש להפעיל את שירות הנגישות כדי שהפעולות יעבדו תמיד."
                            },
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
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("עדיין אין פעולות", style = MaterialTheme.typography.titleMedium)
                            Text("הוסף פעולה ובחר כניסות או מיקום לחיצה.")
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
    OutlinedCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
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
            Text(
                "הפעלה: " + if (item.triggerType == TriggerType.SCREEN_TAP) {
                    "לחיצה לפי מיקום"
                } else {
                    "כניסה לאפליקציה"
                },
            )
            Text("מצב: " + item.contextSummary())
        }
    }
}

@Composable
private fun SettingsScreen(
    timeout: Long,
    showTapCount: Boolean,
    onBack: () -> Unit,
    onTimeout: (Long) -> Unit,
    onShowTapCount: (Boolean) -> Unit,
    onAccessibility: () -> Unit,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 16.dp,
                bottom = 32.dp,
            ),
        ) {
            item {
                SettingsCard {
                    Text(
                        "הפעלה קבועה",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("קליק פלוס מוגדרת לפעול תמיד ברקע. האפשרות אינה ניתנת לכיבוי מתוך האפליקציה.")
                    Spacer(Modifier.height(6.dp))
                    Text("כדי לעצור את הפעילות יש להשבית את שירות הנגישות של קליק פלוס.")
                }
            }

            item {
                SettingsCard {
                    Text(
                        "זמן לספירת כניסות",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("$timeout אלפיות השנייה")
                    Slider(
                        value = timeout.toFloat(),
                        onValueChange = { onTimeout(it.toLong()) },
                        valueRange = 300f..1200f,
                        steps = 8,
                    )
                }
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "חיווי מספר הלחיצות",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("מציג על המסך את מספר הכניסות או הלחיצות שנספרו.")
                        }
                        Switch(
                            checked = showTapCount,
                            onCheckedChange = onShowTapCount,
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    Text(
                        "שירות נגישות",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("השירות נדרש לזיהוי האפליקציה הקדמית, זיהוי לחיצות ולביצוע הפעולות.")
                    Button(
                        onClick = onAccessibility,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("פתיחת הגדרות נגישות")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun EditorSection(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun NumberChoice(
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                count.toString(),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun EditorScreen(
    existing: KeyActionConfig?,
    onBack: () -> Unit,
    onSave: (KeyActionConfig) -> Unit,
    onDelete: (KeyActionConfig) -> Unit,
) {
    val context = LocalContext.current

    var triggerType by remember(existing?.id) {
        mutableStateOf(existing?.triggerType ?: TriggerType.APP_ENTRY)
    }
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var pressCount by remember(existing?.id) { mutableIntStateOf(existing?.pressCount ?: 1) }
    var actionType by remember(existing?.id) {
        mutableStateOf(existing?.actionType ?: ActionType.SYSTEM)
    }
    var systemAction by remember(existing?.id) {
        mutableStateOf(existing?.systemActionId ?: SystemActionPreset.MEDIA_PLAY_PAUSE.id)
    }
    var packageName by remember(existing?.id) { mutableStateOf(existing?.targetPackage.orEmpty()) }
    var appName by remember(existing?.id) { mutableStateOf(existing?.targetAppName.orEmpty()) }
    var contextType by remember(existing?.id) {
        mutableStateOf(existing?.contextConditionType ?: ContextConditionType.ANY)
    }
    var contextValue by remember(existing?.id) {
        mutableStateOf(existing?.contextConditionValue.orEmpty())
    }
    var contextName by remember(existing?.id) {
        mutableStateOf(existing?.contextConditionName.orEmpty())
    }
    var screenTapPackage by remember(existing?.id) {
        mutableStateOf(existing?.screenTapPackage.orEmpty())
    }
    var screenTapAppName by remember(existing?.id) {
        mutableStateOf(existing?.screenTapAppName.orEmpty())
    }
    var screenTapXRatio by remember(existing?.id) {
        mutableFloatStateOf(existing?.screenTapXRatio ?: -1f)
    }
    var screenTapYRatio by remember(existing?.id) {
        mutableFloatStateOf(existing?.screenTapYRatio ?: -1f)
    }
    var screenTapToleranceRatio by remember(existing?.id) {
        mutableFloatStateOf(existing?.screenTapToleranceRatio ?: 0.08f)
    }
    var showApps by remember { mutableStateOf(false) }
    var capturePending by remember { mutableStateOf(false) }

    val serviceEnabled = isServiceEnabled(context)
    val phonePermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(capturePending) {
        if (!capturePending) return@LaunchedEffect
        while (capturePending) {
            val runtime = context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            if (runtime.getBoolean("tap_capture_ready", false)) {
                screenTapXRatio = runtime.getFloat("tap_capture_x_ratio", -1f)
                screenTapYRatio = runtime.getFloat("tap_capture_y_ratio", -1f)
                screenTapPackage = runtime.getString("tap_capture_package", screenTapPackage).orEmpty()
                screenTapAppName = runtime.getString("tap_capture_app_name", screenTapAppName).orEmpty()
                runtime.edit().putBoolean("tap_capture_ready", false).apply()
                capturePending = false
                break
            }
            delay(250L)
        }
    }

    val actionValid = actionType == ActionType.SYSTEM || packageName.isNotBlank()
    val conditionValid = when (triggerType) {
        TriggerType.SCREEN_TAP ->
            screenTapPackage.isNotBlank() &&
                screenTapXRatio >= 0f &&
                screenTapYRatio >= 0f

        TriggerType.APP_ENTRY -> when (contextType) {
            ContextConditionType.APP, ContextConditionType.RADIO -> contextValue.isNotBlank()
            ContextConditionType.RINGING -> phonePermission
            else -> true
        }
    }

    val startTapLearning = {
        if (serviceEnabled && screenTapPackage.isNotBlank()) {
            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("tap_capture_ready", false)
                .putBoolean("tap_learning", true)
                .putString("tap_learning_package", screenTapPackage)
                .apply()
            capturePending = true
            context.packageManager.getLaunchIntentForPackage(screenTapPackage)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 16.dp,
                bottom = 32.dp,
            ),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("שם הפעולה (לא חובה)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                EditorSection {
                    Text(
                        "סוג ההפעלה",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TriggerType.entries.forEach { option ->
                            FilterChip(
                                selected = triggerType == option,
                                onClick = {
                                    triggerType = option
                                    if (option == TriggerType.SCREEN_TAP) {
                                        contextType = ContextConditionType.ANY
                                    }
                                },
                                label = { Text(option.titleHebrew) },
                            )
                        }
                    }

                    Text(
                        "כמה כניסות / לחיצות רצופות?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (1..10).forEach { count ->
                            NumberChoice(
                                selected = pressCount == count,
                                count = count,
                                onClick = { pressCount = count },
                            )
                        }
                    }
                    Text(
                        if (triggerType == TriggerType.APP_ENTRY) {
                            "כל כניסה רצופה לאפליקציה נספרת פעם אחת."
                        } else {
                            "הלחיצות על המיקום שנלמד נספרות ברצף בתוך זמן הספירה."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                EditorSection {
                    Text(
                        "מה לבצע?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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

            item {
                EditorSection {
                    if (triggerType == TriggerType.APP_ENTRY) {
                        Text(
                            "מצב",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ContextConditionType.entries.forEach { option ->
                                FilterChip(
                                    selected = contextType == option,
                                    onClick = {
                                        contextType = option
                                        if (
                                            option != ContextConditionType.APP &&
                                            option != ContextConditionType.RADIO
                                        ) {
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
                                OutlinedButton(
                                    onClick = { showApps = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (contextName.isBlank()) "בחר אפליקציה" else contextName)
                                }
                                Text(
                                    "הבדיקה תתבצע מול האפליקציה שהייתה פתוחה לפני הפעלת ClickPlus.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            ContextConditionType.RADIO -> {
                                OutlinedButton(
                                    onClick = { showApps = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (contextName.isBlank()) "בחר אפליקציית רדיו" else contextName,
                                    )
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
                                Text("הפעולה תשמש כברירת מחדל בכל מצב.")
                        }
                    } else {
                        Text(
                            "מיקום לחיצה בתוך אפליקציה אחרת",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "בחר אפליקציה, לחץ על \"למד מיקום\", ואז בצע לחיצה על הכפתור או האזור הרצוי. ClickPlus תשמור את מיקום הרכיב ותוכל להפעיל ממנו את הפעולה.",
                        )

                        OutlinedButton(
                            onClick = { showApps = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (screenTapAppName.isBlank()) {
                                    "בחר אפליקציה שבה הלחיצה תזוהה"
                                } else {
                                    screenTapAppName
                                },
                            )
                        }

                        if (!serviceEnabled) {
                            Text(
                                "כדי ללמוד ולזהות לחיצות יש להפעיל קודם את שירות הנגישות.",
                                fontWeight = FontWeight.Bold,
                            )
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("הפעלת שירות נגישות")
                            }
                        }

                        Button(
                            onClick = startTapLearning,
                            enabled = serviceEnabled && screenTapPackage.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (capturePending) "ממתין ללחיצה..." else "למד מיקום לחיצה")
                        }

                        if (screenTapXRatio >= 0f && screenTapYRatio >= 0f) {
                            Text(
                                "מיקום שנלמד: X ${(screenTapXRatio * 100f).toInt()}% · Y ${(screenTapYRatio * 100f).toInt()}%",
                                fontWeight = FontWeight.Bold,
                            )
                            Text("סטייה מותרת: ${(screenTapToleranceRatio * 100f).toInt()}%")
                            Slider(
                                value = screenTapToleranceRatio,
                                onValueChange = {
                                    screenTapToleranceRatio = it.coerceIn(0.01f, 0.25f)
                                },
                                valueRange = 0.01f..0.25f,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                    triggerType = triggerType,
                                    screenTapPackage = screenTapPackage,
                                    screenTapAppName = screenTapAppName,
                                    screenTapXRatio = screenTapXRatio,
                                    screenTapYRatio = screenTapYRatio,
                                    screenTapToleranceRatio = screenTapToleranceRatio,
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
    }

    if (showApps) {
        AppPickerDialog(
            onDismiss = { showApps = false },
            onSelect = { pkg, label ->
                when {
                    triggerType == TriggerType.SCREEN_TAP -> {
                        screenTapPackage = pkg
                        screenTapAppName = label
                    }

                    contextType == ContextConditionType.APP ||
                        contextType == ContextConditionType.RADIO -> {
                        contextValue = pkg
                        contextName = label
                    }

                    else -> {
                        packageName = pkg
                        appName = label
                    }
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
