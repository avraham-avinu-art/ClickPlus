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
    override fun supports(config: KeyActionConfig): Boolean = true

    override fun execute(config: KeyActionConfig): Boolean {
        if (!config.enabled) return false
        return runCatching {
            when (config.actionType) {
                ActionType.APP -> {
                    val intent = service.packageManager.getLaunchIntentForPackage(config.targetPackage)
                        ?: return false
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    service.startActivity(intent)
                    true
                }
                ActionType.SYSTEM -> executeSystem(config.systemActionId)
            }
        }.getOrDefault(false)
    }

    private fun executeSystem(actionId: String): Boolean = when (actionId) {
        SystemActionPreset.HOME.id -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        SystemActionPreset.BACK.id -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        SystemActionPreset.RECENTS.id -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        SystemActionPreset.NOTIFICATIONS.id -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        SystemActionPreset.MEDIA_PLAY_PAUSE.id -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        SystemActionPreset.MEDIA_NEXT.id -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        SystemActionPreset.MEDIA_PREVIOUS.id -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        SystemActionPreset.VOLUME_UP.id -> adjustVolume(AudioManager.ADJUST_RAISE)
        SystemActionPreset.VOLUME_DOWN.id -> adjustVolume(AudioManager.ADJUST_LOWER)
        SystemActionPreset.SETTINGS.id -> openSettings(Settings.ACTION_SETTINGS)
        SystemActionPreset.DIALER.id -> openSettings(Intent.ACTION_DIAL)
        else -> false
    }

    private fun openSettings(action: String): Boolean = runCatching {
        service.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    private fun dispatchMediaKey(keyCode: Int): Boolean {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return true
    }

    private fun adjustVolume(direction: Int): Boolean {
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        audio.adjustSuggestedStreamVolume(direction, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
        return true
    }
}
