@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.clickplus.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.clickplus.data.ActionTextFormatter
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppMode
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset
import com.example.clickplus.data.TriggerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
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
    var basicMonitorHealthy by remember { mutableStateOf(isBasicForegroundMonitorResponsive(context)) }
    LaunchedEffect(accessibility, mode) {
        while (true) {
            accessibilityHealthy = isAccessibilityServiceResponsive(context)
            basicMonitorHealthy = isBasicForegroundMonitorResponsive(context)
            delay(2000)
        }
    }
    val needsBasicAppEntryMonitor = mode == AppMode.BASIC && hasUsageAccess(context) &&
        mappings.any { it.enabled && it.triggerType == TriggerType.APP_ENTRY }
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
            }

            if (mode == AppMode.FULL && accessibility && !accessibilityHealthy) {
                item {
                    StatusCard(
                        "שירות הנגישות לא מגיב",
                        false,
                        "Android מציג שהשירות מופעל, אבל ClickPlus לא מזהה פעילות של השירות. ייתכן שהשירות קרס.",
                        onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        actionLabel = "פתיחת הגדרות נגישות",
                    )
                }
            }

            if (needsBasicAppEntryMonitor && !basicMonitorHealthy) {
                item {
                    StatusCard(
                        "זיהוי כניסות לאפליקציות לא מגיב",
                        false,
                        "מצב בסיסי דורש שירות מעקב שקט וגישת נתוני שימוש. ייתכן שהשירות נעצר.",
                        onAction = {
                            val intent = Intent(context, com.example.clickplus.service.BasicForegroundMonitorService::class.java)
                            runCatching { ContextCompat.startForegroundService(context, intent) }
                        },
                        actionLabel = "הפעלת שירות הזיהוי",
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
    var basicMonitorHealthy by remember { mutableStateOf(isBasicForegroundMonitorResponsive(context)) }
    LaunchedEffect(mode, usage, mappings) {
        while (true) {
            basicMonitorHealthy = isBasicForegroundMonitorResponsive(context)
            delay(2000)
        }
    }
    val notification = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val phoneStatePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val answerCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
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

            if (
                mode == AppMode.BASIC &&
                usage &&
                mappings.any { it.enabled && it.triggerType == TriggerType.APP_ENTRY } &&
                !basicMonitorHealthy
            ) {
                item {
                    StatusCard(
                        "שירות זיהוי האפליקציות לא מגיב",
                        false,
                        "גישת נתוני שימוש קיימת, אבל שירות המעקב לא מדווח פעילות. ייתכן שהוא קרס או נעצר.",
                        onAction = {
                            val intent = Intent(context, com.example.clickplus.service.BasicForegroundMonitorService::class.java)
                            runCatching { ContextCompat.startForegroundService(context, intent) }
                        },
                        actionLabel = "הפעלת שירות הזיהוי",
                    )
                }
            }

            if (mappings.any {
                    it.actionType == ActionType.SYSTEM &&
                        (it.systemActionId == SystemActionPreset.ANSWER_CALL.id ||
                            it.systemActionId == SystemActionPreset.DECLINE_CALL.id)
                } && !answerCallPermission
            ) {
                item {
                    StatusCard(
                        "הרשאת מענה/דחיית שיחה",
                        false,
                        "אחת הפעולות שבחרת דורשת את הרשאת מענה לשיחות של Android.",
                    )
                }
            }

            if (mappings.any { it.contextConditionType == ContextConditionType.RINGING } && !phoneStatePermission) {
                item {
                    StatusCard(
                        "זיהוי צלצול",
                        false,
                        "אחת הפעולות תלויה בזיהוי צלצול ודורשת הרשאת מצב טלפון.",
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
                                "הטריגר המפעיל: $triggerText · $actionName",
                                fontWeight = FontWeight.Bold,
                            )
                            Text("הפעולה: $intendedAction")
                            Text("בפועל: $actual")
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
