package com.example.clickplus.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset

interface ClickActionPerformer {
    fun execute(config: KeyActionConfig): Boolean
    fun supports(config: KeyActionConfig): Boolean
}

class BasicActionPerformer(private val context: Context) : ClickActionPerformer {
    override fun supports(config: KeyActionConfig): Boolean {
        return config.actionType == ActionType.APP ||
            config.systemActionId in setOf(
                SystemActionPreset.MEDIA_PLAY_PAUSE.id,
                SystemActionPreset.MEDIA_NEXT.id,
                SystemActionPreset.MEDIA_PREVIOUS.id,
                SystemActionPreset.VOLUME_UP.id,
                SystemActionPreset.VOLUME_DOWN.id,
                SystemActionPreset.SETTINGS.id,
                SystemActionPreset.DIALER.id,
            )
    }

    override fun execute(config: KeyActionConfig): Boolean {
        if (!supports(config)) return false
        return runCatching {
            when (config.actionType) {
                ActionType.APP -> {
                    val intent = context.packageManager.getLaunchIntentForPackage(config.targetPackage)
                        ?: return false
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                    true
                }
                ActionType.SYSTEM -> when (config.systemActionId) {
                    SystemActionPreset.MEDIA_PLAY_PAUSE.id -> media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    SystemActionPreset.MEDIA_NEXT.id -> media(KeyEvent.KEYCODE_MEDIA_NEXT)
                    SystemActionPreset.MEDIA_PREVIOUS.id -> media(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    SystemActionPreset.VOLUME_UP.id -> volume(AudioManager.ADJUST_RAISE)
                    SystemActionPreset.VOLUME_DOWN.id -> volume(AudioManager.ADJUST_LOWER)
                    SystemActionPreset.SETTINGS.id -> {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
                    }
                    SystemActionPreset.DIALER.id -> {
                        context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
                    }
                    else -> false
                }
            }
        }.getOrDefault(false)
    }

    private fun media(code: Int): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return true
    }

    private fun volume(direction: Int): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        audio.adjustSuggestedStreamVolume(direction, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
        return true
    }
}
