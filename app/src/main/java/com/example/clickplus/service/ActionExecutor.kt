package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset

class ActionExecutor(private val service: AccessibilityService) : ClickActionPerformer {
    override fun supports(config: KeyActionConfig): Boolean {
        return config.actionType == ActionType.APP ||
            SystemActionPreset.entries.any { it.id == config.systemActionId }
    }

    override fun execute(config: KeyActionConfig): ActionExecutionResult {
        if (!config.enabled) return ActionExecutionResult.failure("הכלל מושבת")
        if (!supports(config)) return ActionExecutionResult.failure("פעולת המערכת אינה מוכרת")

        return try {
            when (config.actionType) {
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
