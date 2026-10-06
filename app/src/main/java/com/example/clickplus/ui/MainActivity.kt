package com.example.clickplus.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.HudStyle
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.OperationMode
import com.example.clickplus.data.ThemeOption
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var prefsRepo: AppPreferencesRepository
    private val detectedKey = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsRepo = AppPreferencesRepository(applicationContext)
        setContent { ClickPlusApp(prefsRepo, detectedKey.value) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) detectedKey.value = event.keyCode
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun ClickPlusApp(prefsRepo: AppPreferencesRepository, detectedKey: Int?) {
    val theme by prefsRepo.themeOptionFlow.collectAsState(initial = ThemeOption.DARK_OLED)
    val colors = if (theme != ThemeOption.LIGHT) {
        androidx.compose.material3.darkColorScheme(
            background = Color.Black, surface = Color(0xFF121212), primary = Color(0xFF00E676)
        )
    } else androidx.compose.material3.lightColorScheme()
    MaterialTheme(colorScheme = colors) { Surface(Modifier.fillMaxSize()) { MainScreen(prefsRepo, detectedKey) } }
}

@Composable
private fun MainScreen(prefsRepo: AppPreferencesRepository, detectedKey: Int?) {
    val completed by prefsRepo.onboardingCompletedFlow.collectAsState(initial = false)
    if (!completed) OnboardingWizard(prefsRepo, detectedKey) else MainDashboard(prefsRepo)
}

@Composable
private fun OnboardingWizard(prefsRepo: AppPreferencesRepository, detectedKey: Int?) {
    var step by remember { mutableStateOf(1) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Click+", style = MaterialTheme.typography.headlineLarge)
            Text("Hardware Key Mapper", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(26.dp))
            when (step) {
                1 -> StepBlock(
                    "שלב 1 — הרשאת נגישות",
                    "אפשר את Click+ תחת הגדרות נגישות כדי ללכוד מקשי חומרה.",
                    "פתיחת הגדרות נגישות"
                ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                2 -> StepBlock(
                    "שלב 2 — הרשאת HUD",
                    "אפשר הרשאת הצגה מעל אפליקציות כדי לקבל חיווי HUD בזמן הלחיצה.",
                    "פתיחת הרשאת Overlay"
                ) { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { data = android.net.Uri.parse("package:${context.packageName}") }) }
                3 -> {
                    Text("שלב 3 — בדיקת מקש", fontSize = 21.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("לחץ על מקש פיזי כשהמסך הזה פתוח כדי לזהות את KeyCode.")
                    Spacer(Modifier.height(12.dp))
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("זוהה: ${detectedKey?.let { "KEYCODE $it" } ?: "טרם זוהה"}", style = MaterialTheme.typography.titleLarge) } }
                    Spacer(Modifier.height(12.dp))
                    Text("אפשר להמשיך גם ללא זיהוי ולהגדיר מיפוי ידנית.")
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (step > 1) OutlinedButton(onClick = { step-- }) { Text("חזרה") } else Spacer(Modifier.size(1.dp))
            if (step < 3) Button(onClick = { step++ }) { Text("הבא") }
            else Button(onClick = { scope.launch { prefsRepo.setOnboardingCompleted(true) } }) { Text("התחל להשתמש") }
        }
    }
}

@Composable
private fun StepBlock(title: String, body: String, button: String, onClick: () -> Unit) {
    Text(title, fontSize = 21.sp)
    Spacer(Modifier.height(14.dp))
    Text(body)
    Spacer(Modifier.height(18.dp))
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text(button) }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun MainDashboard(prefsRepo: AppPreferencesRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mappings by prefsRepo.mappingsFlow.collectAsState(initial = emptyList())
    val bigUi by prefsRepo.carFriendlyUiFlow.collectAsState(initial = true)
    val mode by prefsRepo.operationModeFlow.collectAsState(initial = OperationMode.MODE_A_MULTI_TAP)
    val hud by prefsRepo.hudStyleFlow.collectAsState(initial = HudStyle.SHORT_TEXT)
    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Click+") }, actions = {
        Text("Big UI", modifier = Modifier.padding(end = 8.dp))
        Switch(checked = bigUi, onCheckedChange = { scope.launch { prefsRepo.saveCarFriendlyUi(it) } }, modifier = Modifier.padding(end = 12.dp))
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = mode == OperationMode.MODE_A_MULTI_TAP, onClick = { scope.launch { prefsRepo.saveOperationMode(OperationMode.MODE_A_MULTI_TAP) } }, label = { Text("Multi-Tap") })
                FilterChip(selected = mode == OperationMode.MODE_B_CONFIRMATION, onClick = { scope.launch { prefsRepo.saveOperationMode(OperationMode.MODE_B_CONFIRMATION) } }, label = { Text("Confirm") })
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { showSettings = true }) { Text("הגדרות") }
            }
            Spacer(Modifier.height(12.dp))
            Text("מיפויים", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                if (mappings.isEmpty()) item { EmptyState { showAdd = true } }
                items(mappings, key = { it.id }) { item ->
                    MappingRowItem(item, bigUi) { updated ->
                        scope.launch { prefsRepo.saveMappings(mappings.map { if (it.id == updated.id) updated else it }) }
                    }
                }
            }
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(if (bigUi) 64.dp else 52.dp)) { Text("הוסף מיפוי") }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (showAdd) AddMappingDialog(onDismiss = { showAdd = false }) { config ->
        scope.launch { prefsRepo.saveMappings(mappings + config) }
        showAdd = false
    }
    if (showSettings) SettingsDialog(prefsRepo, hud) { showSettings = false }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("עדיין אין מיפויים", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAdd) { Text("צור מיפוי ראשון") }
    } }
}

@Composable
private fun MappingRowItem(item: KeyActionConfig, bigUi: Boolean, onToggle: (KeyActionConfig) -> Unit) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(if (bigUi) 20.dp else 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (item.customLabel.isNotBlank()) item.customLabel else "Key Code: ${item.triggerKeyCode}", style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
                Text("Taps: ${item.tapCount} • ${item.actionType.name}")
            }
            Switch(checked = item.isEnabled, onCheckedChange = { onToggle(item.copy(isEnabled = it)) })
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { testMapping(context, item) }) { Text("Test") }
            Text("KEYCODE ${item.triggerKeyCode}", modifier = Modifier.align(Alignment.CenterVertically))
        }
    } }
}

private fun testMapping(context: android.content.Context, config: KeyActionConfig) {
    val i = Intent(context, KeyInterceptorAccessibilityService::class.java)
        .setAction("com.example.clickplus.TEST_MAPPING")
        .putExtra("config_json", config.toJson().toString())
    runCatching { context.startService(i) }
}

@Composable
private fun AddMappingDialog(onDismiss: () -> Unit, onSave: (KeyActionConfig) -> Unit) {
    var label by remember { mutableStateOf("") }
    var keyCode by remember { mutableStateOf("") }
    var taps by remember { mutableStateOf("1") }
    var action by remember { mutableStateOf(ActionType.SYSTEM_KEY) }
    var target by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("מיפוי חדש") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("שם / תווית") }, singleLine = true)
                OutlinedTextField(value = keyCode, onValueChange = { keyCode = it.filter(Char::isDigit) }, label = { Text("Trigger KeyCode") }, singleLine = true)
                OutlinedTextField(value = taps, onValueChange = { taps = it.filter(Char::isDigit) }, label = { Text("מספר לחיצות") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Action: ${action.name}") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ActionType.entries.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { action = a; expanded = false }) }
                    }
                }
                if (action != ActionType.SYSTEM_KEY) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(when (action) {
                            ActionType.LAUNCH_APP -> "Package name"
                            ActionType.CLICK_NODE_BY_ID -> "View ID"
                            ActionType.CLICK_NODE_BY_TEXT -> "Text to click"
                            ActionType.SEND_INTENT -> "Broadcast action"
                            else -> "Target"
                        }) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val k = keyCode.toIntOrNull()
                val t = taps.toIntOrNull()
                if (k != null && t != null && k >= 0 && t > 0) {
                    onSave(KeyActionConfig(
                        customLabel = label,
                        triggerKeyCode = k,
                        tapCount = t,
                        actionType = action,
                        targetPackage = if (action == ActionType.LAUNCH_APP) target else "",
                        nodeIdentifier = if (action == ActionType.CLICK_NODE_BY_ID || action == ActionType.CLICK_NODE_BY_TEXT) target else "",
                        targetClassOrIntent = if (action == ActionType.SEND_INTENT) target else "",
                        systemKeyCode = if (action == ActionType.SYSTEM_KEY) target.toIntOrNull() ?: k else 0
                    ))
                }
            }) { Text("שמור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

@Composable
private fun SettingsDialog(prefsRepo: AppPreferencesRepository, hud: HudStyle, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tapTimeout by remember { mutableStateOf("450") }
    var debounce by remember { mutableStateOf("80") }
    var selectedHud by remember { mutableStateOf(hud) }
    var theme by remember { mutableStateOf(ThemeOption.DARK_OLED) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("הגדרות") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = tapTimeout, onValueChange = { tapTimeout = it.filter(Char::isDigit) }, label = { Text("Tap timeout (ms)") })
            OutlinedTextField(value = debounce, onValueChange = { debounce = it.filter(Char::isDigit) }, label = { Text("Debounce (ms)") })
            Text("HUD")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudStyle.entries.forEach { s -> FilterChip(selected = selectedHud == s, onClick = { selectedHud = s }, label = { Text(s.name) }) }
            }
            Text("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeOption.entries.forEach { t -> FilterChip(selected = theme == t, onClick = { theme = t }, label = { Text(t.name) }) }
            }
        }
    }, confirmButton = {
        Button(onClick = {
            scope.launch {
                prefsRepo.saveTapTimeout(tapTimeout.toLongOrNull() ?: 450L)
                prefsRepo.saveDebounce(debounce.toLongOrNull() ?: 80L)
                prefsRepo.saveHudStyle(selectedHud)
                prefsRepo.saveThemeOption(theme)
            }
            onDismiss()
        }) { Text("שמור") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } })
}
