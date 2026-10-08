@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.clickplus.ui

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
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
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
        val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        val dotColor = MaterialTheme.colorScheme.primary
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val px = size.width * x.coerceIn(0f, 1f)
            val py = size.height * y.coerceIn(0f, 1f)
            drawCircle(
                ringColor,
                radius = 30f,
                center = Offset(px, py),
            )
            drawCircle(
                dotColor,
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
internal fun PositionPreview(xPercent: Int, yPercent: Int) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    androidx.compose.foundation.Canvas(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp, max = 180.dp)
            .background(surfaceVariant, RoundedCornerShape(16.dp))
    ) {
        val x = size.width * xPercent.coerceIn(0, 100) / 100f
        val y = size.height * yPercent.coerceIn(0, 100) / 100f
        drawLine(
            outline.copy(alpha = 0.35f),
            Offset(x, 0f),
            Offset(x, size.height),
            strokeWidth = 1.5f,
        )
        drawLine(
            outline.copy(alpha = 0.35f),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.5f,
        )
        drawCircle(
            primary.copy(alpha = 0.18f),
            radius = 22.dp.toPx(),
            center = Offset(x, y),
        )
        drawCircle(
            primary,
            radius = 9.dp.toPx(),
            center = Offset(x, y),
        )
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
    SystemActionPreset.VOLUME_STATUS -> Icons.Outlined.VolumeUp
    SystemActionPreset.BRIGHTNESS_UP,
    SystemActionPreset.BRIGHTNESS_DOWN,
    SystemActionPreset.BRIGHTNESS_SET -> Icons.Outlined.Brightness6
    SystemActionPreset.SETTINGS -> Icons.Outlined.Settings
    SystemActionPreset.WIFI_SETTINGS -> Icons.Outlined.Wifi
    SystemActionPreset.BLUETOOTH_SETTINGS -> Icons.Outlined.Bluetooth
    SystemActionPreset.DISPLAY_SETTINGS -> Icons.Outlined.DisplaySettings
    SystemActionPreset.SOUND_SETTINGS -> Icons.Outlined.VolumeUp
    SystemActionPreset.BATTERY_SETTINGS -> Icons.Outlined.BatteryStd
    SystemActionPreset.APP_SETTINGS -> Icons.Outlined.Apps
    SystemActionPreset.DIALER -> Icons.Outlined.Call
    SystemActionPreset.ANSWER_CALL -> Icons.Outlined.Call
    SystemActionPreset.DECLINE_CALL -> Icons.Outlined.Call
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
