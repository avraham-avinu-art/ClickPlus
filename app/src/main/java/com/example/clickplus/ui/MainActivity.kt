package com.example.clickplus.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.HudStyle
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.OperationMode
import com.example.clickplus.data.SystemActionPreset
import com.example.clickplus.data.ThemeOption
import com.example.clickplus.data.actionSummaryHebrew
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefsRepo: AppPreferencesRepository
    private val capturedKeyCode = mutableStateOf<Int?>(null)
    private val keyCaptureActive = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsRepo = AppPreferencesRepository(applicationContext)

        setContent {
            ClickPlusApp(
                prefsRepo = prefsRepo,
                detectedKey = capturedKeyCode.value,
                captureActive = keyCaptureActive.value,
                onBeginCapture = {
                    capturedKeyCode.value = null
                    keyCaptureActive.value = true
                },
                onCancelCapture = {
                    keyCaptureActive.value = false
                }
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCaptureActive.value) {
            capturedKeyCode.value = event.keyCode
            keyCaptureActive.value = false
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun ClickPlusApp(
    prefsRepo: AppPreferencesRepository,
    detectedKey: Int?,
    captureActive: Boolean,
    onBeginCapture: () -> Unit,
    onCancelCapture: () -> Unit
) {
    val themeOption by prefsRepo.themeOptionFlow.collectAsState(initial = ThemeOption.DARK_OLED)
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeOption) {
        ThemeOption.DARK_OLED -> true
        ThemeOption.LIGHT -> false
        ThemeOption.AUTO -> systemDark
    }

    val colors = if (useDark) {
        darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF101010),
            primary = Color(0xFF00E676)
        )
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = colors) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Surface(Modifier.fillMaxSize()) {
                MainScreen(
                    prefsRepo = prefsRepo,
                    detectedKey = detectedKey,
                    captureActive = captureActive,
                    onBeginCapture = onBeginCapture,
                    onCancelCapture = onCancelCapture
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    prefsRepo: AppPreferencesRepository,
    detectedKey: Int?,
    captureActive: Boolean,
    onBeginCapture: () -> Unit,
    onCancelCapture: () -> Unit
) {
    val completed by prefsRepo.onboardingCompletedFlow.collectAsState(initial = false)

    if (!completed) {
        OnboardingWizard(prefsRepo)
    } else {
        MainDashboard(
            prefsRepo = prefsRepo,
            detectedKey = detectedKey,
            captureActive = captureActive,
            onBeginCapture = onBeginCapture,
            onCancelCapture = onCancelCapture
        )
    }
}

@Composable
private fun OnboardingWizard(prefsRepo: AppPreferencesRepository) {
    var step by remember { mutableStateOf(1) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Click+", style = MaterialTheme.typography.headlineLarge)
            Text("מיפוי מקשי חומרה", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(26.dp))

            when (step) {
                1 -> StepBlock(
                    title = "שלב 1 — הרשאת נגישות",
                    body = "אפשר את Click+ תחת הגדרות נגישות כדי ללכוד מקשי חומרה ולבצע פעולות.",
                    button = "פתיחת הגדרות נגישות"
                ) {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }

                2 -> StepBlock(
                    title = "שלב 2 — הרשאת תצוגת HUD",
                    body = "אפשר הצגה מעל אפליקציות כדי לקבל חיווי מיידי על מספר הלחיצות והפעולה.",
                    button = "פתיחת הרשאת HUD"
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                    )
                }

                3 -> {
                    Text("שלב 3 — יצירת מיפוי", fontSize = 21.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("במסך הראשי לחץ על ״הוסף מיפוי חדש״ ולכוד את הכפתור הפיזי בלחיצה אחת.")
                    Spacer(Modifier.height(12.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("הכול מוכן", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("אין צורך להזין KeyCode ידנית.")
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (step > 1) {
                OutlinedButton(onClick = { step-- }) { Text("חזרה") }
            } else {
                Spacer(Modifier.size(1.dp))
            }

            if (step < 3) {
                Button(onClick = { step++ }) { Text("הבא") }
            } else {
                Button(onClick = {
                    scope.launch { prefsRepo.setOnboardingCompleted(true) }
                }) {
                    Text("התחל להשתמש")
                }
            }
        }
    }
}

@Composable
private fun StepBlock(
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit
) {
    Text(title, fontSize = 21.sp)
    Spacer(Modifier.height(14.dp))
    Text(body)
    Spacer(Modifier.height(18.dp))
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp)) {
        Text(button)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainDashboard(
    prefsRepo: AppPreferencesRepository,
    detectedKey: Int?,
    captureActive: Boolean,
    onBeginCapture: () -> Unit,
    onCancelCapture: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mappings by prefsRepo.mappingsFlow.collectAsState(initial = emptyList())
    val bigUi by prefsRepo.carFriendlyUiFlow.collectAsState(initial = true)
    val mode by prefsRepo.operationModeFlow.collectAsState(initial = OperationMode.MODE_A_MULTI_TAP)
    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Click+") },
                actions = {
                    Text("ממשק מוגדל", modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = bigUi,
                        onCheckedChange = { scope.launch { prefsRepo.saveCarFriendlyUi(it) } },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == OperationMode.MODE_A_MULTI_TAP,
                    onClick = {
                        scope.launch { prefsRepo.saveOperationMode(OperationMode.MODE_A_MULTI_TAP) }
                    },
                    label = { Text(OperationMode.MODE_A_MULTI_TAP.titleHebrew) }
                )
                FilterChip(
                    selected = mode == OperationMode.MODE_B_CONFIRMATION,
                    onClick = {
                        scope.launch { prefsRepo.saveOperationMode(OperationMode.MODE_B_CONFIRMATION) }
                    },
                    label = { Text(OperationMode.MODE_B_CONFIRMATION.titleHebrew) }
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { showSettings = true }) { Text("הגדרות") }
            }

            Spacer(Modifier.height(12.dp))
            Text("מיפויים", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (mappings.isEmpty()) {
                    item { EmptyState { showAdd = true } }
                }
                items(mappings, key = { it.id }) { item ->
                    MappingRowItem(
                        item = item,
                        bigUi = bigUi,
                        onTest = { testMapping(context, item) },
                        onToggle = { updated ->
                            scope.launch {
                                prefsRepo.saveMappings(
                                    mappings.map { current ->
                                        if (current.id == updated.id) updated else current
                                    }
                                )
                            }
                        }
                    )
                }
            }

            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().height(if (bigUi) 64.dp else 52.dp)
            ) {
                Text("הוסף מיפוי חדש", fontSize = if (bigUi) 20.sp else 17.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showAdd) {
        AddMappingDialog(
            onDismiss = {
                onCancelCapture()
                showAdd = false
            },
            detectedKey = detectedKey,
            captureActive = captureActive,
            onBeginCapture = onBeginCapture,
            onCancelCapture = onCancelCapture
        ) { config ->
            scope.launch { prefsRepo.saveMappings(mappings + config) }
            showAdd = false
        }
    }

    if (showSettings) {
        SettingsDialog(prefsRepo = prefsRepo, onDismiss = { showSettings = false })
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("עדיין אין מיפויים", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Text("צור מיפוי ראשון לכפתור פיזי.")
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAdd) { Text("צור מיפוי ראשון") }
        }
    }
}

@Composable
private fun MappingRowItem(
    item: KeyActionConfig,
    bigUi: Boolean,
    onTest: () -> Unit,
    onToggle: (KeyActionConfig) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if (bigUi) 20.dp else 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.customLabel.ifBlank { item.keyNameHebrew },
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = if (bigUi) 20.sp else 17.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "לחיצות: ${item.tapCount} • ${actionSummaryHebrew(item)}",
                        fontSize = if (bigUi) 15.sp else 13.sp
                    )
                }
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = { onToggle(item.copy(isEnabled = it)) }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onTest) { Text("בדיקת פעולה") }
                Text("מקש ${item.triggerKeyCode}", modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun testMapping(context: android.content.Context, config: KeyActionConfig) {
    val service = KeyInterceptorAccessibilityService.instance
    if (service != null) {
        service.testMapping(config)
    } else {
        android.widget.Toast.makeText(
            context,
            "שירות Click+ אינו פעיל. הפעל אותו בהגדרות הנגישות.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

@Composable
private fun AddMappingDialog(
    onDismiss: () -> Unit,
    detectedKey: Int?,
    captureActive: Boolean,
    onBeginCapture: () -> Unit,
    onCancelCapture: () -> Unit,
    onSave: (KeyActionConfig) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var keyCode by remember { mutableStateOf<Int?>(null) }
    var keyName by remember { mutableStateOf("לא נלכד מקש עדיין") }
    var taps by remember { mutableStateOf("1") }
    var action by remember { mutableStateOf(ActionType.SYSTEM_KEY) }
    var selectedPreset by remember { mutableStateOf(SystemActionPreset.HOME) }
    var selectedAppPackage by remember { mutableStateOf("") }
    var selectedAppName by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var systemMenuExpanded by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showCaptureDialog by remember { mutableStateOf(false) }

    LaunchedEffect(detectedKey) {
        if (captureActive && detectedKey != null) {
            keyCode = detectedKey
            keyName = "מקש שנלכד: ${KeyEvent.keyCodeToString(detectedKey).removePrefix("KEYCODE_")} (${detectedKey})"
            showCaptureDialog = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            onCancelCapture()
            onDismiss()
        },
        title = { Text("הגדרת מיפוי חדש") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(520.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("שם / תווית לזיהוי") },
                        supportingText = { Text("אופציונלי") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Button(
                        onClick = {
                            showCaptureDialog = true
                            onBeginCapture()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(keyName)
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("מספר לחיצות רצופות")
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = taps,
                            onValueChange = { taps = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { actionMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("סוג פעולה: ${action.titleHebrew}")
                        }
                        DropdownMenu(
                            expanded = actionMenuExpanded,
                            onDismissRequest = { actionMenuExpanded = false }
                        ) {
                            ActionType.entries.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(candidate.titleHebrew) },
                                    onClick = {
                                        action = candidate
                                        actionMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    when (action) {
                        ActionType.LAUNCH_APP -> {
                            OutlinedButton(
                                onClick = { showAppPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (selectedAppName.isBlank()) {
                                        "בחירת אפליקציה"
                                    } else {
                                        "נבחרה: $selectedAppName"
                                    }
                                )
                            }
                        }

                        ActionType.SYSTEM_KEY -> {
                            Box(Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { systemMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("פעולת מערכת: ${selectedPreset.titleHebrew}")
                                }
                                DropdownMenu(
                                    expanded = systemMenuExpanded,
                                    onDismissRequest = { systemMenuExpanded = false }
                                ) {
                                    SystemActionPreset.entries.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(preset.titleHebrew) },
                                            onClick = {
                                                selectedPreset = preset
                                                systemMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        ActionType.CLICK_NODE_BY_ID -> {
                            OutlinedTextField(
                                value = target,
                                onValueChange = { target = it },
                                label = { Text("מזהה רכיב (View ID)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        ActionType.CLICK_NODE_BY_TEXT -> {
                            OutlinedTextField(
                                value = target,
                                onValueChange = { target = it },
                                label = { Text("הטקסט שמופיע על הכפתור") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        ActionType.SEND_INTENT -> {
                            OutlinedTextField(
                                value = target,
                                onValueChange = { target = it },
                                label = { Text("Action של Intent מותאם") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled =
                    keyCode != null &&
                    (action != ActionType.LAUNCH_APP || selectedAppPackage.isNotBlank()) &&
                    (
                        action !in setOf(
                            ActionType.CLICK_NODE_BY_ID,
                            ActionType.CLICK_NODE_BY_TEXT,
                            ActionType.SEND_INTENT
                        ) || target.isNotBlank()
                    ),
                onClick = {
                    val safeKey = keyCode
                    val safeTaps = taps.toIntOrNull()?.coerceIn(1, 20) ?: 1

                    if (safeKey != null) onSave(
                        KeyActionConfig(
                            customLabel = label.trim(),
                            triggerKeyCode = safeKey,
                            keyNameHebrew = keyName,
                            tapCount = safeTaps,
                            actionType = action,
                            targetPackage = selectedAppPackage,
                            targetAppName = selectedAppName,
                            targetClassOrIntent = if (action == ActionType.SEND_INTENT) target.trim() else "",
                            nodeIdentifier = if (
                                action == ActionType.CLICK_NODE_BY_ID ||
                                action == ActionType.CLICK_NODE_BY_TEXT
                            ) target.trim() else "",
                            systemKeyCode = if (action == ActionType.SYSTEM_KEY) {
                                selectedPreset.keyCode
                            } else {
                                KeyEvent.KEYCODE_HOME
                            }
                        )
                    )
                    onCancelCapture()
                }
            ) {
                Text("שמור מיפוי")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onCancelCapture()
                onDismiss()
            }) {
                Text("ביטול")
            }
        }
    )

    if (showCaptureDialog) {
        AlertDialog(
            onDismissRequest = {
                showCaptureDialog = false
                onCancelCapture()
            },
            title = { Text("לחץ על כפתור") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("לחץ עכשיו על הכפתור הפיזי שברצונך למפות.")
                    Text(
                        if (captureActive) "ממתין ללחיצה…" else "הקליטה הסתיימה.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showCaptureDialog = false
                    onCancelCapture()
                }) {
                    Text("ביטול")
                }
            }
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onSelect = { packageName, appName ->
                selectedAppPackage = packageName
                selectedAppName = appName
                showAppPicker = false
            }
        )
    }
}

data class AppInfoItem(
    val label: String,
    val packageName: String,
    val icon: Bitmap?
)

@Composable
private fun AppPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val apps = remember {
        pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            .asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                AppInfoItem(
                    label = app.loadLabel(pm).toString(),
                    packageName = app.packageName,
                    icon = runCatching { app.loadIcon(pm).toBitmap(48, 48) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בחר אפליקציה לפתיחה") },
        text = {
            if (apps.isEmpty()) {
                Text("לא נמצאו אפליקציות שניתנות להפעלה.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app.packageName, app.label) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon.asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(46.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            Column {
                                Text(app.label, style = MaterialTheme.typography.titleMedium)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("סגור") }
        }
    )
}

@Composable
private fun SettingsDialog(
    prefsRepo: AppPreferencesRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val storedTimeout by prefsRepo.tapTimeoutFlow.collectAsState(initial = 450L)
    val storedDebounce by prefsRepo.debounceMsFlow.collectAsState(initial = 80L)
    val storedHud by prefsRepo.hudStyleFlow.collectAsState(initial = HudStyle.SHORT_TEXT)
    val storedTheme by prefsRepo.themeOptionFlow.collectAsState(initial = ThemeOption.DARK_OLED)

    var tapTimeout by remember(storedTimeout) { mutableStateOf(storedTimeout.toString()) }
    var debounce by remember(storedDebounce) { mutableStateOf(storedDebounce.toString()) }
    var selectedHud by remember(storedHud) { mutableStateOf(storedHud) }
    var selectedTheme by remember(storedTheme) { mutableStateOf(storedTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הגדרות מתקדמות") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(430.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = tapTimeout,
                        onValueChange = { tapTimeout = it.filter(Char::isDigit) },
                        label = { Text("זמן חלון לריבוי לחיצות (מילישניות)") },
                        supportingText = { Text("ברירת מחדל: 450") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = debounce,
                        onValueChange = { debounce = it.filter(Char::isDigit) },
                        label = { Text("סינון רעשים (מילישניות)") },
                        supportingText = { Text("ברירת מחדל: 80") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("סגנון HUD", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudStyle.entries.forEach { style ->
                            FilterChip(
                                selected = selectedHud == style,
                                onClick = { selectedHud = style },
                                label = { Text(style.titleHebrew) }
                            )
                        }
                    }
                }
                item {
                    Text("מראה", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeOption.entries.forEach { option ->
                            FilterChip(
                                selected = selectedTheme == option,
                                onClick = { selectedTheme = option },
                                label = { Text(option.titleHebrew) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    prefsRepo.saveTapTimeout(tapTimeout.toLongOrNull() ?: 450L)
                    prefsRepo.saveDebounce(debounce.toLongOrNull() ?: 80L)
                    prefsRepo.saveHudStyle(selectedHud)
                    prefsRepo.saveThemeOption(selectedTheme)
                }
                onDismiss()
            }) {
                Text("שמור")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}
