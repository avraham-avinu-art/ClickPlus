package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.profileIdFromActionId
import com.example.clickplus.data.SystemActionPreset

class ActionExecutor(private val service: AccessibilityService) : ClickActionPerformer {
    private val handler = Handler(Looper.getMainLooper())
    override fun supports(config: KeyActionConfig): Boolean {
        return config.actionType == ActionType.APP_TAP ||
            config.actionType == ActionType.MULTI_POINT_TAP ||
            config.actionType == ActionType.APP ||
            config.actionType == ActionType.PROFILE ||
            SystemActionPreset.entries.any { it.id == config.systemActionId }
    }

    override fun execute(config: KeyActionConfig): ActionExecutionResult {
        if (!config.enabled) return ActionExecutionResult.failure("הכלל מושבת")
        if (!supports(config)) return ActionExecutionResult.failure("פעולת המערכת אינה מוכרת")

        return try {
            when (config.actionType) {
                ActionType.APP_TAP -> executeAppTap(config)
                ActionType.MULTI_POINT_TAP -> executeMultiPointTap(config)
                ActionType.APP -> {
                    if (config.targetPackage.isBlank()) {
                        return ActionExecutionResult.failure("לא נבחרה אפליקציית יעד")
                    }
                    val intent = service.packageManager.getLaunchIntentForPackage(config.targetPackage)
                        ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת או שאין לה מסך פתיחה")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    service.startActivity(intent)
                    ActionExecutionResult.success("אפליקציית היעד נפתחה")
                }
                ActionType.PROFILE -> executeProfile(config.systemActionId)
                ActionType.SYSTEM -> executeSystem(
                    config.systemActionId,
                    if (config.systemActionId == SystemActionPreset.DIAL_CONTACT.id) {
                        config.contactNumber.ifBlank { config.actionParameter }
                    } else config.actionParameter,
                )
            }
        } catch (error: Throwable) {
            ActionExecutionResult.failure(
                "Android לא הצליח לבצע את הפעולה: " +
                    (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
            )
        }
    }

    private fun pointForCurrentOrientation(
        config: KeyActionConfig,
        second: Boolean,
    ): Pair<Float, Float> {
        val defaultX = if (second) config.screenTapSecondXRatio else config.screenTapXRatio
        val defaultY = if (second) config.screenTapSecondYRatio else config.screenTapYRatio
        val meta = AdvancedRuleRepository(service).getRuleMetadata(config.id)
        if (!meta.useOrientationSpecificPosition) return defaultX to defaultY

        val landscape = service.resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val x = if (second) {
            if (landscape) meta.landscapeSecondX else meta.portraitSecondX
        } else {
            if (landscape) meta.landscapeX else meta.portraitX
        }
        val y = if (second) {
            if (landscape) meta.landscapeSecondY else meta.portraitSecondY
        } else {
            if (landscape) meta.landscapeY else meta.portraitY
        }
        return if (x >= 0f && y >= 0f) x to y else defaultX to defaultY
    }

    private fun executeAppTap(config: KeyActionConfig): ActionExecutionResult {
        val targetPackage = config.screenTapPackage.trim()
        if (targetPackage.isBlank()) {
            return ActionExecutionResult.failure("לא נבחרה אפליקציה לביצוע הלחיצה")
        }

        val point = pointForCurrentOrientation(config, second = false)
        val xRatio = point.first.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        val yRatio = point.second.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        if (xRatio == null || yRatio == null || config.screenTapXRatio < 0f || config.screenTapYRatio < 0f) {
            return ActionExecutionResult.failure("לא נלמד מיקום לחיצה תקין באפליקציה")
        }

        val intent = service.packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת או שאין לה מסך פתיחה")

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        service.startActivity(intent)

        val executionId = java.util.UUID.randomUUID().toString()
        scheduleTapWhenAppIsVisible(config, targetPackage, xRatio, yRatio, executionId)
        return ActionExecutionResult.pending(
            "האפליקציה נפתחה; ממתינים להופעתה על המסך ואז תתבצע הלחיצה",
            executionId,
        )
    }

    private fun executeMultiPointTap(config: KeyActionConfig): ActionExecutionResult {
        val targetPackage = config.screenTapPackage.trim()
        if (targetPackage.isBlank()) return ActionExecutionResult.failure("לא נבחרה אפליקציה")
        val first = pointForCurrentOrientation(config, second = false)
        val second = pointForCurrentOrientation(config, second = true)
        val x1 = first.first
        val y1 = first.second
        val x2 = second.first
        val y2 = second.second
        if (x1 < 0f || y1 < 0f || x2 < 0f || y2 < 0f) {
            return ActionExecutionResult.failure("לא נלמדו שתי נקודות לחיצה")
        }
        val intent = service.packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        service.startActivity(intent)
        val executionId = java.util.UUID.randomUUID().toString()
        scheduleTwoTapsWhenAppIsVisible(config, targetPackage, x1, y1, x2, y2, executionId)
        return ActionExecutionResult.pending(
            "האפליקציה נפתחה; שתי הלחיצות יתבצעו כשהיא תהיה בחזית",
            executionId,
        )
    }

    private fun scheduleTwoTapsWhenAppIsVisible(
        config: KeyActionConfig,
        targetPackage: String,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        executionId: String,
    ) {
        val startedAt = System.currentTimeMillis()
        val check = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() - startedAt >= 6000L) {
                    logAppTapResult(config, targetPackage, false, "האפליקציה לא הופיעה בחזית בתוך 6 שניות", executionId)
                    return
                }
                if (service.rootInActiveWindow?.packageName?.toString().orEmpty() == targetPackage) {
                    val meta = AdvancedRuleRepository(service).getRuleMetadata(config.id)
                    val delayMs = (if (meta.delayMs > 0L) meta.delayMs else AppPreferencesRepository.actionDelaySnapshot(service))
                        .coerceIn(0L, 10_000L)
                    if (delayMs == 0L) {
                        dispatchTwoTaps(config, targetPackage, x1, y1, x2, y2, executionId)
                    } else {
                        handler.postDelayed(
                            { dispatchTwoTaps(config, targetPackage, x1, y1, x2, y2, executionId) },
                            delayMs,
                        )
                    }
                } else handler.postDelayed(this, 150L)
            }
        }
        handler.post(check)
    }

    private fun dispatchTwoTaps(
        config: KeyActionConfig,
        targetPackage: String,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        executionId: String,
    ) {
        val metrics = service.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val path = Path().apply {
            moveTo((x1 * width).coerceIn(0f, width - 1f), (y1 * height).coerceIn(0f, height - 1f))
        }
        val gesture1 = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()
        service.dispatchGesture(
            gesture1,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    val delay = config.screenTapIntervalMs.coerceIn(500L, 10_000L)
                    handler.postDelayed({
                        if (service.rootInActiveWindow?.packageName?.toString().orEmpty() != targetPackage) {
                            logAppTapResult(
                                config,
                                targetPackage,
                                false,
                                "אפליקציית היעד יצאה מהחזית לפני הלחיצה השנייה",
                                executionId,
                            )
                            return@postDelayed
                        }
                        val path2 = Path().apply {
                            moveTo((x2 * width).coerceIn(0f, width - 1f), (y2 * height).coerceIn(0f, height - 1f))
                        }
                        val gesture2 = GestureDescription.Builder()
                            .addStroke(GestureDescription.StrokeDescription(path2, 0L, 60L))
                            .build()
                        service.dispatchGesture(
                            gesture2,
                            object : AccessibilityService.GestureResultCallback() {
                                override fun onCompleted(gestureDescription: GestureDescription?) {
                                    logAppTapResult(config, targetPackage, true, "שתי הלחיצות בוצעו בהצלחה", executionId)
                                }
                                override fun onCancelled(gestureDescription: GestureDescription?) {
                                    logAppTapResult(config, targetPackage, false, "הלחיצה השנייה בוטלה על ידי Android", executionId)
                                }
                            },
                            handler,
                        )
                    }, delay)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    logAppTapResult(config, targetPackage, false, "הלחיצה הראשונה בוטלה על ידי Android", executionId)
                }
            },
            handler,
        )
    }

    private fun scheduleTapWhenAppIsVisible(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
        executionId: String,
    ) {
        val startedAt = System.currentTimeMillis()
        val timeoutMs = 6000L

        val check = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() - startedAt >= timeoutMs) {
                    logAppTapResult(config, targetPackage, false, "האפליקציה נפתחה, אך לא זוהתה בחזית בתוך 6 שניות", executionId)
                    return
                }

                val foregroundPackage = service.rootInActiveWindow?.packageName?.toString().orEmpty()
                if (foregroundPackage == targetPackage) {
                    val meta = AdvancedRuleRepository(service).getRuleMetadata(config.id)
                    val delayMs = (if (meta.delayMs > 0L) meta.delayMs else AppPreferencesRepository.actionDelaySnapshot(service))
                        .coerceIn(0L, 10_000L)
                    val run = {
                        val count = config.screenTapCount.coerceIn(1, 10)
                        if (count == 1) {
                            dispatchLearnedTap(config, targetPackage, xRatio, yRatio, executionId)
                        } else {
                            dispatchRepeatedTaps(
                                config,
                                targetPackage,
                                xRatio,
                                yRatio,
                                count,
                                1,
                                executionId,
                            )
                        }
                    }
                    if (delayMs == 0L) run() else handler.postDelayed(run, delayMs)
                } else {
                    handler.postDelayed(this, 150L)
                }
            }
        }
        handler.post(check)
    }

    private fun dispatchRepeatedTaps(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
        count: Int,
        index: Int,
        executionId: String,
    ) {
        if (service.rootInActiveWindow?.packageName?.toString().orEmpty() != targetPackage) {
            logAppTapResult(
                config,
                targetPackage,
                false,
                "אפליקציית היעד יצאה מהחזית במהלך סדרת הלחיצות",
                executionId,
            )
            return
        }

        val metrics = service.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val x = (xRatio * width).coerceIn(0f, width - 1f)
        val y = (yRatio * height).coerceIn(0f, height - 1f)
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()

        runCatching {
            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (index >= count) {
                            logAppTapResult(
                                config,
                                targetPackage,
                                true,
                                "כל " + count + " הלחיצות בוצעו בהצלחה",
                                executionId,
                            )
                        } else {
                            handler.postDelayed(
                                {
                                    dispatchRepeatedTaps(
                                        config,
                                        targetPackage,
                                        xRatio,
                                        yRatio,
                                        count,
                                        index + 1,
                                        executionId,
                                    )
                                },
                                config.screenTapIntervalMs.coerceIn(500L, 10_000L),
                            )
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        logAppTapResult(
                            config,
                            targetPackage,
                            false,
                            "לחיצה מספר " + index + " בוטלה על ידי Android",
                            executionId,
                        )
                    }
                },
                handler,
            )
        }.onFailure { error ->
            logAppTapResult(
                config,
                targetPackage,
                false,
                "לא ניתן לשלוח לחיצה מספר " + index + ": " +
                    (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName),
                executionId,
            )
        }
    }

    private fun dispatchLearnedTap(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
        executionId: String,
    ) {
        val metrics = service.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val x = (xRatio * width).coerceIn(0f, width - 1f)
        val y = (yRatio * height).coerceIn(0f, height - 1f)

        if (service.rootInActiveWindow?.packageName?.toString().orEmpty() != targetPackage) {
            logAppTapResult(
                config,
                targetPackage,
                false,
                "אפליקציית היעד כבר אינה בחזית בזמן הלחיצה",
                executionId,
            )
            return
        }

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()

        runCatching {
            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        logAppTapResult(
                            config,
                            targetPackage,
                            true,
                            "הלחיצה בוצעה ב-X ${(xRatio * 100f).toInt()}% · Y ${(yRatio * 100f).toInt()}%",
                            executionId,
                        )
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        logAppTapResult(config, targetPackage, false, "Android ביטל את הלחיצה באפליקציית היעד", executionId)
                    }
                },
                handler,
            )
        }.onFailure { error ->
            logAppTapResult(
                config,
                targetPackage,
                false,
                "לא ניתן לשלוח את הלחיצה: " + (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName),
                executionId,
            )
        }
    }
    private fun logAppTapResult(
        config: KeyActionConfig,
        targetPackage: String,
        success: Boolean,
        detail: String,
        executionId: String,
    ) {
        AdvancedRuleRepository.updateActionLog(
            context = service,
            logId = executionId,
            appPackage = targetPackage,
            success = success,
            detail = detail,
        )
    }


    private fun executeProfile(profileActionId: String): ActionExecutionResult {
        val selectedProfileId = profileIdFromActionId(profileActionId)
        if (selectedProfileId != null) {
            val profile = AdvancedRuleRepository(service).profiles().firstOrNull { it.id == selectedProfileId }
                ?: return ActionExecutionResult.failure("הפרופיל שנבחר אינו קיים")
            if (!profile.enabled) {
                return ActionExecutionResult.failure("הפרופיל שנבחר מושבת")
            }
            AdvancedRuleRepository.setActiveProfileId(service, profile.id)
            return ActionExecutionResult.success("עבר לפרופיל: " + profile.name)
        }

        // Backward compatibility for rules saved by older versions.
        return when (profileActionId) {
            "profile_next" ->
                ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(service, 1))
            "profile_previous" ->
                ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(service, -1))
            "profile_default" -> {
                AdvancedRuleRepository.setActiveProfileId(service, "default")
                ActionExecutionResult.success("חזר לפרופיל ברירת המחדל")
            }
            else -> ActionExecutionResult.failure("פעולת פרופיל ClickPlus אינה מוכרת")
        }
    }

    private fun executeSystem(actionId: String, actionParameter: String = ""): ActionExecutionResult = when (actionId) {
        SystemActionPreset.HOME.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_HOME, "מסך הבית נפתח")
        SystemActionPreset.BACK.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_BACK, "בוצעה חזרה")
        SystemActionPreset.RECENTS.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS, "מסך היישומים האחרונים נפתח")
        SystemActionPreset.NOTIFICATIONS.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "חלונית ההתראות נפתחה")
        SystemActionPreset.LOCK_SCREEN.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "המסך ננעל")
        SystemActionPreset.POWER_MENU.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, "תפריט הכיבוי נפתח")
        SystemActionPreset.SCREENSHOT.id ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                globalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "צילום המסך בוצע")
            } else ActionExecutionResult.failure("צילום מסך באמצעות שירות הנגישות דורש Android 11 ומעלה")
        SystemActionPreset.MEDIA_STOP.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_STOP, "עצירת השמעה")
        SystemActionPreset.MEDIA_PLAY.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY, "הפעלת השמעה")
        SystemActionPreset.MEDIA_RESUME.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY, "המשך השמעה")
        SystemActionPreset.MEDIA_PLAY_PAUSE.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "נגן / השהה")
        SystemActionPreset.MEDIA_NEXT.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "השיר הבא")
        SystemActionPreset.MEDIA_PREVIOUS.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "השיר הקודם")
        SystemActionPreset.MEDIA_FAST_FORWARD.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "הרצה קדימה")
        SystemActionPreset.MEDIA_REWIND.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND, "הרצה לאחור")
        SystemActionPreset.VOLUME_UP.id ->
            adjustVolume(AudioManager.ADJUST_RAISE, "עוצמת השמע הוגברה")
        SystemActionPreset.VOLUME_DOWN.id ->
            adjustVolume(AudioManager.ADJUST_LOWER, "עוצמת השמע הונמכה")
        SystemActionPreset.VOLUME_MUTE.id ->
            muteVolume(true, "השמע הושתק")
        SystemActionPreset.VOLUME_STATUS.id ->
            ActionExecutionResult.success("עוצמת השמע הנוכחית: " + currentVolumeText())
        SystemActionPreset.BRIGHTNESS_UP.id ->
            changeBrightness(0.10f, "הבהירות הוגברה")
        SystemActionPreset.BRIGHTNESS_DOWN.id ->
            changeBrightness(-0.10f, "הבהירות הוחלשה")
        SystemActionPreset.BRIGHTNESS_SET.id ->
            setBrightness(actionParameter, "הבהירות שונתה")
        SystemActionPreset.WIFI_SETTINGS.id ->
            openSettings(Settings.ACTION_WIFI_SETTINGS, "הגדרות Wi‑Fi נפתחו")
        SystemActionPreset.BLUETOOTH_SETTINGS.id ->
            openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "הגדרות Bluetooth נפתחו")
        SystemActionPreset.DISPLAY_SETTINGS.id ->
            openSettings(Settings.ACTION_DISPLAY_SETTINGS, "הגדרות תצוגה נפתחו")
        SystemActionPreset.SOUND_SETTINGS.id ->
            openSettings(Settings.ACTION_SOUND_SETTINGS, "הגדרות שמע נפתחו")
        SystemActionPreset.BATTERY_SETTINGS.id ->
            openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS, "הגדרות סוללה נפתחו")
        SystemActionPreset.APP_SETTINGS.id ->
            openSettings(Settings.ACTION_APPLICATION_SETTINGS, "הגדרות אפליקציות נפתחו")
        SystemActionPreset.SETTINGS.id ->
            openSettings(Settings.ACTION_SETTINGS, "מסך ההגדרות נפתח")
        SystemActionPreset.DIALER.id ->
            openSettings(Intent.ACTION_DIAL, "החייגן נפתח")
        SystemActionPreset.DIAL_NUMBER.id -> {
            val number = actionParameter.trim()
            if (number.isBlank()) ActionExecutionResult.failure("לא הוגדר מספר לחיוג")
            else openSettingsIntent(
                Intent(
                    Intent.ACTION_DIAL,
                    android.net.Uri.parse("tel:" + android.net.Uri.encode(number)),
                ),
                "מסך החיוג נפתח",
            )
        }
        SystemActionPreset.DIAL_CONTACT.id -> {
            val number = actionParameter.trim()
            if (number.isBlank()) {
                ActionExecutionResult.failure("לא נבחר איש קשר")
            } else {
                openSettingsIntent(
                    Intent(
                        Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + android.net.Uri.encode(number)),
                    ),
                    "מסך החיוג לאיש הקשר נפתח",
                )
            }
        }
        SystemActionPreset.ANSWER_CALL.id -> answerCall()
        SystemActionPreset.DECLINE_CALL.id -> declineCall()
        else -> ActionExecutionResult.failure("פעולת המערכת אינה מוכרת")
    }

    private fun openSettingsIntent(intent: Intent, reason: String): ActionExecutionResult = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        service.startActivity(intent)
        ActionExecutionResult.success(reason)
    } catch (error: Throwable) {
        ActionExecutionResult.failure("Android לא הצליח לפתוח את היעד: " + (error.message ?: "שגיאה"))
    }

    private fun muteVolume(mute: Boolean, reason: String): ActionExecutionResult {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע אינו זמין")
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
        return ActionExecutionResult.success(reason)
    }

    private fun currentVolumeText(): String {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "לא זמין"
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toString() + "/" + audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    private fun changeBrightness(delta: Float, reason: String): ActionExecutionResult {
        if (!Settings.System.canWrite(service)) return ActionExecutionResult.failure("נדרשת הרשאת שינוי הגדרות מערכת")
        val current = Settings.System.getInt(service.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val next = (current + delta * 255f).toInt().coerceIn(1, 255)
        Settings.System.putInt(service.contentResolver, Settings.System.SCREEN_BRIGHTNESS, next)
        return ActionExecutionResult.success(reason)
    }

    private fun setBrightness(value: String, reason: String): ActionExecutionResult {
        if (!Settings.System.canWrite(service)) return ActionExecutionResult.failure("נדרשת הרשאת שינוי הגדרות מערכת")
        val percent = value.toIntOrNull()?.coerceIn(1, 100)
            ?: return ActionExecutionResult.failure("יש להגדיר בהירות בין 1 ל-100")
        Settings.System.putInt(service.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (percent * 255f / 100f).toInt())
        return ActionExecutionResult.success(reason)
    }

    private fun answerCall(): ActionExecutionResult = runCatching {
        val telecom = service.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        if (!service.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS).equals(android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            return ActionExecutionResult.failure("נדרשת הרשאת מענה לשיחות")
        }
        telecom.acceptRingingCall()
        ActionExecutionResult.success("השיחה נענתה")
    }.getOrElse { ActionExecutionResult.failure("לא ניתן לענות לשיחה: " + (it.message ?: "שגיאה")) }

    private fun declineCall(): ActionExecutionResult = runCatching {
        val telecom = service.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        if (!service.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS).equals(android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            return ActionExecutionResult.failure("נדרשת הרשאת מענה לשיחות")
        }
        @Suppress("DEPRECATION")
        telecom.endCall()
        ActionExecutionResult.success("השיחה נדחתה")
    }.getOrElse { ActionExecutionResult.failure("לא ניתן לדחות שיחה: " + (it.message ?: "שגיאה")) }

    private fun globalAction(action: Int, reason: String): ActionExecutionResult {
        return if (service.performGlobalAction(action)) {
            ActionExecutionResult.success(reason)
        } else {
            ActionExecutionResult.failure("Android לא אישר את פעולת המערכת")
        }
    }

    private fun openSettings(action: String, reason: String): ActionExecutionResult = try {
        service.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        ActionExecutionResult.success(reason)
    } catch (error: Throwable) {
        ActionExecutionResult.failure(
            "Android לא הצליח לפתוח את המסך: " +
                (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
        )
    }

    private fun dispatchMediaKey(keyCode: Int, reason: String): ActionExecutionResult {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ActionExecutionResult.success(reason)
    }

    private fun adjustVolume(direction: Int, reason: String): ActionExecutionResult {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI,
        )
        return ActionExecutionResult.success(reason)
    }
}
