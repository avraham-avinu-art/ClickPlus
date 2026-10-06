package com.example.clickplus.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.HudStyle
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset
import com.example.clickplus.data.ThemeOption
import com.example.clickplus.data.actionSummaryHebrew
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

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
    val dark = when (themeOption) {
        ThemeOption.DARK_OLED, ThemeOption.DARK -> true
        ThemeOption.LIGHT -> false
        ThemeOption.AUTO -> isSystemInDarkTheme()
    }

    val colors = if (dark) {
        darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF17161B),
            surfaceVariant = Color(0xFF242129),
            primary = Color(0xFF00E676),
            onPrimary = Color(0xFF001B0D),
            secondary = Color(0xFFB9AFCB)
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
                    prefsRepo,
                    detectedKey,
                    captureActive,
                    onBeginCapture,
                    onCancelCapture
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

    if (completed) {
        MainDashboard(
            prefsRepo,
            detectedKey,
            captureActive,
            onBeginCapture,
            onCancelCapture
        )
    } else {
        OnboardingWizard(prefsRepo)
    }
}

@Composable
private fun OnboardingWizard(prefsRepo: AppPreferencesRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("+Click", fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text("מיפוי מקשי חומרה", fontSize = 22.sp)
            Spacer(Modifier.height(26.dp))
            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            when (step) {
                1 -> OnboardingStep(
                    title = "שלב 1 — הרשאת נגישות",
                    body = "אפשר את Click+ בהגדרות הנגישות. כך האפליקציה תזהה לחיצות על מקשי חומרה גם בתוך אפליקציות אחרות.",
                    button = "פתיחת הגדרות נגישות",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                2 -> OnboardingStep(
                    title = "שלב 2 — חיווי צף",
                    body = "אפשר הצגה מעל אפליקציות כדי לקבל חיווי קצר על הלחיצה והפעולה שבוצעה.",
                    button = "פתיחת הרשאת חיווי",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = android.net.Uri.parse("package:" + context.packageName)
                            }
                        )
                    }
                )
                3 -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("הכול מוכן", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "כעת תוכל להוסיף מיפוי חדש, ללחוץ על כפתור פיזי ולבחור פעולה מוכנה.",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                OutlinedButton(onClick = { step-- }) { Text("חזרה") }
            } else {
                Spacer(Modifier.width(1.dp))
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
private fun OnboardingStep(
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(body, fontSize = 18.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                Text(button)
            }
        }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mappings by prefsRepo.mappingsFlow.collectAsState(initial = emptyList())
    val bigUi by prefsRepo.carFriendlyUiFlow.collectAsState(initial = true)
    var serviceEnabled by remember { mutableStateOf(isServiceEnabled(context)) }

    var showSettings by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showBackupRestore by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<KeyActionConfig?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var quickPress by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            serviceEnabled = isServiceEnabled(context)
            delay(1000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("+Click • מיפוי מקשים", maxLines = 1) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Text("⚙", fontSize = 24.sp)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Text("⋮", fontSize = 30.sp)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("אודות") },
                                onClick = {
                                    showMenu = false
                                    showAbout = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("גיבוי ושחזור") },
                                onClick = {
                                    showMenu = false
                                    showBackupRestore = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("עזרה") },
                                onClick = {
                                    showMenu = false
                                    showHelp = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            ServiceStatusCard(
                enabled = serviceEnabled,
                onOpenSettings = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            Spacer(Modifier.height(14.dp))

            Text("ברירת מחדל למיפוי חדש", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                FilterChip(
                    selected = quickPress == 1,
                    onClick = { quickPress = 1 },
                    label = { Text("לחיצה בודדת") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = quickPress == 2,
                    onClick = { quickPress = 2 },
                    label = { Text("לחיצה כפולה") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = quickPress == 0,
                    onClick = { quickPress = 0 },
                    label = { Text("לחיצה ארוכה") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("מיפויים", style = MaterialTheme.typography.headlineSmall)
                Text(mappings.size.toString() + " מוגדרים")
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mappings.isEmpty()) {
                    item { EmptyStateCard { showAdd = true } }
                } else {
                    items(mappings, key = { it.id }) { item ->
                        MappingCard(
                            item = item,
                            bigUi = bigUi,
                            onClick = { editing = item },
                            onToggle = { enabled ->
                                scope.launch {
                                    prefsRepo.saveMappings(
                                        mappings.map {
                                            if (it.id == item.id) it.copy(isEnabled = enabled) else it
                                        }
                                    )
                                }
                            },
                            onTest = { testMapping(context, item) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().height(if (bigUi) 66.dp else 54.dp)
            ) {
                Text("+ הוסף מיפוי חדש", fontSize = if (bigUi) 21.sp else 18.sp)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAdd || editing != null) {
        MappingDialog(
            initial = editing,
            defaultPressCount = quickPress,
            detectedKey = detectedKey,
            captureActive = captureActive,
            onBeginCapture = onBeginCapture,
            onCancelCapture = onCancelCapture,
            onDismiss = {
                onCancelCapture()
                showAdd = false
                editing = null
            },
            onSave = { config ->
                scope.launch {
                    val updated = if (editing != null) {
                        mappings.map { if (it.id == config.id) config else it }
                    } else {
                        mappings + config
                    }
                    prefsRepo.saveMappings(updated)
                }
                onCancelCapture()
                showAdd = false
                editing = null
            },
            onDelete = { config ->
                scope.launch {
                    prefsRepo.saveMappings(mappings.filterNot { it.id == config.id })
                }
                showAdd = false
                editing = null
            }
        )
    }

    if (showSettings) SettingsDialog(prefsRepo) { showSettings = false }

    if (showAbout) {
        InfoDialog(
            title = "אודות Click+",
            body = "כלי למיפוי מקשי חומרה. הקצה לכפתורים פיזיים פתיחת אפליקציות, פעולות מערכת, שליטת מדיה ולחיצה על רכיבים במסך.",
            onDismiss = { showAbout = false }
        )
    }

    if (showHelp) {
        InfoDialog(
            title = "עזרה",
            body = "לחץ על ״הוסף מיפוי חדש״. לכוד מקש פיזי, בחר סוג לחיצה ובחר פעולה מוכנה. אפשר לערוך מיפוי קיים בלחיצה עליו.",
            onDismiss = { showHelp = false }
        )
    }

    if (showBackupRestore) {
        BackupRestoreDialog(mappings, prefsRepo) { showBackupRestore = false }
    }
}

@Composable
private fun ServiceStatusCard(enabled: Boolean, onOpenSettings: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (enabled) "●" else "○",
                color = if (enabled) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
                fontSize = 28.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (enabled) "שירות הנגישות: פעיל" else "שירות הנגישות: מופסק",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (enabled) "Click+ מוכן לקליטת מקשי חומרה."
                    else "הפעל את השירות כדי שהמיפויים יעבדו."
                )
            }
            if (!enabled) OutlinedButton(onClick = onOpenSettings) { Text("הפעלה") }
        }
    }
}

@Composable
private fun EmptyStateCard(onAdd: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("טרם הוגדרו מקשים", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("הוסף מיפוי אחד כדי להתחיל.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd) { Text("+ הוסף מיפוי חדש") }
        }
    }
}

@Composable
private fun MappingCard(
    item: KeyActionConfig,
    bigUi: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(if (bigUi) 18.dp else 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.customLabel.ifBlank { item.keyNameHebrew },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(item.pressTypeHebrew() + "  •  " + actionSummaryHebrew(item))
                }
                Switch(checked = item.isEnabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("מקש " + item.triggerKeyCode, fontSize = 13.sp)
                OutlinedButton(onClick = onTest) { Text("בדיקה") }
            }
        }
    }
}

@Composable
private fun MappingDialog(
    initial: KeyActionConfig?,
    defaultPressCount: Int,
    detectedKey: Int?,
    captureActive: Boolean,
    onBeginCapture: () -> Unit,
    onCancelCapture: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (KeyActionConfig) -> Unit,
    onDelete: (KeyActionConfig) -> Unit
) {
    val context = LocalContext.current
    val existing = initial

    var step by remember { mutableStateOf(1) }
    var label by remember { mutableStateOf(existing?.customLabel.orEmpty()) }
    var keyCode by remember { mutableStateOf(existing?.triggerKeyCode) }
    var keyName by remember { mutableStateOf(existing?.keyNameHebrew ?: "לחץ כדי לזהות מקש") }
    var pressCount by remember { mutableStateOf(existing?.tapCount ?: defaultPressCount) }
    var actionType by remember { mutableStateOf(existing?.actionType ?: ActionType.SYSTEM_KEY) }
    var systemAction by remember {
        mutableStateOf(
            SystemActionPreset.entries.firstOrNull { it.id == existing?.systemActionId }
                ?: SystemActionPreset.HOME
        )
    }
    var selectedAppPackage by remember { mutableStateOf(existing?.targetPackage.orEmpty()) }
    var selectedAppName by remember { mutableStateOf(existing?.targetAppName.orEmpty()) }
    var nodeTarget by remember { mutableStateOf(existing?.nodeIdentifier.orEmpty()) }
    var customIntent by remember { mutableStateOf(existing?.targetClassOrIntent.orEmpty()) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showSystemMenu by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }
    var sampling by remember { mutableStateOf(false) }
    var sampleMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(detectedKey) {
        if (captureActive && detectedKey != null) {
            keyCode = detectedKey
            keyName =
                "נלכד בהצלחה: " +
                    KeyEvent.keyCodeToString(detectedKey).removePrefix("KEYCODE_") +
                    " (" + detectedKey + ")"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (existing == null) "הוסף מיפוי מקש" else "עריכת מיפוי")
                Spacer(Modifier.height(6.dp))
                Text("שלב " + step + " מתוך 3", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
                )
            }
        },
        text = {
            when (step) {
                1 -> {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("שם המיפוי") },
                            supportingText = { Text("לדוגמה: כפתור ווליום ימני") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = onBeginCapture,
                            modifier = Modifier.fillMaxWidth().height(74.dp)
                        ) {
                            Text(
                                if (captureActive) "ממתין בלחיצה…" else "🔘 " + keyName,
                                fontSize = 18.sp
                            )
                        }

                        if (keyCode != null) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("נלכד בהצלחה", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(keyName)
                                }
                            }
                        }

                        Text("אין צורך להזין KeyCode ידנית.", fontSize = 14.sp)
                    }
                }

                2 -> {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("איך תרצה שהמקש יתנהג?", style = MaterialTheme.typography.titleMedium)

                        PressOption(
                            title = "לחיצה בודדת",
                            subtitle = "פעולה אחת בלחיצה רגילה",
                            selected = pressCount == 1,
                            onClick = { pressCount = 1 }
                        )
                        PressOption(
                            title = "לחיצה כפולה",
                            subtitle = "שתי לחיצות מהירות",
                            selected = pressCount == 2,
                            onClick = { pressCount = 2 }
                        )
                        PressOption(
                            title = "לחיצה ארוכה",
                            subtitle = "החזקה של המקש",
                            selected = pressCount == 0,
                            onClick = { pressCount = 0 }
                        )
                    }
                }

                3 -> {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showActionMenu = true },
                                modifier = Modifier.fillMaxWidth().height(58.dp)
                            ) {
                                Text(
                                    when (actionType) {
                                        ActionType.LAUNCH_APP -> "📱 פתיחת אפליקציה"
                                        ActionType.SYSTEM_KEY -> "⚙ פעולת מערכת"
                                        ActionType.CLICK_NODE_BY_ID,
                                        ActionType.CLICK_NODE_BY_TEXT -> "🎯 לחיצה על רכיב במסך"
                                        ActionType.SEND_INTENT -> "🛠 מתקדם — Intent"
                                    },
                                    fontSize = 17.sp
                                )
                            }

                            DropdownMenu(
                                expanded = showActionMenu,
                                onDismissRequest = { showActionMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📱 פתיחת אפליקציה") },
                                    onClick = {
                                        actionType = ActionType.LAUNCH_APP
                                        showActionMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚙ פעולת מערכת") },
                                    onClick = {
                                        actionType = ActionType.SYSTEM_KEY
                                        showActionMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎯 לחיצה על רכיב במסך") },
                                    onClick = {
                                        actionType = ActionType.CLICK_NODE_BY_TEXT
                                        showActionMenu = false
                                    }
                                )
                                if (showAdvanced) {
                                    DropdownMenuItem(
                                        text = { Text("🛠 מתקדם — פקודת Intent") },
                                        onClick = {
                                            actionType = ActionType.SEND_INTENT
                                            showActionMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        when (actionType) {
                            ActionType.LAUNCH_APP -> {
                                OutlinedButton(
                                    onClick = { showApps = true },
                                    modifier = Modifier.fillMaxWidth().height(62.dp)
                                ) {
                                    Text(
                                        if (selectedAppName.isBlank()) "בחר אפליקציה מהרשימה"
                                        else "נבחרה: " + selectedAppName
                                    )
                                }
                            }

                            ActionType.SYSTEM_KEY -> {
                                Box(Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showSystemMenu = true },
                                        modifier = Modifier.fillMaxWidth().height(58.dp)
                                    ) {
                                        Text("פעולה: " + systemAction.titleHebrew)
                                    }
                                    DropdownMenu(
                                        expanded = showSystemMenu,
                                        onDismissRequest = { showSystemMenu = false }
                                    ) {
                                        SystemActionPreset.entries.forEach { preset ->
                                            DropdownMenuItem(
                                                text = { Text(preset.titleHebrew) },
                                                onClick = {
                                                    systemAction = preset
                                                    showSystemMenu = false

                                                    if (preset == SystemActionPreset.FLASHLIGHT) {
                                                        val activity = context as? ComponentActivity
                                                        if (activity != null) {
                                                            ActivityCompat.requestPermissions(
                                                                activity,
                                                                arrayOf(Manifest.permission.CAMERA),
                                                                2001
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "פעולות מוכנות: בית, חזרה, אחרונים, התראות, מדיה, ווליום ועוד.",
                                    fontSize = 14.sp
                                )
                            }

                            ActionType.CLICK_NODE_BY_ID,
                            ActionType.CLICK_NODE_BY_TEXT -> {
                                OutlinedButton(
                                    onClick = {
                                        val service = KeyInterceptorAccessibilityService.instance
                                        if (service == null) {
                                            sampleMessage = "הפעל תחילה את שירות הנגישות."
                                        } else {
                                            sampling = true
                                            service.sampleNode { text, viewId ->
                                                sampling = false
                                                nodeTarget =
                                                    if (actionType == ActionType.CLICK_NODE_BY_ID) viewId else text
                                                sampleMessage =
                                                    if (nodeTarget.isBlank()) {
                                                        "לא נמצא מידע זמין ברכיב שנבחר."
                                                    } else {
                                                        "הרכיב נדגם בהצלחה."
                                                    }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(62.dp)
                                ) {
                                    Text(
                                        if (sampling) "ממתין לבחירת רכיב…" else "🎯 דגום רכיב מהמסך"
                                    )
                                }

                                FilterChip(
                                    selected = actionType == ActionType.CLICK_NODE_BY_TEXT,
                                    onClick = { actionType = ActionType.CLICK_NODE_BY_TEXT },
                                    label = { Text("לפי טקסט") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FilterChip(
                                    selected = actionType == ActionType.CLICK_NODE_BY_ID,
                                    onClick = { actionType = ActionType.CLICK_NODE_BY_ID },
                                    label = { Text("לפי מזהה רכיב") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                sampleMessage?.let { Text(it) }

                                if (nodeTarget.isNotBlank()) {
                                    Card(Modifier.fillMaxWidth()) {
                                        Text(
                                            nodeTarget,
                                            modifier = Modifier.padding(14.dp),
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }

                            ActionType.SEND_INTENT -> {
                                OutlinedTextField(
                                    value = customIntent,
                                    onValueChange = { customIntent = it },
                                    label = { Text("Intent מותאם אישית") },
                                    supportingText = { Text("למשתמשים מתקדמים בלבד") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        customIntent = Settings.ACTION_WIFI_SETTINGS
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("הכנס פעולה נפוצה: הגדרות Wi‑Fi")
                                }
                            }
                        }

                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(
                                if (showAdvanced) "הסתר אפשרויות מתקדמות"
                                else "אפשרויות מתקדמות"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step < 3) {
                Button(
                    enabled = step != 1 || keyCode != null,
                    onClick = { step++ }
                ) {
                    Text("המשך")
                }
            } else {
                Button(
                    enabled = isMappingValid(
                        keyCode,
                        actionType,
                        selectedAppPackage,
                        nodeTarget,
                        customIntent
                    ),
                    onClick = {
                        val safeKey = keyCode ?: return@Button
                        onSave(
                            KeyActionConfig(
                                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                customLabel = label.trim(),
                                triggerKeyCode = safeKey,
                                keyNameHebrew = keyName,
                                tapCount = pressCount,
                                actionType = actionType,
                                targetPackage = selectedAppPackage,
                                targetAppName = selectedAppName,
                                targetClassOrIntent = customIntent,
                                nodeIdentifier = nodeTarget,
                                systemActionId = systemAction.id,
                                systemKeyCode = systemAction.legacyKeyCode,
                                profileName = existing?.profileName ?: "DEFAULT",
                                isEnabled = existing?.isEnabled ?: true
                            )
                        )
                    }
                ) {
                    Text("שמור")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) { Text("מחק") }
                }
                TextButton(
                    onClick = {
                        if (step > 1) step-- else onDismiss()
                    }
                ) {
                    Text(if (step > 1) "חזרה" else "ביטול")
                }
            }
        }
    )

    if (showApps) {
        AppPickerDialog(
            onDismiss = { showApps = false },
            onSelect = { pkg, name ->
                selectedAppPackage = pkg
                selectedAppName = name
                showApps = false
            }
        )
    }
}

private fun isMappingValid(
    keyCode: Int?,
    actionType: ActionType,
    packageName: String,
    nodeTarget: String,
    intent: String
): Boolean {
    if (keyCode == null) return false
    return when (actionType) {
        ActionType.LAUNCH_APP -> packageName.isNotBlank()
        ActionType.SYSTEM_KEY -> true
        ActionType.CLICK_NODE_BY_ID,
        ActionType.CLICK_NODE_BY_TEXT -> nodeTarget.isNotBlank()
        ActionType.SEND_INTENT -> intent.isNotBlank()
    }
}

@Composable
private fun PressOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (selected) "●" else "○",
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 22.sp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle)
            }
        }
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
                    icon = runCatching { app.loadIcon(pm).toBitmap(56, 56) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בחירת אפליקציה") },
        text = {
            LazyColumn(
                Modifier.height(430.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app.packageName, app.label) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = app.label,
                                modifier = Modifier.size(48.dp)
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
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("סגור") } }
    )
}

@Composable
private fun SettingsDialog(
    prefsRepo: AppPreferencesRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val timeout by prefsRepo.tapTimeoutFlow.collectAsState(initial = 450L)
    val debounce by prefsRepo.debounceMsFlow.collectAsState(initial = 80L)
    val hud by prefsRepo.hudStyleFlow.collectAsState(initial = HudStyle.SHORT_TEXT)
    val theme by prefsRepo.themeOptionFlow.collectAsState(initial = ThemeOption.DARK_OLED)
    val big by prefsRepo.carFriendlyUiFlow.collectAsState(initial = true)

    var localTimeout by remember(timeout) { mutableStateOf(timeout.toFloat()) }
    var localDebounce by remember(debounce) { mutableStateOf(debounce.toFloat()) }
    var localHud by remember(hud) { mutableStateOf(hud) }
    var localTheme by remember(theme) { mutableStateOf(theme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הגדרות מתקדמות") },
        text = {
            LazyColumn(
                Modifier.height(520.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("מראה ועיצוב", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeCard(ThemeOption.LIGHT, localTheme == ThemeOption.LIGHT) {
                            localTheme = ThemeOption.LIGHT
                        }
                        ThemeCard(ThemeOption.DARK, localTheme == ThemeOption.DARK) {
                            localTheme = ThemeOption.DARK
                        }
                        ThemeCard(ThemeOption.DARK_OLED, localTheme == ThemeOption.DARK_OLED) {
                            localTheme = ThemeOption.DARK_OLED
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { localTheme = ThemeOption.AUTO },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (localTheme == ThemeOption.AUTO) "✓ אוטומטי" else "אוטומטי")
                    }
                }

                item {
                    Text("חלון זמן לריבוי לחיצות", style = MaterialTheme.typography.titleMedium)
                    Text(localTimeout.toInt().toString() + " מילישניות", fontSize = 18.sp)
                    Slider(
                        value = localTimeout,
                        onValueChange = { localTimeout = it },
                        valueRange = 200f..1000f,
                        steps = 15
                    )
                    Text("הזמן המרבי בין לחיצה ללחיצה באותו רצף.")
                }

                item {
                    Text("סינון רעשים", style = MaterialTheme.typography.titleMedium)
                    Text(localDebounce.toInt().toString() + " מילישניות", fontSize = 18.sp)
                    Slider(
                        value = localDebounce,
                        onValueChange = { localDebounce = it },
                        valueRange = 0f..300f,
                        steps = 14
                    )
                    Text("מונע לחיצות כפולות שנוצרות מרעשי חומרה.")
                }

                item {
                    Text("סגנון חיווי", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            HudStyle.SHORT_TEXT,
                            HudStyle.NUMBER_ONLY,
                            HudStyle.SILENT
                        ).forEach { option ->
                            FilterChip(
                                selected = localHud == option,
                                onClick = { localHud = option },
                                label = { Text(option.titleHebrew) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Switch(
                        checked = big,
                        onCheckedChange = {
                            scope.launch { prefsRepo.saveCarFriendlyUi(it) }
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("ממשק מוגדל", fontWeight = FontWeight.Bold)
                    Text("כפתורים וטקסט גדולים יותר לשימוש נוח.")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    prefsRepo.saveTapTimeout(localTimeout.toLong())
                    prefsRepo.saveDebounce(localDebounce.toLong())
                    prefsRepo.saveHudStyle(localHud)
                    prefsRepo.saveThemeOption(localTheme)
                }
                onDismiss()
            }) {
                Text("שמור")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

@Composable
private fun ThemeCard(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = when (option) {
        ThemeOption.LIGHT -> Color(0xFFF4F4F4)
        ThemeOption.DARK -> Color(0xFF27252D)
        ThemeOption.DARK_OLED -> Color.Black
        ThemeOption.AUTO -> Color.Gray
    }

    Card(Modifier.weight(1f).clickable(onClick = onClick)) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = background,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                if (selected) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "✓",
                            color = if (option == ThemeOption.LIGHT) Color.Black else Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(option.titleHebrew, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("סגור") } }
    )
}

@Composable
private fun BackupRestoreDialog(
    mappings: List<KeyActionConfig>,
    prefsRepo: AppPreferencesRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("גיבוי ושחזור") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("אפשר לגבות או לשחזר את המיפויים דרך לוח ההעתקה של המכשיר.")

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val array = JSONArray()
                        mappings.forEach { array.put(it.toJson()) }
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("Click+ backup", array.toString())
                        )
                        message = "הגיבוי הועתק ללוח."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("העתק גיבוי")
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val clip = clipboard?.primaryClip
                        val text = clip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        runCatching {
                            val array = JSONArray(text)
                            val restored = buildList {
                                for (i in 0 until array.length()) {
                                    add(KeyActionConfig.fromJson(array.getJSONObject(i)))
                                }
                            }
                            scope.launch {
                                prefsRepo.saveMappings(restored)
                                message = "השחזור הושלם בהצלחה."
                            }
                        }.onFailure {
                            message = "לא נמצא גיבוי תקין בלוח."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("שחזר מהלוח")
                }

                if (message.isNotBlank()) Text(message)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("סגור") } }
    )
}

private fun isServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    return manager
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            val serviceInfo = info.resolveInfo?.serviceInfo
            serviceInfo?.packageName == context.packageName &&
                serviceInfo.name == KeyInterceptorAccessibilityService::class.java.name
        }
}

private fun testMapping(context: Context, config: KeyActionConfig) {
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
