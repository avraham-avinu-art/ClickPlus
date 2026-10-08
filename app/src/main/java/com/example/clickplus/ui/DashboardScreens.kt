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

internal fun HomeDashboard(
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
    var accessibilityHealthy by remember { mutableStateOf(isAccessibilityServiceResponsive(context)) }
    LaunchedEffect(accessibility) {
        while (true) {
            accessibilityHealthy = isAccessibilityServiceResponsive(context)
            delay(2000)
        }
    }
    val repo = remember { AdvancedRuleRepository(context) }
    var deleteTarget by remember { mutableStateOf<KeyActionConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("קליק פלוס", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, "הגדרות")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            Modifier.size(46.dp),
                            CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                "קליק פלוס פעיל",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                "קליק פלוס פעיל",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                when {
                                    mode == AppMode.BASIC -> "מצב בסיסי · ללא נגישות"
                                    accessibility -> "מצב מלא · נגישות פעילה"
                                    else -> "מצב מלא · נגישות לא פעילה"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (mode == AppMode.FULL && !accessibility) {
                                Text(
                                    "שירות הנגישות אינו פעיל.",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(
                        onClick = onStatus,
                        Modifier.height(36.dp).widthIn(min = 102.dp),
                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Outlined.Tune, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("סטטוס", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onLogs,
                        Modifier.height(36.dp).widthIn(min = 68.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Outlined.History, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("יומן", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onProfiles,
                        Modifier.height(36.dp).widthIn(min = 82.dp),
                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Outlined.Apps, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("פרופילים", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (mode == AppMode.FULL && accessibility && !accessibilityHealthy) {
                    item {
                        StatusCard(
                            "שירות הנגישות לא מגיב",
                            false,
                            "Android מציג שהשירות מופעל, אבל ClickPlus לא מזהה שהוא פעיל. ייתכן שהשירות קרס.",
                            onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            actionLabel = "פתיחת הגדרות נגישות",
                        )
                    }
                }

            if (mappings.isEmpty()) {
                item { EmptyState(onAdd) }
            } else {
                items(mappings, key = { it.id }) { item ->
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
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onAdd,
                        Modifier.fillMaxWidth().height(54.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("הוספת פעולה", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                TextButton(
                    onClick = {
                        deleteTarget = null
                        onDelete(target)
                    },
                ) { Text("מחיקה") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("ביטול") }
            },
        )
    }
}







@Composable
internal fun StatusScreen(
    mappings: List<KeyActionConfig>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val mode = AdvancedRuleRepository.currentMode(context)
    val accessibilityEnabled = isAccessibilityEnabled(context)
    var accessibilityHealthy by remember { mutableStateOf(isAccessibilityServiceResponsive(context)) }
    LaunchedEffect(accessibilityEnabled) {
        while (true) {
            accessibilityHealthy = isAccessibilityServiceResponsive(context)
            delay(2000)
        }
    }
    val usage = hasUsageAccess(context)
    val notification = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val phone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val activeProfileId = AdvancedRuleRepository.activeProfileId(context)
    val repo = remember { AdvancedRuleRepository(context) }
    val activeProfile = repo.profiles().firstOrNull { it.id == activeProfileId }
    val activeRules = mappings.count { repo.getRuleMetadata(it.id).profileId == activeProfileId && it.enabled }

    Scaffold(
        topBar = {
            SimpleTopBar(
                "סטטוס השירות",
                onBack,
                "כאן רואים אם השירות עובד ומה דורש טיפול.",
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
                    if (mode == AppMode.FULL) {
                        "מצב מלא משתמש בשירות הנגישות. אם Android מציג שהשירות מופעל אבל ClickPlus לא מגיב, ייתכן שהשירות קרס."
                    } else {
                        "מצב בסיסי פועל ללא שירות נגישות ומשתמש רק ביכולות הזמינות במצב זה."
                    },
                ) {
                    Text(
                        when {
                            mode == AppMode.BASIC -> "מצב בסיסי"
                            accessibilityEnabled && accessibilityHealthy -> "מצב מלא · השירות פעיל"
                            accessibilityEnabled -> "מצב מלא · השירות לא מגיב"
                            else -> "מצב מלא · השירות כבוי"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (mode == AppMode.FULL && !accessibilityEnabled) {
                item {
                    StatusCard(
                        "שירות נגישות",
                        false,
                        "שירות הנגישות כבוי. יש להפעיל אותו ב-Android.",
                        onAction = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        actionLabel = "פתיחת הגדרות נגישות",
                    )
                }
            } else if (mode == AppMode.FULL && !accessibilityHealthy) {
                item {
                    StatusCard(
                        "שירות נגישות",
                        false,
                        "Android מסמן שהשירות מופעל, אבל ClickPlus לא מזהה פעילות של השירות. ייתכן שהוא קרס.",
                        onAction = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        actionLabel = "פתיחת הגדרות נגישות",
                    )
                }
            }

            if (mode == AppMode.FULL && Build.VERSION.SDK_INT >= 33 && !notification) {
                item {
                    StatusCard(
                        "התראות",
                        false,
                        "הרשאת התראות חסרה. לאחר האישור ההתראה של השירות תישאר שקטה.",
                    )
                }
            }

            if (mode == AppMode.BASIC && !usage) {
                item {
                    StatusCard(
                        "נתוני שימוש",
                        false,
                        "נדרשת גישה זו כאשר פעולה במצב בסיסי צריכה לזהות את האפליקציה שמוצגת על המסך.",
                        onAction = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        actionLabel = "פתיחת גישת נתוני שימוש",
                    )
                }
            }

            if (mappings.any {
                    it.actionType == ActionType.SYSTEM &&
                        (it.systemActionId == SystemActionPreset.ANSWER_CALL.id ||
                            it.systemActionId == SystemActionPreset.DECLINE_CALL.id)
                } && !phone
            ) {
                item {
                    StatusCard(
                        "הרשאת טלפון",
                        false,
                        "אחת הפעולות שבחרת דורשת הרשאת טלפון.",
                    )
                }
            }

            item {
                SettingCard(
                    "פרופיל פעיל",
                    "רק פרופיל אחד יכול להיות פעיל בכל רגע.",
                ) {
                    Text(
                        activeProfile?.name ?: "כללי",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("פעולות פעילות בפרופיל: $activeRules")
                }
            }

            item {
                SettingCard("פעולות", "") {
                    Text("סה״כ פעולות: " + mappings.size)
                    Text("פעולות מופעלות: " + mappings.count { it.enabled })
                }
            }
        }
    }
}


@Composable
internal fun LogsScreen(
    mappings: List<KeyActionConfig>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(AdvancedRuleRepository.logs(context)) }
    var showClearConfirm by remember { mutableStateOf(false) }
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowForward, "חזרה")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(Icons.Outlined.Delete, "ניקוי")
                    }
                },
            )
        },
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("אין אירועים להצגה עדיין.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(logs, key = { it.id }) { log ->
                    val rule = mappings.firstOrNull { it.id == log.ruleId }
                    val repo = remember(log.ruleId) { AdvancedRuleRepository(context) }
                    val profileName = rule?.let { repo.getRuleMetadata(it.id).profileId }
                        ?.let { id -> repo.profiles().firstOrNull { profile -> profile.id == id }?.name }
                    val isManual = log.detail == "בדיקה ידנית" || log.triggerLabel == "בדיקה ידנית"
                    val triggerText = log.triggerLabel.ifBlank {
                        when {
                            isManual -> "בדיקה ידנית"
                            rule?.triggerType == TriggerType.APP_ENTRY ->
                                "כניסה ל-" + rule.triggerAppName.ifBlank { "האפליקציה שנבחרה" }
                            rule != null -> "לחיצות כניסה לקליק פלוס"
                            else -> "הפעלה"
                        }
                    }
                    val actionName = log.actionLabel.ifBlank {
                        rule?.let { ActionTextFormatter.actionLabel(it, profileName) } ?: "הפעולה"
                    }
                    val intendedAction = log.actionDetails.ifBlank {
                        rule?.let { ActionTextFormatter.actionDetails(it, profileName) }
                            ?: log.detail.ifBlank { "לא נמסר פירוט" }
                    }
                    val actual = log.actualAction.ifBlank {
                        when {
                            log.success == true -> "הפעולה בוצעה"
                            log.success == false -> "הפעולה לא בוצעה"
                            else -> log.detail.ifBlank { "ממתין לתוצאה" }
                        }
                    }

                    val (status, bg, fg) = when (log.success) {
                        true -> Triple("הצליח", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        false -> Triple("נכשל", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                        null -> Triple("ממתין", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }

                    OutlinedCard(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column {
                                    Text(
                                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(log.timestamp)),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Surface(shape = RoundedCornerShape(9.dp), color = bg) {
                                    Text(
                                        status,
                                        color = fg,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    )
                                }
                            }
                            Text(
                                "הטריגר המפעיל-$triggerText",
                                fontWeight = FontWeight.Bold,
                            )
                            Text("הפעולה-$actionName: $intendedAction")
                            Text("מה קרה בפועל-$actual")
                            Text(
                                "סטטוס: " + when (log.success) {
                                    true -> "הפעולה הצליחה"
                                    false -> "הפעולה לא הצליחה"
                                    null -> "הפעולה בביצוע"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (log.success == false && log.failureReason.isNotBlank()) {
                                Text(
                                    "סיבת כישלון: " + log.failureReason,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("לנקות את היומן?") },
            text = { Text("כל רשומות היומן יימחקו.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AdvancedRuleRepository.clearLogs(context)
                        logs = emptyList()
                        showClearConfirm = false
                    },
                ) { Text("ניקוי") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("ביטול") }
            },
        )
    }
}
@Composable
internal fun ProfilesScreen(
    mappings: List<KeyActionConfig>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { AdvancedRuleRepository(context) }
    var profiles by remember { mutableStateOf(repo.profiles()) }
    var activeId by remember { mutableStateOf(AdvancedRuleRepository.activeProfileId(context)) }
    var newProfileDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ClickPlusProfile?>(null) }

    fun saveProfileList(next: List<ClickPlusProfile>) {
        val normalized = if (next.any { it.id == "default" }) next else listOf(ClickPlusProfile("default", "כללי", true)) + next
        profiles = normalized
        repo.saveProfiles(normalized)
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
                        "רק פרופיל אחד פעיל בכל רגע. פעולה משויכת לפרופיל מסוים ותפעל רק כשהוא פעיל."
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
                    "פרופיל מושבת לא ניתן לבחור כפרופיל פעיל.",
                ) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        profiles.forEach { profile ->
                            ChoiceChip(
                                selected = activeId == profile.id,
                                enabled = profile.enabled,
                                onClick = {
                                    if (profile.enabled) {
                                        activeId = profile.id
                                        AdvancedRuleRepository.setActiveProfileId(context, profile.id)
                                    }
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
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(profile.name, fontWeight = FontWeight.Bold)
                            Text("$count פעולות משויכות", style = MaterialTheme.typography.bodySmall)
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (profile.id == activeId) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        "פעיל",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    Spacer(Modifier.size(22.dp))
                                }
                                Switch(
                                    checked = profile.enabled,
                                    onCheckedChange = {
                                        val nextEnabled = it
                                        if (!nextEnabled && activeId == profile.id) return@Switch
                                        saveProfileList(profiles.map { p -> if (p.id == profile.id) p.copy(enabled = nextEnabled) else p })
                                    },
                                )
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
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
                        saveProfileList(profiles + ClickPlusProfile(name = name))
                    }
                    newProfileDialog = false
                }) { Text("הוספה") }
            },
            dismissButton = { TextButton(onClick = { newProfileDialog = false }) { Text("ביטול") } },
        )
    }

    deleteTarget?.let { target ->
        val count = mappings.count { repo.getRuleMetadata(it.id).profileId == target.id }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("למחוק את הפרופיל?") },
            text = {
                Text(
                    if (count > 0) {
                        "לפרופיל הזה משויכות $count פעולות. לפני המחיקה הן יועברו לפרופיל \"כללי\"."
                    } else {
                        "הפרופיל יימחק."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (count > 0) {
                        mappings
                            .filter { repo.getRuleMetadata(it.id).profileId == target.id }
                            .forEach { rule ->
                                repo.saveRuleMetadata(rule.id, repo.getRuleMetadata(rule.id).copy(profileId = "default"))
                            }
                    }
                    val wasActive = activeId == target.id
                    saveProfileList(profiles.filterNot { it.id == target.id })
                    if (wasActive) {
                        activeId = "default"
                        AdvancedRuleRepository.setActiveProfileId(context, "default")
                    }
                    deleteTarget = null
                }) { Text("מחיקה") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("ביטול") }
            },
        )
    }
}

@Composable
internal fun SettingsScreen(
    timeout: Long,
    actionDelay: Long,
    showTapCount: Boolean,
    tapCountX: Int,
    tapCountY: Int,
    tapCountSize: Int,
    currentMode: AppMode,
    themeMode: String,
    onBack: () -> Unit,
    onTimeout: (Long) -> Unit,
    onActionDelay: (Long) -> Unit,
    onShowTapCount: (Boolean) -> Unit,
    onTapCountX: (Int) -> Unit,
    onTapCountY: (Int) -> Unit,
    onTapCountSize: (Int) -> Unit,
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
                    Text(ActionTextFormatter.durationMs(timeout), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Text(ActionTextFormatter.durationMs(actionDelay), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    "כאשר החיווי פעיל, מספר האירועים המזוהים מוצג על המסך. אפשר להגדיר מיקום וגודל.",
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("הצג חיווי", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(checked = showTapCount, onCheckedChange = onShowTapCount)
                    }
                    Spacer(Modifier.height(6.dp))
                    val tapCountXFromRight = (100 - tapCountX).coerceIn(0, 100)
                    Text("מיקום אופקי: " + tapCountXFromRight + "% מימין")
                    Slider(
                        value = tapCountXFromRight.toFloat(),
                        onValueChange = { onTapCountX((100 - it.toInt()).coerceIn(0, 100)) },
                        valueRange = 0f..100f,
                    )
                    Text("מיקום אנכי: " + tapCountY + "% מלמעלה")
                    Slider(
                        value = tapCountY.toFloat(),
                        onValueChange = { onTapCountY(it.toInt()) },
                        valueRange = 0f..100f,
                    )
                    Text("גודל החיווי: " + tapCountSize + "dp")
                    Slider(
                        value = tapCountSize.toFloat(),
                        onValueChange = { onTapCountSize(it.toInt()) },
                        valueRange = 32f..96f,
                        steps = 15,
                    )
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
internal fun BackupScreen(
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
    var pendingImportActions by remember { mutableIntStateOf(0) }
    var pendingImportProfiles by remember { mutableIntStateOf(0) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showResetActionsConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }
    var showResetRuntimeConfirm by remember { mutableStateOf(false) }

    fun backupSettings() = JSONObject()
        .put("tapTimeoutMs", timeout)
        .put("actionDelayMs", actionDelay)
        .put("showTapCount", showTapCount)
        .put("tapCountX", AppPreferencesRepository.tapCountXSnapshot(context))
        .put("tapCountY", AppPreferencesRepository.tapCountYSnapshot(context))
        .put("tapCountPosition", AppPreferencesRepository.tapCountPositionSnapshot(context))
        .put("mode", AdvancedRuleRepository.currentMode(context).name)
        .put("themeMode", advanced.themeMode())
        .put("activeProfileId", AdvancedRuleRepository.activeProfileId(context))

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
                val version = root.optInt("version", -1)
                if (version != 2) {
                    error("גרסת גיבוי לא נתמכת")
                }
                val actions = root.optJSONArray("mappings") ?: JSONArray()
                val profiles = root.optJSONArray("profiles") ?: JSONArray()
                pendingImport = root
                pendingImportActions = actions.length()
                pendingImportProfiles = profiles.length()
                showImportConfirm = true
            }.onFailure {
                message = "הייבוא נכשל: " + (it.message ?: "שגיאה")
            }
        }
    }

    fun applyImport(root: JSONObject) {
        scope.launch {
            runCatching {
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
                    it.optString("mode", "").takeIf { mode -> mode.isNotBlank() }?.let { mode ->
                        runCatching {
                            AdvancedRuleRepository.setMode(context, AppMode.valueOf(mode))
                        }
                    }
                    it.optString("themeMode", "").takeIf { theme -> theme.isNotBlank() }?.let { theme ->
                        advanced.saveThemeMode(theme)
                    }
                    it.optString("activeProfileId", "").takeIf { id -> id.isNotBlank() }?.let { id ->
                        AdvancedRuleRepository.setActiveProfileId(context, id)
                    }
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
            backupSettings(),
        )
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            message = "הגיבוי נשמר."
        }.onFailure { message = "הייצוא נכשל: " + (it.message ?: "שגיאה") }
    }

    fun resetActions() {
        scope.launch {
            basePrefs.saveMappings(emptyList())
            advanced.clearAllRuleMetadata()
            advanced.saveProfiles(listOf(ClickPlusProfile("default", "כללי", true)))
            AdvancedRuleRepository.setActiveProfileId(context, "default")
            message = "הפעולות אופסו."
        }
    }

    fun resetRuntimeState() {
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("background_only", false)
            .remove("pending_activation_launches")
            .remove("last_target_package")
            .apply()
        AdvancedRuleRepository.setLastExternalPackage(context, "")
        message = "מצב הכניסות והאפליקציות שנפתחו אופס."
    }

    fun resetAll() {
        scope.launch {
            basePrefs.saveMappings(emptyList())
            basePrefs.saveShowTapCount(false)
            basePrefs.saveTapTimeout(1200L)
            basePrefs.saveActionDelay(0L)
            basePrefs.saveTapCountX(50)
            basePrefs.saveTapCountY(65)
            advanced.clearAllRuleMetadata()
            advanced.saveProfiles(listOf(ClickPlusProfile("default", "כללי", true)))
            AdvancedRuleRepository.setActiveProfileId(context, "default")
            AdvancedRuleRepository.setMode(context, AppMode.FULL)
            advanced.saveThemeMode("system")
            AdvancedRuleRepository.clearLogs(context)
            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .edit()
                .remove("permission_intro_completed")
                .remove("permission_bootstrap_done")
                .apply()
            message = "כל נתוני האפליקציה אופסו."
        }
    }

    Scaffold(topBar = { SimpleTopBar("גיבוי והעברה", onBack, "כאן אפשר לשמור, לייבא או לאפס את נתוני האפליקציה.") }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingCard("ייצוא ושיתוף", "שומר פעולות, פרופילים והגדרות.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("clickplus-backup.json") }, Modifier.weight(1f)) {
                            Text("ייצוא")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = java.io.File(context.cacheDir, "clickplus-backup.json")
                                file.writeText(
                                    advanced.exportJson(
                                        JSONArray().apply { mappings.forEach { put(it.toJson()) } }.toString(),
                                        backupSettings(),
                                    )
                                )
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    context.packageName + ".fileprovider",
                                    file,
                                )
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
                        ) {
                            Text("שיתוף")
                        }
                    }
                }
            }
            item {
                SettingCard("ייבוא", "לפני ההחלפה תוצג תצוגה מקדימה של מספר הפעולות והפרופילים.") {
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        Modifier.fillMaxWidth(),
                    ) { Text("ייבוא גיבוי") }
                }
            }
            item {
                SettingCard("איפוס מצב כניסות", "מאפס את זיהוי הכניסה ואת רשימת האפליקציות שנפתחו, בלי למחוק פעולות.") {
                    OutlinedButton(
                        onClick = { showResetRuntimeConfirm = true },
                        Modifier.fillMaxWidth(),
                    ) { Text("איפוס מצב כניסות ואפליקציות שנפתחו") }
                }
            }
            item {
                SettingCard("איפוס פעולות", "מחיקת הפעולות והשיוכים שלהן, בלי לשנות הגדרות כלליות.") {
                    OutlinedButton(
                        onClick = { showResetActionsConfirm = true },
                        Modifier.fillMaxWidth(),
                    ) { Text("איפוס פעולות") }
                }
            }
            item {
                SettingCard("איפוס כל האפליקציה", "מחיקת הפעולות, ההגדרות, היומן והפרופילים וחזרה לברירת המחדל.") {
                    OutlinedButton(
                        onClick = { showResetAllConfirm = true },
                        Modifier.fillMaxWidth(),
                    ) { Text("איפוס כל האפליקציה") }
                }
            }
            if (message.isNotBlank()) {
                item {
                    Text(message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (showImportConfirm && pendingImport != null) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("לאשר ייבוא גיבוי?") },
            text = {
                Text(
                    "הקובץ מכיל $pendingImportActions פעולות ו-$pendingImportProfiles פרופילים. " +
                        "הנתונים הקיימים יוחלפו בנתוני הגיבוי."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImport?.let(::applyImport)
                    pendingImport = null
                }) { Text("אישור ייבוא") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; pendingImport = null }) { Text("ביטול") }
            },
        )
    }

    if (showResetActionsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetActionsConfirm = false },
            title = { Text("לאפס את הפעולות?") },
            text = { Text("כל הפעולות והשיוכים שלהן יימחקו.") },
            confirmButton = {
                TextButton(onClick = { showResetActionsConfirm = false; resetActions() }) { Text("איפוס") }
            },
            dismissButton = {
                TextButton(onClick = { showResetActionsConfirm = false }) { Text("ביטול") }
            },
        )
    }

    if (showResetAllConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirm = false },
            title = { Text("לאפס את כל האפליקציה?") },
            text = { Text("כל הפעולות, ההגדרות, היומן והפרופילים יוחזרו לברירת המחדל.") },
            confirmButton = {
                TextButton(onClick = { showResetAllConfirm = false; resetAll() }) { Text("איפוס מלא") }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirm = false }) { Text("ביטול") }
            },
        )
    if (showResetRuntimeConfirm) {
        AlertDialog(
            onDismissRequest = { showResetRuntimeConfirm = false },
            title = { Text("לאפס את מצב הכניסות?") },
            text = { Text("ClickPlus ישכח אילו אפליקציות היו פתוחות ויתחיל לזהות כניסות מחדש.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetRuntimeConfirm = false
                        resetRuntimeState()
                    },
                ) { Text("איפוס") }
            },
            dismissButton = {
                TextButton(onClick = { showResetRuntimeConfirm = false }) { Text("ביטול") }
            },
        )
    }

    }
}

@Composable
