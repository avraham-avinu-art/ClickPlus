package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.SystemActionPreset

class AccessibilitySystemActionExecutor(
    private val service: AccessibilityService,
) {

    fun execute(actionId: String, actionParameter: String = ""): ActionExecutionResult = when (actionId) {
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
    }

    private fun declineCall(): ActionExecutionResult = runCatching {
        val telecom = service.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        if (!service.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS).equals(android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            return ActionExecutionResult.failure("נדרשת הרשאת מענה לשיחות")
        }
        @Suppress("DEPRECATION")
        telecom.endCall()
        ActionExecutionResult.success("השיחה נדחתה")
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
