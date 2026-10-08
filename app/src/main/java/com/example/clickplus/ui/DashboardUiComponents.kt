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
import androidx.compose.material.icons.outlined.Contacts
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

@Composable
internal fun EmptyState(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Add, null, Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            Text("עדיין אין פעולות", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "צור את הפעולה הראשונה שלך לפי כניסה או לפי לחיצה אוטומטית.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd) { Text("יצירת הפעולה הראשונה") }
        }
    }
}

@Composable
internal fun RuleCard(
    item: KeyActionConfig,
    profileName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    item.name.ifBlank { "פעולה" },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.contextSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.actionSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = item.enabled,
                onCheckedChange = onEnabledChange,
            )
            IconButton(onClick = onDelete, Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Delete, "מחיקה")
            }
        }
    }
}

@Composable
internal fun AssistSummaryChip(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun PermissionIntroScreen(
    onEnter: () -> Unit,
) {
    val context = LocalContext.current
    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val phoneLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Surface(
                    Modifier.size(72.dp),
                    CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
            item {
                Text(
                    "הגדרה ראשונית",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    "כל כפתור פותח את מסך Android המתאים. אפשר לאשר רק את ההרשאות שנדרשות לפעולות שלך; אין צורך לאשר הכול מראש.",
                    textAlign = TextAlign.Center,
                )
            }
            item {
                SettingCard("הרשאות", "בחר רק את ההרשאות שאתה צריך עכשיו.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                            },
                            Modifier.fillMaxWidth(),
                        ) { Text("שירות נגישות") }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            Modifier.fillMaxWidth(),
                        ) {
                            Text(if (Build.VERSION.SDK_INT >= 33) "הרשאת התראות" else "התראות – אין צורך בהרשאה בגרסה זו")
                        }

                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_PHONE_STATE,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                                }
                            },
                            Modifier.fillMaxWidth(),
                        ) { Text("הרשאת טלפון") }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            },
                            Modifier.fillMaxWidth(),
                        ) { Text("גישה לנתוני שימוש") }
                    }
                }
            }
            item {
                Button(
                    onClick = onEnter,
                    Modifier.fillMaxWidth().height(52.dp),
                ) { Text("כניסה לאפליקציה") }
            }
        }
    }
}

@Composable
internal fun StatusCard(
    title: String,
    ok: Boolean,
    detail: String,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null,
) {
    OutlinedCard(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    null,
                    tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            Text(detail, style = MaterialTheme.typography.bodySmall)
            if (onAction != null && !actionLabel.isNullOrBlank()) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
internal fun ThemeChip(
    label: String,
    value: String,
    selectedValue: String,
    onTheme: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    FilterChip(
        selected = selectedValue == value,
        onClick = { onTheme(value) },
        modifier = Modifier.height(40.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, null, Modifier.size(17.dp)) },
    )
}

@Composable
internal fun SettingCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

@Composable
internal fun LearningScreenshotPreview(
    path: String?,
    x: Float,
    y: Float,
) {
    val filePath = path?.takeIf { java.io.File(it).exists() } ?: return
    val image = remember(filePath) {
        runCatching { BitmapFactory.decodeFile(filePath)?.asImageBitmap() }.getOrNull()
    } ?: return

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 170.dp, max = 320.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
    ) {
        Image(
            bitmap = image,
            contentDescription = "צילום מסך של אפליקציית היעד",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val sourceWidth = image.width.toFloat().coerceAtLeast(1f)
            val sourceHeight = image.height.toFloat().coerceAtLeast(1f)
            val scale = minOf(size.width / sourceWidth, size.height / sourceHeight)
            val shownWidth = sourceWidth * scale
            val shownHeight = sourceHeight * scale
            val offsetX = (size.width - shownWidth) / 2f
            val offsetY = (size.height - shownHeight) / 2f
            val px = offsetX + shownWidth * x.coerceIn(0f, 1f)
            val py = offsetY + shownHeight * y.coerceIn(0f, 1f)
            drawCircle(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                radius = 30f,
                center = Offset(px, py),
            )
            drawCircle(
                MaterialTheme.colorScheme.primary,
                radius = 10f,
                center = Offset(px, py),
            )
        }
        Surface(
            Modifier.align(Alignment.TopEnd).padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ) {
            Text(
                "צילום מסך · המיקום שנלמד",
                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}


@Composable
internal fun ProfileActionPicker(
    selectedId: String,
    onActionSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { AdvancedRuleRepository(context) }
    val profiles = repo.profiles().filter { it.enabled }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("בחירת פרופיל", fontWeight = FontWeight.Bold)
        Text(
            "בחר את הפרופיל המסוים שאליו ClickPlus יעבור כאשר הפעולה מופעלת.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (profiles.isEmpty()) {
            Text(
                "לא נמצאו פרופילים לבחירה.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            profiles.forEach { profile ->
                val id = profileActionId(profile.id)
                ChoiceChip(
                    selected = selectedId == id,
                    onClick = { onActionSelected(id) },
                    label = if (profile.enabled) profile.name else profile.name + " · מושבת",
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
            }
        }
    }
}

internal fun systemActionIcon(action: SystemActionPreset) = when (action) {
    SystemActionPreset.HOME -> Icons.Outlined.Home
    SystemActionPreset.BACK -> Icons.Outlined.ArrowBack
    SystemActionPreset.RECENTS -> Icons.Outlined.Apps
    SystemActionPreset.NOTIFICATIONS -> Icons.Outlined.Notifications
    SystemActionPreset.LOCK_SCREEN -> Icons.Outlined.Lock
    SystemActionPreset.POWER_MENU -> Icons.Outlined.PowerSettingsNew
    SystemActionPreset.SCREENSHOT -> Icons.Outlined.PhotoCamera
    SystemActionPreset.MEDIA_STOP -> Icons.Outlined.Stop
    SystemActionPreset.MEDIA_PLAY,
    SystemActionPreset.MEDIA_RESUME -> Icons.Outlined.PlayArrow
    SystemActionPreset.MEDIA_PLAY_PAUSE -> Icons.Outlined.Pause
    SystemActionPreset.MEDIA_NEXT -> Icons.Outlined.SkipNext
    SystemActionPreset.MEDIA_PREVIOUS -> Icons.Outlined.SkipPrevious
    SystemActionPreset.MEDIA_FAST_FORWARD -> Icons.Outlined.FastForward
    SystemActionPreset.MEDIA_REWIND -> Icons.Outlined.FastRewind
    SystemActionPreset.VOLUME_UP -> Icons.Outlined.VolumeUp
    SystemActionPreset.VOLUME_DOWN -> Icons.Outlined.VolumeDown
    SystemActionPreset.VOLUME_MUTE -> Icons.Outlined.VolumeMute
    SystemActionPreset.VOLUME_STATUS -> Icons.Outlined.Info
    SystemActionPreset.BRIGHTNESS_UP -> Icons.Outlined.LightMode
    SystemActionPreset.BRIGHTNESS_DOWN -> Icons.Outlined.DarkMode
    SystemActionPreset.BRIGHTNESS_SET -> Icons.Outlined.Brightness6
    SystemActionPreset.SETTINGS -> Icons.Outlined.Settings
    SystemActionPreset.WIFI_SETTINGS -> Icons.Outlined.Wifi
    SystemActionPreset.BLUETOOTH_SETTINGS -> Icons.Outlined.Bluetooth
    SystemActionPreset.DISPLAY_SETTINGS -> Icons.Outlined.DisplaySettings
    SystemActionPreset.SOUND_SETTINGS -> Icons.Outlined.VolumeUp
    SystemActionPreset.BATTERY_SETTINGS -> Icons.Outlined.BatteryStd
    SystemActionPreset.APP_SETTINGS -> Icons.Outlined.Apps
    SystemActionPreset.DIALER -> Icons.Outlined.Apps
    SystemActionPreset.ANSWER_CALL -> Icons.Outlined.Call
    SystemActionPreset.DECLINE_CALL -> Icons.Outlined.Warning
    SystemActionPreset.DIAL_NUMBER -> Icons.Outlined.Call
    SystemActionPreset.DIAL_CONTACT -> Icons.Outlined.Contacts
}

internal fun systemCategoryIcon(category: String) = when (category) {
    "ניווט" -> Icons.Outlined.Apps
    "מכשיר" -> Icons.Outlined.PowerSettingsNew
    "נגן" -> Icons.Outlined.PlayArrow
    "ווליום" -> Icons.Outlined.VolumeUp
    "בהירות מסך" -> Icons.Outlined.Brightness6
    "הגדרות" -> Icons.Outlined.Settings
    "חייגן" -> Icons.Outlined.Call
    else -> Icons.Outlined.Tune
}

@Composable
internal fun SystemActionPicker(
    selectedId: String,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onActionSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val systemActions = SystemActionPreset.entries
    val categories = systemActions.map { it.categoryHebrew }.distinct()
    val selected = selectedCategory
    if (selected == null) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("בחר קטגוריה", fontWeight = FontWeight.Bold)
            categories.forEach { category ->
                OutlinedCard(
                    Modifier.fillMaxWidth().clickable { onCategorySelected(category) },
                    shape = RoundedCornerShape(13.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            systemCategoryIcon(category),
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(category, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    } else {
        val actions = systemActions.filter { it.categoryHebrew == selected }
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    systemCategoryIcon(selected),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(selected, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = { onCategorySelected(null) }) {
                    Text("קטגוריות")
                }
            }
            actions.forEach { action ->
                val supported = AdvancedRuleRepository.currentMode(context) != AppMode.BASIC ||
                    BasicActionPerformer(context).supports(
                        KeyActionConfig(actionType = ActionType.SYSTEM, systemActionId = action.id)
                    )
                ChoiceChip(
                    selected = selectedId == action.id,
                    enabled = supported,
                    onClick = { if (supported) onActionSelected(action.id) },
                    label = if (supported) action.titleHebrew else action.titleHebrew + " · לא זמין במצב בסיסי",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    leadingIcon = { Icon(systemActionIcon(action), null, modifier = Modifier.size(21.dp)) },
                )
            }
        }
    }
}

@Composable
internal fun EditorSectionCard(
    number: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    Modifier.size(34.dp),
                    CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            number,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider()
            content()
        }
    }
}

@Composable
internal fun EditorSliderRow(
    title: String,
    valueText: String,
    description: String = "",
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    valueText,
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (description.isNotBlank()) {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

internal data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@Composable
internal fun AppPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (InstalledApp) -> Unit,
) {
    val context = LocalContext.current
    val apps = remember {
        runCatching {
            context.packageManager.getInstalledApplications(0)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .mapNotNull { info ->
                    runCatching {
                        context.packageManager.getLaunchIntentForPackage(info.packageName) ?: return@runCatching null
                        InstalledApp(
                            packageName = info.packageName,
                            label = context.packageManager.getApplicationLabel(info).toString(),
                            icon = runCatching { info.loadIcon(context.packageManager) }.getOrNull(),
                        )
                    }.getOrNull()
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.ROOT) }
                .toList()
        }.getOrDefault(emptyList())
    }
    var query by remember { mutableStateOf("") }
    val filtered = apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("חיפוש אפליקציה") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
            )
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 520.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(app) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(Modifier.size(42.dp), CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            val image = remember(app.packageName) {
                                app.icon?.let { runCatching { it.toBitmap(42, 42).asImageBitmap() }.getOrNull() }
                            }
                            if (image != null) {
                                Image(image, app.label, Modifier.fillMaxSize().padding(7.dp))
                            } else {
                                Icon(Icons.Outlined.Apps, null, Modifier.padding(9.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileSelector(
    profiles: List<ClickPlusProfile>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("פרופיל לפעולה", fontWeight = FontWeight.Medium)
        Text(
            "הפעולה תפעל רק כשהפרופיל הזה פעיל. פרופיל מושבת אינו זמין לשיוך.",
            style = MaterialTheme.typography.bodySmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            profiles.forEach { profile ->
                ChoiceChip(
                    selected = selectedId == profile.id,
                    enabled = profile.enabled,
                    onClick = { if (profile.enabled) onSelect(profile.id) },
                    label = if (profile.enabled) profile.name else profile.name + " · מושבת",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
internal fun PointEditor(
    x: Float,
    y: Float,
    toleranceX: Float,
    toleranceY: Float,
    onChange: (Float, Float) -> Unit,
) {
    val safeX = x.coerceIn(0f, 1f)
    val safeY = y.coerceIn(0f, 1f)
    val safeTx = toleranceX.coerceIn(0.01f, 0.25f)
    val safeTy = toleranceY.coerceIn(0.01f, 0.25f)
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("תצוגה מקדימה · גרור את הנקודה למיקום המדויק", style = MaterialTheme.typography.bodySmall)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 170.dp, max = 320.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
        ) {
            androidx.compose.foundation.Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .pointerInput(safeX, safeY) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val px = safeX * size.width
                            val py = safeY * size.height
                            val dx = down.position.x - px
                            val dy = down.position.y - py
                            if (dx * dx + dy * dy > 40.dp.toPx() * 40.dp.toPx()) return@awaitEachGesture
                            down.consume()
                            drag(down.id) { change ->
                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                onChange(nx, ny)
                                change.consume()
                            }
                        }
                    }
            ) {
                val px = safeX * size.width
                val py = safeY * size.height
                drawRect(
                    outlineColor.copy(alpha = 0.35f),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))),
                )
                drawRect(
                    primaryColor.copy(alpha = 0.15f),
                    topLeft = Offset(
                        ((safeX - safeTx).coerceAtLeast(0f)) * size.width,
                        ((safeY - safeTy).coerceAtLeast(0f)) * size.height,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        (safeTx * 2f).coerceAtMost(1f) * size.width,
                        (safeTy * 2f).coerceAtMost(1f) * size.height,
                    ),
                )
                drawCircle(primaryColor.copy(alpha = 0.18f), radius = 30f, center = Offset(px, py))
                drawCircle(primaryColor, radius = 11f, center = Offset(px, py))
            }
        }
    }
}

@Composable
internal fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    compactText: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val borderColor = if (!enabled) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    } else if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val labelColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .heightIn(min = 42.dp)
             .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            borderColor,
        ),
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                leadingIcon?.invoke()
                if (leadingIcon != null) Spacer(Modifier.width(5.dp))
                if (selected) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    style = if (compactText) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
internal fun SimpleTopBar(
    title: String,
    onBack: () -> Unit,
    helpText: String = "כאן נמצא הסבר קצר על אפשרויות המסך.",
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "חזרה") } },
        actions = { HelpIconButton("הסבר על " + title, helpText) },
    )
}

@Composable
internal fun HelpIconButton(title: String, text: String) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Outlined.Info, "הסבר") }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("הבנתי") } },
        )
    }
}

internal fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.split(':').any { it.contains(context.packageName, true) }
}

internal fun isAccessibilityServiceResponsive(context: Context): Boolean {
    if (!isAccessibilityEnabled(context)) return false
    val prefs = context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
    val alive = prefs.getBoolean("accessibility_service_alive", false)
    val heartbeat = prefs.getLong("accessibility_service_heartbeat", 0L)
    return alive && heartbeat > 0L && System.currentTimeMillis() - heartbeat < 5000L
}

internal fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    return runCatching {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
}
