package com.example.clickplus.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AdvancedRuleRepository
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
        SystemActionPreset.MEDIA_STOP.id,
        SystemActionPreset.MEDIA_PLAY.id,
        SystemActionPreset.MEDIA_RESUME.id,
        SystemActionPreset.MEDIA_PLAY_PAUSE.id,
        SystemActionPreset.MEDIA_NEXT.id,
        SystemActionPreset.MEDIA_PREVIOUS.id,
        SystemActionPreset.MEDIA_FAST_FORWARD.id,
        SystemActionPreset.MEDIA_REWIND.id,
        SystemActionPreset.VOLUME_UP.id,
        SystemActionPreset.VOLUME_DOWN.id,
        SystemActionPreset.VOLUME_MUTE.id,
        SystemActionPreset.VOLUME_STATUS.id,
        SystemActionPreset.BRIGHTNESS_UP.id,
        SystemActionPreset.BRIGHTNESS_DOWN.id,
        SystemActionPreset.BRIGHTNESS_SET.id,
        SystemActionPreset.SETTINGS.id,
        SystemActionPreset.WIFI_SETTINGS.id,
        SystemActionPreset.BLUETOOTH_SETTINGS.id,
        SystemActionPreset.DISPLAY_SETTINGS.id,
        SystemActionPreset.SOUND_SETTINGS.id,
        SystemActionPreset.BATTERY_SETTINGS.id,
        SystemActionPreset.APP_SETTINGS.id,
        SystemActionPreset.DIALER.id,
        SystemActionPreset.ANSWER_CALL.id,
        SystemActionPreset.DECLINE_CALL.id,
        SystemActionPreset.DIAL_NUMBER.id,
        SystemActionPreset.DIAL_CONTACT.id,
        SystemActionPreset.PROFILE_NEXT.id,
        SystemActionPreset.PROFILE_PREVIOUS.id,
        SystemActionPreset.PROFILE_DEFAULT.id,
    )

    override fun supports(config: KeyActionConfig): Boolean {
        return config.actionType == ActionType.APP ||
            config.actionType == ActionType.APP_TAP ||
            config.systemActionId in supportedSystemActions
    }

    override fun execute(config: KeyActionConfig): ActionExecutionResult {
        if (!config.enabled) return ActionExecutionResult.failure("הכלל מושבת")
        if (!supports(config)) {
            return ActionExecutionResult.failure("הפעולה אינה נתמכת במצב Basic")
        }

        return try {
            when (config.actionType) {
                ActionType.MULTI_POINT_TAP -> ActionExecutionResult.failure("שתי לחיצות אוטומטיות דורשות מצב מלא עם שירות נגישות")
                ActionType.APP_TAP -> ActionExecutionResult.failure("לחיצה בתוך אפליקציה דורשת מצב מלא עם שירות נגישות")
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
                    SystemActionPreset.MEDIA_STOP.id ->
                        media(KeyEvent.KEYCODE_MEDIA_STOP, "נשלחה פקודת עצירת השמעה")
                    SystemActionPreset.MEDIA_PLAY.id ->
                        media(KeyEvent.KEYCODE_MEDIA_PLAY, "נשלחה פקודת הפעלת השמעה")
                    SystemActionPreset.MEDIA_RESUME.id ->
                        media(KeyEvent.KEYCODE_MEDIA_PLAY, "נשלחה פקודת המשך השמעה")
                    SystemActionPreset.MEDIA_PLAY_PAUSE.id ->
                        media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "נשלחה פקודת נגן / השהה")
                    SystemActionPreset.MEDIA_NEXT.id ->
                        media(KeyEvent.KEYCODE_MEDIA_NEXT, "נשלחה פקודת השיר הבא")
                    SystemActionPreset.MEDIA_PREVIOUS.id ->
                        media(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "נשלחה פקודת השיר הקודם")
                    SystemActionPreset.MEDIA_FAST_FORWARD.id ->
                        media(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "נשלחה פקודת הרצה קדימה")
                    SystemActionPreset.MEDIA_REWIND.id ->
                        media(KeyEvent.KEYCODE_MEDIA_REWIND, "נשלחה פקודת הרצה לאחור")
                    SystemActionPreset.VOLUME_UP.id ->
                        volume(AudioManager.ADJUST_RAISE, "עוצמת השמע הוגברה")
                    SystemActionPreset.VOLUME_DOWN.id ->
                        volume(AudioManager.ADJUST_LOWER, "עוצמת השמע הונמכה")
                    SystemActionPreset.VOLUME_MUTE.id ->
                        muteVolume(true, "השמע הושתק")
                    SystemActionPreset.VOLUME_STATUS.id ->
                        ActionExecutionResult.success("עוצמת השמע הנוכחית: " + currentVolumeText())
                    SystemActionPreset.BRIGHTNESS_UP.id ->
                        changeBrightness(0.10f, "הבהירות הוגברה")
                    SystemActionPreset.BRIGHTNESS_DOWN.id ->
                        changeBrightness(-0.10f, "הבהירות הוחלשה")
                    SystemActionPreset.BRIGHTNESS_SET.id ->
                        setBrightness(config.actionParameter, "הבהירות שונתה")
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
                    SystemActionPreset.DIAL_NUMBER.id -> {
                        val number = config.actionParameter.trim()
                        if (number.isBlank()) {
                            ActionExecutionResult.failure("לא הוגדר מספר לחיוג")
                        } else {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:" + android.net.Uri.encode(number))
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            ActionExecutionResult.success("מסך החיוג נפתח")
                        }
                    }
                    SystemActionPreset.DIAL_CONTACT.id -> {
                        val number = config.contactNumber.ifBlank { config.actionParameter }.trim()
                        if (number.isBlank()) {
                            ActionExecutionResult.failure("לא נבחר איש קשר")
                        } else {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:" + android.net.Uri.encode(number))
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            ActionExecutionResult.success("מסך החיוג לאיש הקשר נפתח")
                        }
                    }
                    SystemActionPreset.ANSWER_CALL.id,
                    SystemActionPreset.DECLINE_CALL.id ->
                        ActionExecutionResult.failure("פעולת שיחה זו דורשת הרשאות טלפוניה ולא מתבצעת במצב בסיסי")
                    SystemActionPreset.PROFILE_NEXT.id ->
                        ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(context, 1))
                    SystemActionPreset.PROFILE_PREVIOUS.id ->
                        ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(context, -1))
                    SystemActionPreset.PROFILE_DEFAULT.id -> {
                        AdvancedRuleRepository.setActiveProfileId(context, "default")
                        ActionExecutionResult.success("חזר לפרופיל ברירת המחדל")
                    }
                    SystemActionPreset.WIFI_SETTINGS.id -> openSettings(Settings.ACTION_WIFI_SETTINGS, "הגדרות Wi‑Fi נפתחו")
                    SystemActionPreset.BLUETOOTH_SETTINGS.id -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "הגדרות Bluetooth נפתחו")
                    SystemActionPreset.DISPLAY_SETTINGS.id -> openSettings(Settings.ACTION_DISPLAY_SETTINGS, "הגדרות תצוגה נפתחו")
                    SystemActionPreset.SOUND_SETTINGS.id -> openSettings(Settings.ACTION_SOUND_SETTINGS, "הגדרות שמע נפתחו")
                    SystemActionPreset.BATTERY_SETTINGS.id -> openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS, "הגדרות סוללה נפתחו")
                    SystemActionPreset.APP_SETTINGS.id -> openSettings(Settings.ACTION_APPLICATION_SETTINGS, "הגדרות אפליקציות נפתחו")
                    else -> ActionExecutionResult.failure("פעולת המערכת אינה נתמכת במצב בסיסי")
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
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
        return ActionExecutionResult.success(reason)
    }

    private fun muteVolume(mute: Boolean, reason: String): ActionExecutionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult.failure("שירות השמע של Android אינו זמין")
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
            AudioManager.FLAG_SHOW_UI,
        )
        return ActionExecutionResult.success(reason)
    }

    private fun openSettings(action: String, reason: String): ActionExecutionResult = try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        ActionExecutionResult.success(reason)
    } catch (error: Throwable) {
        ActionExecutionResult.failure("לא ניתן לפתוח את ההגדרה: " + (error.message ?: "שגיאה"))
    }

    private fun currentVolumeText(): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "לא זמין"
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toString() + "/" +
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toString()
    }

    private fun changeBrightness(delta: Float, reason: String): ActionExecutionResult {
        return runCatching {
            val resolver = context.contentResolver
            val current = android.provider.Settings.System.getInt(
                resolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                128,
            ) / 255f
            val next = (current + delta).coerceIn(0.01f, 1f)
            if (android.provider.Settings.System.canWrite(context)) {
                android.provider.Settings.System.putInt(
                    resolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    (next * 255f).toInt(),
                )
                ActionExecutionResult.success(reason)
            } else {
                ActionExecutionResult.failure("אין הרשאת שינוי הגדרות מערכת. יש לאפשר שינוי הגדרות דרך Android.")
            }
        }.getOrElse {
            ActionExecutionResult.failure("לא ניתן לשנות את בהירות המסך")
        }
    }

    private fun setBrightness(value: String, reason: String): ActionExecutionResult {
        val percent = value.toIntOrNull()?.coerceIn(1, 100)
            ?: return ActionExecutionResult.failure("יש להגדיר בהירות בין 1 ל-100")
        return runCatching {
            val resolver = context.contentResolver
            if (!android.provider.Settings.System.canWrite(context)) {
                return@runCatching ActionExecutionResult.failure("אין הרשאת שינוי הגדרות מערכת")
            }
            android.provider.Settings.System.putInt(
                resolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                (percent * 255 / 100f).toInt(),
            )
            ActionExecutionResult.success(reason + " ל-" + percent + "%")
        }.getOrElse {
            ActionExecutionResult.failure("לא ניתן לשנות את בהירות המסך")
        }
    }
}


class UnavailableActionPerformer(private val reason: String) : ClickActionPerformer {
    override fun supports(config: KeyActionConfig): Boolean = true
    override fun execute(config: KeyActionConfig): ActionExecutionResult =
        ActionExecutionResult.failure(reason)
}
