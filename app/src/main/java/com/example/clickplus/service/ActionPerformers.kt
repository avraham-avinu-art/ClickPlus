package com.example.clickplus.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset

data class ActionExecutionResult(
    val success: Boolean,
    val reason: String = "",
    val pending: Boolean = false,
) {
    companion object {
        fun success(reason: String = "הפעולה בוצעה בהצלחה") = ActionExecutionResult(true, reason)
        fun pending(reason: String) = ActionExecutionResult(true, reason, pending = true)
        fun failure(reason: String) = ActionExecutionResult(false, reason)
    }
}

interface ClickActionPerformer {
    fun execute(config: KeyActionConfig): ActionExecutionResult
    fun supports(config: KeyActionConfig): Boolean
}

class BasicActionPerformer(private val context: Context) : ClickActionPerformer {
    private val supportedSystemActions = setOf(
        SystemActionPreset.MEDIA_PLAY_PAUSE.id,
        SystemActionPreset.MEDIA_NEXT.id,
        SystemActionPreset.MEDIA_PREVIOUS.id,
        SystemActionPreset.VOLUME_UP.id,
        SystemActionPreset.VOLUME_DOWN.id,
        SystemActionPreset.SETTINGS.id,
        SystemActionPreset.DIALER.id,
    )

    override fun supports(config: KeyActionConfig): Boolean {
        return config.actionType == ActionType.APP ||
            config.systemActionId in supportedSystemActions
    }

    override fun execute(config: KeyActionConfig): ActionExecutionResult {
        if (!config.enabled) return ActionExecutionResult.failure("הכלל מושבת")
        if (!supports(config)) {
            return ActionExecutionResult.failure("הפעולה אינה נתמכת במצב Basic")
        }

        return try {
            when (config.actionType) {
                ActionType.APP -> {
                    if (config.targetPackage.isBlank()) {
                        return ActionExecutionResult.failure("לא נבחרה אפליקציית יעד")
                    }
                    val intent = context.packageManager.getLaunchIntentForPackage(config.targetPackage)
                        ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת או שאין לה מסך פתיחה")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                    ActionExecutionResult.success("אפליקציית היעד נפתחה")
                }
                ActionType.SYSTEM -> when (config.systemActionId) {
                    SystemActionPreset.MEDIA_PLAY_PAUSE.id ->
                        media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "נשלחה פקודת נגן / השהה")
                    SystemActionPreset.MEDIA_NEXT.id ->
                        media(KeyEvent.KEYCODE_MEDIA_NEXT, "נשלחה פקודת השיר הבא")
                    SystemActionPreset.MEDIA_PREVIOUS.id ->
                        media(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "נשלחה פקודת השיר הקודם")
                    SystemActionPreset.VOLUME_UP.id ->
                        volume(AudioManager.ADJUST_RAISE, "עוצמת השמע הוגברה")
                    SystemActionPreset.VOLUME_DOWN.id ->
                        volume(AudioManager.ADJUST_LOWER, "עוצמת השמע הונמכה")
                    SystemActionPreset.SETTINGS.id -> {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        ActionExecutionResult.success("מסך ההגדרות נפתח")
                    }
                    SystemActionPreset.DIALER.id -> {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        ActionExecutionResult.success("החייגן נפתח")
                    }
                    else -> ActionExecutionResult.failure("פעולת המערכת אינה נתמכת במצב Basic")
                }
            }
        } catch (error: Throwable) {
            ActionExecutionResult.failure(
                "Android לא הצליח לבצע את הפעולה: " +
                    (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
            )
        }
    }

    private fun media(code: Int, reason: String): ActionExecutionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return ActionExecutionResult.success(reason)
    }

    private fun volume(direction: Int, reason: String): ActionExecutionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.adjustSuggestedStreamVolume(
            direction,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI
        )
        return ActionExecutionResult.success(reason)
    }
}
