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
    private val deviceActions = CommonDeviceActions(service)

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
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_STOP, "עצירת השמעה")
        SystemActionPreset.MEDIA_PLAY.id ->
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_PLAY, "הפעלת השמעה")
        SystemActionPreset.MEDIA_RESUME.id ->
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_PLAY, "המשך השמעה")
        SystemActionPreset.MEDIA_PLAY_PAUSE.id ->
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "נגן / השהה")
        SystemActionPreset.MEDIA_NEXT.id ->
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_NEXT, "השיר הבא")
        SystemActionPreset.MEDIA_PREVIOUS.id ->
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "השיר הקודם")
        SystemActionPreset.MEDIA_FAST_FORWARD.id ->
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "הרצה קדימה")
        SystemActionPreset.MEDIA_REWIND.id ->
            deviceActions.media(KeyEvent.KEYCODE_MEDIA_REWIND, "הרצה לאחור")
        SystemActionPreset.VOLUME_UP.id ->
            deviceActions.deviceActions.adjustVolume(AudioManager.ADJUST_RAISE, "עוצמת השמע הוגברה")
        SystemActionPreset.VOLUME_DOWN.id ->
            deviceActions.adjustVolume(AudioManager.ADJUST_LOWER, "עוצמת השמע הונמכה")
        SystemActionPreset.VOLUME_MUTE.id ->
            deviceActions.muteVolume(true, "השמע הושתק")
        SystemActionPreset.VOLUME_STATUS.id ->
            ActionExecutionResult.success("עוצמת השמע הנוכחית: " + deviceActions.currentVolumeText())
        SystemActionPreset.BRIGHTNESS_UP.id ->
            deviceActions.deviceActions.changeBrightness(0.10f, "הבהירות הוגברה")
        SystemActionPreset.BRIGHTNESS_DOWN.id ->
            deviceActions.deviceActions.changeBrightness(-0.10f, "הבהירות הוחלשה")
        SystemActionPreset.BRIGHTNESS_SET.id ->
            deviceActions.deviceActions.setBrightness(actionParameter, "הבהירות שונתה")
        SystemActionPreset.WIFI_SETTINGS.id ->
            deviceActions.deviceActions.openSettings(Settings.ACTION_WIFI_SETTINGS, "הגדרות Wi‑Fi נפתחו")
        SystemActionPreset.BLUETOOTH_SETTINGS.id ->
            deviceActions.openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "הגדרות Bluetooth נפתחו")
        SystemActionPreset.DISPLAY_SETTINGS.id ->
            deviceActions.openSettings(Settings.ACTION_DISPLAY_SETTINGS, "הגדרות תצוגה נפתחו")
        SystemActionPreset.SOUND_SETTINGS.id ->
            deviceActions.openSettings(Settings.ACTION_SOUND_SETTINGS, "הגדרות שמע נפתחו")
        SystemActionPreset.BATTERY_SETTINGS.id ->
            deviceActions.openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS, "הגדרות סוללה נפתחו")
        SystemActionPreset.APP_SETTINGS.id ->
            deviceActions.openSettings(Settings.ACTION_APPLICATION_SETTINGS, "הגדרות אפליקציות נפתחו")
        SystemActionPreset.SETTINGS.id ->
            deviceActions.openSettings(Settings.ACTION_SETTINGS, "מסך ההגדרות נפתח")
        SystemActionPreset.DIALER.id ->
            deviceActions.openSettings(Intent.ACTION_DIAL, "החייגן נפתח")
        SystemActionPreset.DIAL_NUMBER.id -> {
            val number = actionParameter.trim()
            if (number.isBlank()) ActionExecutionResult.failure("לא הוגדר מספר לחיוג")
            else deviceActions.openIntent(
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
                deviceActions.openIntent(
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

}
