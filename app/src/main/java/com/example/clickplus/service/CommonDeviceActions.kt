package com.example.clickplus.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent

/**
 * Device operations that behave the same in Basic and Full modes.
 * Full mode-specific accessibility global actions remain in
 * AccessibilitySystemActionExecutor.
 */
class CommonDeviceActions(private val context: Context) {
    fun media(keyCode: Int, reason: String): ActionExecutionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ActionExecutionResult.success(reason)
    }

    fun adjustVolume(direction: Int, reason: String): ActionExecutionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI,
        )
        return ActionExecutionResult.success(reason)
    }

    fun muteVolume(mute: Boolean, reason: String): ActionExecutionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
            AudioManager.FLAG_SHOW_UI,
        )
        return ActionExecutionResult.success(reason)
    }

    fun currentVolumeText(): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "לא זמין"
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toString() + "/" +
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toString()
    }

    fun changeBrightness(delta: Float, reason: String): ActionExecutionResult {
        if (!Settings.System.canWrite(context)) {
            return ActionExecutionResult.failure("נדרשת הרשאה לשינוי הגדרות מערכת.")
        }
        return runCatching {
            val current = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128,
            )
            val next = (current + delta * 255f).toInt().coerceIn(1, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, next)
            ActionExecutionResult.success(reason)
        }.getOrElse {
            ActionExecutionResult.failure("לא ניתן לשנות את בהירות המסך")
        }
    }

    fun setBrightness(value: String, reason: String): ActionExecutionResult {
        val percent = value.toIntOrNull()?.coerceIn(1, 100)
            ?: return ActionExecutionResult.failure("יש להגדיר בהירות בין 1 ל-100")
        if (!Settings.System.canWrite(context)) {
            return ActionExecutionResult.failure("נדרשת הרשאה לשינוי הגדרות מערכת.")
        }
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (percent * 255f / 100f).toInt().coerceIn(1, 255),
            )
            ActionExecutionResult.success(reason + " ל-" + percent + "%")
        }.getOrElse {
            ActionExecutionResult.failure("לא ניתן לשנות את בהירות המסך")
        }
    }

    fun openSettings(action: String, reason: String): ActionExecutionResult =
        openIntent(Intent(action), reason)

    fun openIntent(intent: Intent, reason: String): ActionExecutionResult = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ActionExecutionResult.success(reason)
    } catch (error: Throwable) {
        ActionExecutionResult.failure(
            "Android לא הצליח לפתוח את היעד: " +
                (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
        )
    }
}
