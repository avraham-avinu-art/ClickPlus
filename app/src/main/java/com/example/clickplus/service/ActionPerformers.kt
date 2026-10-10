package com.example.clickplus.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.profileIdFromActionId
import com.example.clickplus.data.SystemActionPreset

data class ActionExecutionResult(
    val success: Boolean,
    val reason: String = "",
    val pending: Boolean = false,
    val executionId: String? = null,
) {
    companion object {
        fun success(reason: String = "הפעולה בוצעה בהצלחה") = ActionExecutionResult(true, reason)
        fun pending(reason: String, executionId: String) =
            ActionExecutionResult(true, reason, pending = true, executionId = executionId)
        fun failure(reason: String) = ActionExecutionResult(false, reason)
    }
}

interface ClickActionPerformer {
    fun execute(config: KeyActionConfig): ActionExecutionResult
    fun supports(config: KeyActionConfig): Boolean
}

object BasicActionCapabilities {
    val supportedSystemActions = setOf(
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
        SystemActionPreset.DIAL_NUMBER.id,
        SystemActionPreset.DIAL_CONTACT.id,
    )

    fun supports(config: KeyActionConfig): Boolean =
        config.actionType == ActionType.APP ||
            config.actionType == ActionType.PROFILE ||
            (config.actionType == ActionType.SYSTEM && config.systemActionId in supportedSystemActions)
}

class BasicActionPerformer(private val context: Context) : ClickActionPerformer {
    private val deviceActions = CommonDeviceActions(context)
    override fun supports(config: KeyActionConfig): Boolean =
        BasicActionCapabilities.supports(config)

    override fun execute(config: KeyActionConfig): ActionExecutionResult {
        if (!config.enabled) return ActionExecutionResult.failure("הכלל מושבת")
        if (!supports(config)) {
            return ActionExecutionResult.failure("הפעולה אינה נתמכת במצב Basic")
        }

        return try {
            when (config.actionType) {
                ActionType.MULTI_POINT_TAP -> ActionExecutionResult.failure("שתי לחיצות אוטומטיות דורשות מצב מלא עם שירות נגישות")
                ActionType.APP_TAP -> ActionExecutionResult.failure("לחיצה בתוך אפליקציה דורשת מצב מלא עם שירות נגישות")
                ActionType.PROFILE -> {
                    val selectedProfileId = profileIdFromActionId(config.systemActionId)
                    if (selectedProfileId != null) {
                        val profile = AdvancedRuleRepository(context).profiles().firstOrNull { it.id == selectedProfileId }
                            ?: return ActionExecutionResult.failure("הפרופיל שנבחר אינו קיים")
                        if (!profile.enabled) {
                            return ActionExecutionResult.failure("הפרופיל שנבחר מושבת")
                        }
                        AdvancedRuleRepository.setActiveProfileId(context, profile.id)
                        ActionExecutionResult.success("עבר לפרופיל: " + profile.name)
                    } else {
                        // Backward compatibility for rules saved by older versions.
                        when (config.systemActionId) {
                            "profile_next" ->
                                ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(context, 1))
                            "profile_previous" ->
                                ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(context, -1))
                            "profile_default" -> {
                                AdvancedRuleRepository.setActiveProfileId(context, "default")
                                ActionExecutionResult.success("חזר לפרופיל ברירת המחדל")
                            }
                            else -> ActionExecutionResult.failure("פעולת פרופיל ClickPlus אינה מוכרת")
                        }
                    }
                }                ActionType.APP -> {
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
                        deviceActions.media(KeyEvent.KEYCODE_MEDIA_STOP, "נשלחה פקודת עצירת השמעה")
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
                        deviceActions.adjustVolume(AudioManager.ADJUST_RAISE, "עוצמת השמע הוגברה")
                    SystemActionPreset.VOLUME_DOWN.id ->
                        volume(AudioManager.ADJUST_LOWER, "עוצמת השמע הונמכה")
                    SystemActionPreset.VOLUME_MUTE.id ->
                        deviceActions.muteVolume(true, "השמע הושתק")
                    SystemActionPreset.VOLUME_STATUS.id ->
                        ActionExecutionResult.success("עוצמת השמע הנוכחית: " + deviceActions.currentVolumeText())
                    SystemActionPreset.BRIGHTNESS_UP.id ->
                        deviceActions.changeBrightness(0.10f, "הבהירות הוגברה")
                    SystemActionPreset.BRIGHTNESS_DOWN.id ->
                        deviceActions.changeBrightness(-0.10f, "הבהירות הוחלשה")
                    SystemActionPreset.BRIGHTNESS_SET.id ->
                        deviceActions.setBrightness(config.actionParameter, "הבהירות שונתה")
                    SystemActionPreset.SETTINGS.id -> {
                        deviceActions.openSettings(Settings.ACTION_SETTINGS, "מסך ההגדרות נפתח")
                    }
                    SystemActionPreset.DIALER.id -> {
                        deviceActions.openIntent(Intent(Intent.ACTION_DIAL), "החייגן נפתח")
                    }
                    SystemActionPreset.DIAL_NUMBER.id -> {
                        val number = config.actionParameter.trim()
                        if (number.isBlank()) {
                            ActionExecutionResult.failure("לא הוגדר מספר לחיוג")
                        } else {
                            deviceActions.openIntent(
                                Intent(
                                    Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:" + android.net.Uri.encode(number)),
                                ),
                                "מסך החיוג נפתח",
                            )
                        }
                    }
                    SystemActionPreset.DIAL_CONTACT.id -> {
                        val number = config.contactNumber.ifBlank { config.actionParameter }.trim()
                        if (number.isBlank()) {
                            ActionExecutionResult.failure("לא נבחר איש קשר")
                        } else {
                            deviceActions.openIntent(
                                Intent(
                                    Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:" + android.net.Uri.encode(number)),
                                ),
                                "מסך החיוג לאיש הקשר נפתח",
                            )
                        }
                    }
                    SystemActionPreset.ANSWER_CALL.id,
                    SystemActionPreset.DECLINE_CALL.id ->
                        ActionExecutionResult.failure("פעולת שיחה זו דורשת הרשאות טלפוניה ולא מתבצעת במצב בסיסי")
                    SystemActionPreset.WIFI_SETTINGS.id -> deviceActions.openSettings(Settings.ACTION_WIFI_SETTINGS, "הגדרות Wi‑Fi נפתחו")
                    SystemActionPreset.BLUETOOTH_SETTINGS.id -> deviceActions.openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "הגדרות Bluetooth נפתחו")
                    SystemActionPreset.DISPLAY_SETTINGS.id -> deviceActions.openSettings(Settings.ACTION_DISPLAY_SETTINGS, "הגדרות תצוגה נפתחו")
                    SystemActionPreset.SOUND_SETTINGS.id -> deviceActions.openSettings(Settings.ACTION_SOUND_SETTINGS, "הגדרות שמע נפתחו")
                    SystemActionPreset.BATTERY_SETTINGS.id -> deviceActions.openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS, "הגדרות סוללה נפתחו")
                    SystemActionPreset.APP_SETTINGS.id -> deviceActions.openSettings(Settings.ACTION_APPLICATION_SETTINGS, "הגדרות אפליקציות נפתחו")
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


}


class UnavailableActionPerformer(private val reason: String) : ClickActionPerformer {
    override fun supports(config: KeyActionConfig): Boolean = true
    override fun execute(config: KeyActionConfig): ActionExecutionResult =
        ActionExecutionResult.failure(reason)
}
