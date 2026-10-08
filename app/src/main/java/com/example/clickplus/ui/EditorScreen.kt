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

internal fun EditorScreen(
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
    var learningSecondPoint by remember { mutableStateOf(false) }
    var learningStage by remember { mutableIntStateOf(1) }
    var showDelete by remember { mutableStateOf(false) }
    var learningScreenshot1 by remember(existing?.id) {
        mutableStateOf(runtimePrefs.getString("tap_learning_screenshot_1", null))
    }
    var learningScreenshot2 by remember(existing?.id) {
        mutableStateOf(runtimePrefs.getString("tap_learning_screenshot_2", null))
    }
    var learningScreenshotError1 by remember(existing?.id) {
        mutableStateOf(runtimePrefs.getString("tap_learning_screenshot_error_1", null))
    }
    var learningScreenshotError2 by remember(existing?.id) {
        mutableStateOf(runtimePrefs.getString("tap_learning_screenshot_error_2", null))
    }

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

    val phoneCallPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

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

    fun beginLearning(packageName: String, stage: Int = 1) {
        if (!isAccessibilityEnabled(context) || !isAccessibilityServiceResponsive(context)) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        if (packageName.isBlank()) return

        val safeStage = stage.coerceIn(1, 2)
        learning = true
        learningSecondPoint = safeStage == 2
        learningStage = safeStage

        runtimePrefs.edit()
            .putBoolean("tap_learning", true)
            .putBoolean("tap_capture_ready", false)
            .putInt("tap_learning_stage", safeStage)
            .putString("tap_learning_package", packageName)
            .remove("tap_learning_multi")
            .apply()

        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(it)
        }
    }

    LaunchedEffect(draft.id) {
        while (true) {
            val latestShot1 = runtimePrefs.getString("tap_learning_screenshot_1", null)
            val latestShot2 = runtimePrefs.getString("tap_learning_screenshot_2", null)
            val latestError1 = runtimePrefs.getString("tap_learning_screenshot_error_1", null)
            val latestError2 = runtimePrefs.getString("tap_learning_screenshot_error_2", null)
            if (latestShot1 != learningScreenshot1) learningScreenshot1 = latestShot1
            if (latestShot2 != learningScreenshot2) learningScreenshot2 = latestShot2
            if (latestError1 != learningScreenshotError1) learningScreenshotError1 = latestError1
            if (latestError2 != learningScreenshotError2) learningScreenshotError2 = latestError2

            if (runtimePrefs.getBoolean("tap_capture_ready", false)) {
                val capturedX = runtimePrefs.getFloat("tap_capture_x_ratio", -1f)
                val capturedY = runtimePrefs.getFloat("tap_capture_y_ratio", -1f)
                val stage = runtimePrefs.getInt("tap_capture_stage", 1)
                val pkg = runtimePrefs.getString("tap_capture_package", "").orEmpty()
                val appName = runtimePrefs.getString("tap_capture_app_name", "").orEmpty()
                runtimePrefs.edit().putBoolean("tap_capture_ready", false).apply()

                if (capturedX >= 0f && capturedY >= 0f) {
                    if (stage == 1) {
                        draft = draft.copy(
                            screenTapPackage = pkg,
                            screenTapAppName = appName,
                            screenTapXRatio = capturedX,
                            screenTapYRatio = capturedY,
                            screenTapSecondXRatio = -1f,
                            screenTapSecondYRatio = -1f,
                        )
                        metadata = metadata.copy(
                            portraitX = capturedX,
                            portraitY = capturedY,
                            landscapeX = capturedX,
                            landscapeY = capturedY,
                            portraitSecondX = -1f,
                            portraitSecondY = -1f,
                            landscapeSecondX = -1f,
                            landscapeSecondY = -1f,
                        )
                        learning = false
                        learningSecondPoint = false
                        learningStage = 1
                    } else {
                        draft = draft.copy(
                            actionType = ActionType.MULTI_POINT_TAP,
                            screenTapSecondXRatio = capturedX,
                            screenTapSecondYRatio = capturedY,
                        )
                        val landscape =
                            context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        metadata = if (landscape) {
                            metadata.copy(landscapeSecondX = capturedX, landscapeSecondY = capturedY)
                        } else {
                            metadata.copy(portraitSecondX = capturedX, portraitSecondY = capturedY)
                        }
                        learning = false
                        learningSecondPoint = false
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
            portraitSecondX = -1f,
            portraitSecondY = -1f,
            landscapeSecondX = -1f,
            landscapeSecondY = -1f,
        )
        runtimePrefs.edit()
            .remove("tap_learning_screenshot_1")
            .remove("tap_learning_screenshot_2")
            .remove("tap_learning_screenshot_error_1")
            .remove("tap_learning_screenshot_error_2")
            .apply()
        learningScreenshot1 = null
        learningScreenshot2 = null
        beginLearning(appPackage, 1)
    }

    fun saveCurrent() {
        validationMessage = ""

        fun fail(message: String) {
            validationMessage = message
        }

        if (!actionTypeChosen) return fail("יש לבחור סוג פעולה.")
        if (draft.name.isBlank()) {
            draft = draft.copy(name = "כניסה ל-" + draft.triggerAppName.ifBlank { "אפליקציה" })
        }
        if (draft.triggerType == TriggerType.APP_ENTRY && draft.triggerPackage.isBlank()) {
            return fail("יש לבחור את האפליקציה שבה תזוהה הכניסה.")
        }

        when (draft.actionType) {
            ActionType.SYSTEM -> when (draft.systemActionId) {
                SystemActionPreset.DIAL_NUMBER.id ->
                    if (draft.actionParameter.filter { it.isDigit() }.isBlank()) return fail("יש להזין מספר לחיוג.")
                SystemActionPreset.DIAL_CONTACT.id ->
                    if (draft.contactName.isBlank() || draft.contactNumber.isBlank()) return fail("יש לבחור איש קשר.")
                SystemActionPreset.BRIGHTNESS_SET.id -> {
                    val value = draft.actionParameter.toIntOrNull()
                    if (value == null || value !in 1..100) return fail("יש להזין בהירות בין 1 ל-100.")
                }
                else -> Unit
            }
            ActionType.APP ->
                if (draft.targetPackage.isBlank()) return fail("יש לבחור אפליקציית יעד.")
            ActionType.APP_TAP ->
                if (draft.screenTapPackage.isBlank() || draft.screenTapXRatio < 0f || draft.screenTapYRatio < 0f) {
                    return fail("יש לבחור אפליקציה וללמוד את מיקום הלחיצה.")
                }
            ActionType.MULTI_POINT_TAP ->
                if (
                    draft.screenTapPackage.isBlank() ||
                    draft.screenTapXRatio < 0f ||
                    draft.screenTapYRatio < 0f ||
                    draft.screenTapSecondXRatio < 0f ||
                    draft.screenTapSecondYRatio < 0f
                ) return fail("יש ללמוד גם את הנקודה הראשונה וגם את הנקודה השנייה.")
            ActionType.PROFILE ->
                if (profileIdFromActionId(draft.systemActionId).isNullOrBlank()) return fail("יש לבחור פרופיל יעד.")
        }

        val safe = draft.copy(screenTapCount = 1)
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
                        "מגדירים את סוג הטריגר, בוחרים את הפעולה שתתבצע, ובמידת הצורך מלמדים נקודת לחיצה בתוך אפליקציית היעד.",
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
                }
            }

            item {
                EditorSectionCard(
                    "2",
                    "מתי להפעיל?",
                    "בחר מה יפעיל את הפעולה.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ChoiceChip(
                            selected = draft.triggerType == TriggerType.CLICKPLUS_ENTRY,
                            onClick = {
                                draft = draft.copy(
                                    triggerType = TriggerType.CLICKPLUS_ENTRY,
                                    triggerPackage = "",
                                    triggerAppName = "",
                                )
                                validationMessage = ""
                            },
                            label = "לחיצות כניסה לקליק פלוס",
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        )
                        ChoiceChip(
                            selected = draft.triggerType == TriggerType.APP_ENTRY,
                            onClick = {
                                draft = draft.copy(triggerType = TriggerType.APP_ENTRY)
                                validationMessage = ""
                            },
                            label = "לחיצות כניסה לאפליקציה אחרת",
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    if (draft.triggerType == TriggerType.CLICKPLUS_ENTRY) {
                        Text(
                            "כניסה ל-ClickPlus נספרת מיד.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
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
                        Text(
                            if (draft.triggerPackage.isBlank()) {
                                "בחר אפליקציה. הכניסה הראשונה אליה רק פותחת אותה ואינה נספרת."
                            } else if (draft.pressCount == 1) {
                                "הכניסה הראשונה רק פותחת את האפליקציה ואינה נספרת. כניסה חוזרת אחת תפעיל את הפעולה, לכן מומלץ בדרך כלל לבחור רצף של 2 ומעלה."
                            } else {
                                "הכניסה הראשונה רק פותחת את האפליקציה ואינה נספרת. כניסות חוזרות לאותה אפליקציה נספרות ברצף."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (draft.triggerPackage.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(Modifier.height(9.dp))
                    Text(
                        if (draft.triggerType == TriggerType.CLICKPLUS_ENTRY) {
                            "כמה לחיצות רצופות ייחשבו לרצף?"
                        } else {
                            "כמה כניסות רצופות ייחשבו לרצף?"
                        },
                        fontWeight = FontWeight.Medium,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1..5, 6..10).forEach { range ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                range.forEach { count ->
                                    ChoiceChip(
                                        selected = draft.pressCount == count,
                                        onClick = { draft = draft.copy(pressCount = count) },
                                        label = count.toString(),
                                        modifier = Modifier.weight(1f).height(46.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                EditorSectionCard(
                    "3",
                    "תנאי הפעלה",
                    "קודם בוחרים אם הפעולה עובדת בכל מצב או רק בתנאי מסוים.",
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        ChoiceChip(
                            selected = draft.contextConditionType == ContextConditionType.ANY,
                            onClick = {
                                draft = draft.copy(
                                    contextConditionType = ContextConditionType.ANY,
                                    contextConditionValue = "",
                                    contextConditionName = "",
                                )
                            },
                            label = "בכל מצב",
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                        ChoiceChip(
                            selected = draft.contextConditionType != ContextConditionType.ANY,
                            onClick = {
                                if (draft.contextConditionType == ContextConditionType.ANY) {
                                    draft = draft.copy(
                                        contextConditionType = ContextConditionType.APP,
                                        contextConditionValue = "",
                                        contextConditionName = "",
                                    )
                                }
                            },
                            label = "לפי מצב מסוים",
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                    }

                    if (draft.contextConditionType != ContextConditionType.ANY) {
                        Spacer(Modifier.height(8.dp))
                        val basicSupported = setOf(
                            ContextConditionType.APP,
                            ContextConditionType.BRIGHTNESS_LOW,
                            ContextConditionType.VOLUME_LEVEL,
                        )
                        Text("בחר את המצב:", fontWeight = FontWeight.Medium)
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            ContextConditionType.entries
                                .filter { it != ContextConditionType.ANY }
                                .forEach { condition ->
                                    val supported = AdvancedRuleRepository.currentMode(context) != AppMode.BASIC ||
                                        condition in setOf(
                                            ContextConditionType.APP,
                                            ContextConditionType.BRIGHTNESS_LOW,
                                            ContextConditionType.VOLUME_LEVEL,
                                        )
                                    ChoiceChip(
                                        selected = draft.contextConditionType == condition,
                                        enabled = supported,
                                        onClick = {
                                            if (supported) {
                                                draft = draft.copy(
                                                    contextConditionType = condition,
                                                    contextConditionValue = if (
                                                        condition == ContextConditionType.VOLUME_LEVEL &&
                                                        draft.contextConditionValue.isBlank()
                                                    ) "15" else draft.contextConditionValue,
                                                )
                                            }
                                        },
                                        label = if (supported) {
                                            condition.titleHebrew
                                        } else {
                                            condition.titleHebrew + " · לא זמין במצב בסיסי"
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp),
                                    )
                                }
                        }                        if (draft.contextConditionType == ContextConditionType.APP ||
                            draft.contextConditionType == ContextConditionType.RADIO
                        ) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { appDialog = true }, Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Apps, null)
                                Spacer(Modifier.width(8.dp))
                                Text(draft.contextConditionName.ifBlank { "בחירת אפליקציה להשוואה" })
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
            }
            item {
                EditorSectionCard(
                    "4",
                    "מה לבצע?",
                    "בחר סוג פעולה. לאחר מכן תיפתח רק הקבוצה הרלוונטית של האפשרויות.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            ChoiceChip(
                                selected = actionTypeChosen && draft.actionType == ActionType.APP,
                                onClick = {
                                    actionTypeChosen = true
                                    selectedSystemCategory = null
                                    draft = draft.copy(actionType = ActionType.APP, systemActionId = "")
                                },
                                label = "פתיחת אפליקציה",
                                modifier = Modifier.weight(1f).height(62.dp),
                            )
                            ChoiceChip(
                                selected = actionTypeChosen && draft.actionType == ActionType.APP_TAP,
                                enabled = AdvancedRuleRepository.currentMode(context) == AppMode.FULL,
                                onClick = {
                                    actionTypeChosen = true
                                    selectedSystemCategory = null
                                    draft = draft.copy(actionType = ActionType.APP_TAP, systemActionId = "")
                                },
                                label = "פתיחה+לחיצות",
                                modifier = Modifier.weight(1f).height(62.dp),
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            ChoiceChip(
                                selected = actionTypeChosen && draft.actionType == ActionType.SYSTEM,
                                onClick = {
                                    actionTypeChosen = true
                                    selectedSystemCategory = if (existing?.actionType == ActionType.SYSTEM) {
                                        SystemActionPreset.entries.firstOrNull { it.id == draft.systemActionId }?.categoryHebrew
                                    } else null
                                    draft = draft.copy(
                                        actionType = ActionType.SYSTEM,
                                        systemActionId = if (existing?.actionType == ActionType.SYSTEM) draft.systemActionId else "",
                                    )
                                },
                                label = "פעולת מכשיר",
                                modifier = Modifier.weight(1f).height(62.dp),
                            )
                            ChoiceChip(
                                selected = actionTypeChosen && draft.actionType == ActionType.PROFILE,
                                onClick = {
                                    actionTypeChosen = true
                                    selectedSystemCategory = null
                                    draft = draft.copy(
                                        actionType = ActionType.PROFILE,
                                        systemActionId = if (draft.actionType == ActionType.PROFILE && profileIdFromActionId(draft.systemActionId) != null) {
                                            draft.systemActionId
                                        } else {
                                            ""
                                        },
                                    )
                                },
                                label = "החלפת פרופיל",
                                modifier = Modifier.weight(1f).height(62.dp),
                            )
                        }
                    }


                    if (AdvancedRuleRepository.currentMode(context) == AppMode.BASIC) {
                        Text(
                            "במצב בסיסי פעולות שדורשות שירות נגישות, כמו לחיצה בתוך אפליקציה, מוצגות כלא זמינות.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (!actionTypeChosen) {
                        Text(
                            "בחר קודם סוג פעולה. האפשרויות המפורטות יופיעו רק לאחר הבחירה.",
                            Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        val permissionNeeded =
                            when {
                                (draft.actionType == ActionType.APP_TAP || draft.actionType == ActionType.MULTI_POINT_TAP) &&
                                    AdvancedRuleRepository.currentMode(context) == AppMode.FULL &&
                                    !isAccessibilityEnabled(context) -> true
                                draft.triggerType == TriggerType.APP_ENTRY &&
                                    AdvancedRuleRepository.currentMode(context) == AppMode.BASIC &&
                                    !hasUsageAccess(context) -> true
                                draft.actionType == ActionType.SYSTEM &&
                                    draft.systemActionId in setOf(
                                        SystemActionPreset.BRIGHTNESS_UP.id,
                                        SystemActionPreset.BRIGHTNESS_DOWN.id,
                                        SystemActionPreset.BRIGHTNESS_SET.id,
                                    ) &&
                                    !Settings.System.canWrite(context) -> true
                                draft.actionType == ActionType.SYSTEM &&
                                    draft.systemActionId in setOf(
                                        SystemActionPreset.ANSWER_CALL.id,
                                        SystemActionPreset.DECLINE_CALL.id,
                                    ) &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ANSWER_PHONE_CALLS,
                                    ) != PackageManager.PERMISSION_GRANTED -> true
                                else -> false
                            }
                        if (permissionNeeded) {
                            val accessibilityMissing =
                                (draft.actionType == ActionType.APP_TAP || draft.actionType == ActionType.MULTI_POINT_TAP) &&
                                    !isAccessibilityEnabled(context)
                            val usageMissing =
                                draft.triggerType == TriggerType.APP_ENTRY &&
                                    AdvancedRuleRepository.currentMode(context) == AppMode.BASIC &&
                                    !hasUsageAccess(context)
                            val brightnessMissing =
                                draft.actionType == ActionType.SYSTEM &&
                                    draft.systemActionId in setOf(
                                        SystemActionPreset.BRIGHTNESS_UP.id,
                                        SystemActionPreset.BRIGHTNESS_DOWN.id,
                                        SystemActionPreset.BRIGHTNESS_SET.id,
                                    ) &&
                                    !Settings.System.canWrite(context)
                            val phoneMissing =
                                draft.actionType == ActionType.SYSTEM &&
                                    draft.systemActionId in setOf(
                                        SystemActionPreset.ANSWER_CALL.id,
                                        SystemActionPreset.DECLINE_CALL.id,
                                    ) &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ANSWER_PHONE_CALLS,
                                    ) != PackageManager.PERMISSION_GRANTED
                            Surface(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(11.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(
                                        when {
                                            accessibilityMissing -> "הפעולה הזאת דורשת את שירות הנגישות כדי לבצע לחיצה בתוך האפליקציה."
                                            usageMissing -> "הטריגר הזה במצב בסיסי דורש גישה לנתוני שימוש כדי לזהות איזו אפליקציה נמצאת על המסך."
                                            brightnessMissing -> "הפעולה הזאת דורשת הרשאה לשינוי בהירות המסך."
                                            phoneMissing -> "פעולת השיחה הזאת דורשת הרשאת טלפון כדי לפעול."
                                            else -> ""
                                        },
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    TextButton(
                                        onClick = {
                                            when {
                                                accessibilityMissing -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                                usageMissing -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                                brightnessMissing -> context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                        Uri.parse("package:" + context.packageName),
                                                    )
                                                )
                                                phoneMissing -> phoneCallPermission.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                                            }
                                        },
                                    ) {
                                        Text("מעבר לנתינת ההרשאה")
                                    }
                                }
                            }
                        }
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
                                }
                            }
                            ActionType.PROFILE -> {
                                Spacer(Modifier.height(6.dp))
                                ProfileActionPicker(
                                    selectedId = draft.systemActionId,
                                    onActionSelected = { id ->
                                        draft = draft.copy(systemActionId = id)
                                    },
                                )
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
                                    Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                ) {
                                    Icon(Icons.Outlined.LocationOn, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (draft.screenTapPackage.isBlank()) {
                                            "בחירת אפליקציה ללימוד מיקום"
                                        } else {
                                            "לימוד מחדש של הנקודה הראשונה"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                if (draft.screenTapPackage.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedCard(
                                        Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Column(
                                            Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp),
                                        ) {
                                            Text(
                                                "נקודה ראשונה · " + draft.screenTapAppName.ifBlank { "אפליקציית היעד" },
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                "X " + (x * 100).toInt() + "% · Y " + (y * 100).toInt() + "%",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            if (!learningScreenshot1.isNullOrBlank()) {
                                                LearningScreenshotPreview(
                                                    path = learningScreenshot1,
                                                    x = x,
                                                    y = y,
                                                )
                                            } else if (!learningScreenshotError1.isNullOrBlank()) {
                                                Text(
                                                    "צילום המסך לא זמין: " + learningScreenshotError1,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            }
                                            PointEditor(
                                                x,
                                                y,
                                                metadata.toleranceXRatio,
                                                metadata.toleranceYRatio,
                                            ) { nx, ny ->
                                                draft = draft.copy(screenTapXRatio = nx, screenTapYRatio = ny)
                                                val landscape = context.resources.configuration.orientation ==
                                                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                                metadata = if (landscape) {
                                                    metadata.copy(landscapeX = nx, landscapeY = ny)
                                                } else {
                                                    metadata.copy(portraitX = nx, portraitY = ny)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    learningSecondPoint = true
                                                    beginLearning(draft.screenTapPackage, 2)
                                                },
                                                Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                            ) {
                                                Icon(Icons.Outlined.Add, null)
                                                Spacer(Modifier.width(7.dp))
                                                Text(
                                                    if (draft.screenTapSecondXRatio >= 0f) {
                                                        "לימוד מחדש של המיקום השני"
                                                    } else {
                                                        "לימוד מיקום נוסף"
                                                    }
                                                )
                                            }

                                            if (draft.screenTapSecondXRatio >= 0f) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "נקודה שנייה",
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    "X " + (x2 * 100).toInt() + "% · Y " + (y2 * 100).toInt() + "%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                if (!learningScreenshot2.isNullOrBlank()) {
                                                    LearningScreenshotPreview(
                                                        path = learningScreenshot2,
                                                        x = x2,
                                                        y = y2,
                                                    )
                                                } else if (!learningScreenshotError2.isNullOrBlank()) {
                                                    Text(
                                                        "צילום המסך לא זמין: " + learningScreenshotError2,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                    )
                                                }
                                                PointEditor(
                                                    x2,
                                                    y2,
                                                    metadata.toleranceXRatio,
                                                    metadata.toleranceYRatio,
                                                ) { nx, ny ->
                                                    draft = draft.copy(
                                                        actionType = ActionType.MULTI_POINT_TAP,
                                                        screenTapSecondXRatio = nx,
                                                        screenTapSecondYRatio = ny,
                                                    )
                                                    val landscape = context.resources.configuration.orientation ==
                                                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                                    metadata = if (landscape) {
                                                        metadata.copy(landscapeSecondX = nx, landscapeSecondY = ny)
                                                    } else {
                                                        metadata.copy(portraitSecondX = nx, portraitSecondY = ny)
                                                    }
                                                }
                                            }

                                            if (draft.actionType == ActionType.MULTI_POINT_TAP) {
                                                Text(
                                                    "השהיה בין שתי הלחיצות: " +
                                                        String.format(
                                                            Locale.getDefault(),
                                                            "%.1f שניות",
                                                            draft.screenTapIntervalMs / 1000f,
                                                        ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Slider(
                                                    value = draft.screenTapIntervalMs.toFloat(),
                                                    onValueChange = {
                                                        draft = draft.copy(
                                                            screenTapIntervalMs = it.toLong().coerceIn(500L, 10_000L),
                                                        )
                                                    },
                                                    valueRange = 500f..10_000f,
                                                    steps = 19,
                                                )
                                            }

                                            if (learning) {
                                                Surface(
                                                    Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                ) {
                                                    Text(
                                                        if (learningStage == 2) {
                                                            "מצב לימוד פעיל: גע עכשיו במקום השני."
                                                        } else {
                                                            "מצב לימוד פעיל: גע עכשיו במקום הראשון."
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
                    "הגדרות אלו אינן חובה. ברירות המחדל מתאימות לרוב הפעולות.",
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
                        description = "המתנה אחרי זיהוי הטריגר ולפני הביצוע. בפעולת פתיחה+לחיצות ההמתנה מתחילה אחרי שהאפליקציה הופיעה.",
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
                    EditorSliderRow(
                        "רמת עדיפות",
                        metadata.priority.toString(),
                        description = "אם כמה פעולות מתאימות, ערך גבוה יותר מקבל קדימות.",
                    ) {
                        Slider(
                            value = metadata.priority.coerceIn(0, 10).toFloat(),
                            onValueChange = {
                                metadata = metadata.copy(priority = it.toInt().coerceIn(0, 10))
                            },
                            valueRange = 0f..10f,
                            steps = 9,
                        )
                    }
                    EditorSliderRow(
                        "מספר ניסיונות",
                        metadata.retries.toString(),
                        description = "מספר ניסיונות הביצוע במקרה של כישלון.",
                    ) {
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
                            description = "כמה סטייה לרוחב עדיין נחשבת לאותה נקודה.",
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
                            description = "כמה סטייה לגובה עדיין נחשבת לאותה נקודה.",
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
                                        portraitSecondX = if (metadata.portraitSecondX >= 0f) metadata.portraitSecondX else
                                            draft.screenTapSecondXRatio.takeIf { it >= 0f } ?: -1f,
                                        portraitSecondY = if (metadata.portraitSecondY >= 0f) metadata.portraitSecondY else
                                            draft.screenTapSecondYRatio.takeIf { it >= 0f } ?: -1f,
                                        landscapeSecondX = if (metadata.landscapeSecondX >= 0f) metadata.landscapeSecondX else
                                            draft.screenTapSecondXRatio.takeIf { it >= 0f } ?: -1f,
                                        landscapeSecondY = if (metadata.landscapeSecondY >= 0f) metadata.landscapeSecondY else
                                            draft.screenTapSecondYRatio.takeIf { it >= 0f } ?: -1f,
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
                            if (draft.screenTapSecondXRatio >= 0f) {
                                Spacer(Modifier.height(8.dp))
                                Text("נקודה שנייה · אנכי", fontWeight = FontWeight.Bold)
                                PointEditor(
                                    metadata.portraitSecondX.takeIf { it >= 0f } ?: x2,
                                    metadata.portraitSecondY.takeIf { it >= 0f } ?: y2,
                                    metadata.toleranceXRatio,
                                    metadata.toleranceYRatio,
                                ) { nx, ny ->
                                    metadata = metadata.copy(portraitSecondX = nx, portraitSecondY = ny)
                                }
                                Text("נקודה שנייה · אופקי", fontWeight = FontWeight.Bold)
                                PointEditor(
                                    metadata.landscapeSecondX.takeIf { it >= 0f } ?: x2,
                                    metadata.landscapeSecondY.takeIf { it >= 0f } ?: y2,
                                    metadata.toleranceXRatio,
                                    metadata.toleranceYRatio,
                                ) { nx, ny ->
                                    metadata = metadata.copy(landscapeSecondX = nx, landscapeSecondY = ny)
                                }
                            }
                        }
                    }
                    ProfileSelector(
                        repo.profiles(),
                        metadata.profileId
                    ) { selected ->
                        metadata = metadata.copy(profileId = selected)
                    }
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
                    Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("בדיקת הפעולה")
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
