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
import com.example.clickplus.data.ActivityLog
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset

class ActionExecutor(private val service: AccessibilityService) : ClickActionPerformer {
    private val handler = Handler(Looper.getMainLooper())
    override fun supports(config: KeyActionConfig): Boolean {
        return config.actionType == ActionType.APP_TAP ||
            config.actionType == ActionType.APP ||
            SystemActionPreset.entries.any { it.id == config.systemActionId }
    }

    override fun execute(config: KeyActionConfig): ActionExecutionResult {
        if (!config.enabled) return ActionExecutionResult.failure("הכלל מושבת")
        if (!supports(config)) return ActionExecutionResult.failure("פעולת המערכת אינה מוכרת")

        return try {
            when (config.actionType) {
                ActionType.APP_TAP -> executeAppTap(config)
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
                ActionType.SYSTEM -> executeSystem(config.systemActionId)
            }
        } catch (error: Throwable) {
            ActionExecutionResult.failure(
                "Android לא הצליח לבצע את הפעולה: " +
                    (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
            )
        }
    }

    private fun executeAppTap(config: KeyActionConfig): ActionExecutionResult {
        val targetPackage = config.screenTapPackage.trim()
        if (targetPackage.isBlank()) {
            return ActionExecutionResult.failure("לא נבחרה אפליקציה לביצוע הלחיצה")
        }

        val xRatio = config.screenTapXRatio.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        val yRatio = config.screenTapYRatio.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        if (xRatio == null || yRatio == null || config.screenTapXRatio < 0f || config.screenTapYRatio < 0f) {
            return ActionExecutionResult.failure("לא נלמד מיקום לחיצה תקין באפליקציה")
        }

        val intent = service.packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת או שאין לה מסך פתיחה")

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        service.startActivity(intent)

        scheduleTapWhenAppIsVisible(config, targetPackage, xRatio, yRatio)
        return ActionExecutionResult.success("האפליקציה נפתחה; הלחיצה תתבצע אוטומטית במיקום שנלמד")
    }

    private fun scheduleTapWhenAppIsVisible(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
    ) {
        val startedAt = System.currentTimeMillis()
        val timeoutMs = 6000L

        val check = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() - startedAt >= timeoutMs) {
                    logAppTapResult(config, targetPackage, false, "האפליקציה נפתחה, אך לא זוהתה בחזית בתוך 6 שניות")
                    return
                }

                val foregroundPackage = service.rootInActiveWindow?.packageName?.toString().orEmpty()
                if (foregroundPackage == targetPackage) {
                    dispatchLearnedTap(config, targetPackage, xRatio, yRatio)
                } else {
                    handler.postDelayed(this, 150L)
                }
            }
        }
        handler.post(check)
    }

    private fun dispatchLearnedTap(
        config: KeyActionConfig,
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
    ) {
        val metrics = service.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val x = (xRatio * width).coerceIn(0f, width - 1f)
        val y = (yRatio * height).coerceIn(0f, height - 1f)

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()

        val dispatched = runCatching {
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
                        )
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        logAppTapResult(config, targetPackage, false, "Android ביטל את הלחיצה באפליקציית היעד")
                    }
                },
                handler,
            )
        }.getOrElse {
            logAppTapResult(
                config,
                targetPackage,
                false,
                "לא ניתן לשלוח את הלחיצה: " + (it.message?.takeIf { message -> message.isNotBlank() } ?: it.javaClass.simpleName),
            )
            false
        }

        if (!dispatched) {
            logAppTapResult(config, targetPackage, false, "Android לא קיבל את פקודת הלחיצה")
        }
    }

    private fun logAppTapResult(
        config: KeyActionConfig,
        targetPackage: String,
        success: Boolean,
        detail: String,
    ) {
        AdvancedRuleRepository.addLog(
            service,
            ActivityLog(
                timestamp = System.currentTimeMillis(),
                type = "ACTION",
                message = if (success) "הפעולה הצליחה" else "הפעולה נכשלה",
                ruleId = config.id,
                appPackage = targetPackage,
                xRatio = config.screenTapXRatio,
                yRatio = config.screenTapYRatio,
                success = success,
                detail = detail,
            ),
        )
    }

    private fun executeSystem(actionId: String): ActionExecutionResult = when (actionId) {
        SystemActionPreset.HOME.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_HOME, "מסך הבית נפתח")
        SystemActionPreset.BACK.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_BACK, "בוצעה חזרה")
        SystemActionPreset.RECENTS.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS, "מסך היישומים האחרונים נפתח")
        SystemActionPreset.NOTIFICATIONS.id ->
            globalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "חלונית ההתראות נפתחה")
        SystemActionPreset.MEDIA_PLAY_PAUSE.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "נשלחה פקודת נגן / השהה")
        SystemActionPreset.MEDIA_NEXT.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "נשלחה פקודת השיר הבא")
        SystemActionPreset.MEDIA_PREVIOUS.id ->
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "נשלחה פקודת השיר הקודם")
        SystemActionPreset.VOLUME_UP.id ->
            adjustVolume(AudioManager.ADJUST_RAISE, "עוצמת השמע הוגברה")
        SystemActionPreset.VOLUME_DOWN.id ->
            adjustVolume(AudioManager.ADJUST_LOWER, "עוצמת השמע הונמכה")
        SystemActionPreset.SETTINGS.id ->
            openSettings(Settings.ACTION_SETTINGS, "מסך ההגדרות נפתח")
        SystemActionPreset.DIALER.id ->
            openSettings(Intent.ACTION_DIAL, "החייגן נפתח")
        else -> ActionExecutionResult.failure("פעולת המערכת אינה מוכרת")
    }

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
        audio.adjustSuggestedStreamVolume(
            direction,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI
        )
        return ActionExecutionResult.success(reason)
    }
}
