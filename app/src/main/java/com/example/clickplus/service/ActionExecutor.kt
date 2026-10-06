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

class ActionExecutor(private val service: AccessibilityService) {

    fun execute(config: KeyActionConfig) {
        if (!config.enabled) return

        when (config.actionType) {
            ActionType.APP -> {
                service.packageManager.getLaunchIntentForPackage(config.targetPackage)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    runCatching { service.startActivity(it) }
                }
            }
            ActionType.SYSTEM -> executeSystem(config.systemActionId)
        }
    }

    private fun executeSystem(actionId: String) {
        when (actionId) {
            SystemActionPreset.HOME.id ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

            SystemActionPreset.BACK.id ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

            SystemActionPreset.RECENTS.id ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

            SystemActionPreset.NOTIFICATIONS.id ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

            SystemActionPreset.MEDIA_PLAY_PAUSE.id ->
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

            SystemActionPreset.MEDIA_NEXT.id ->
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)

            SystemActionPreset.MEDIA_PREVIOUS.id ->
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

            SystemActionPreset.VOLUME_UP.id ->
                adjustVolume(AudioManager.ADJUST_RAISE)

            SystemActionPreset.VOLUME_DOWN.id ->
                adjustVolume(AudioManager.ADJUST_LOWER)

            SystemActionPreset.SETTINGS.id ->
                openSettings(Settings.ACTION_SETTINGS)

            SystemActionPreset.DIALER.id ->
                openSettings(Intent.ACTION_DIAL)
        }
    }

    private fun openSettings(action: String) {
        runCatching {
            service.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun adjustVolume(direction: Int) {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audio.adjustSuggestedStreamVolume(
            direction,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI,
        )
    }
}
