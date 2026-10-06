package com.example.clickplus.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.SystemActionPreset

class ActionExecutor(private val service: AccessibilityService) {

    fun execute(config: KeyActionConfig) {
        if (!config.isEnabled) return

        when (config.actionType) {
            ActionType.LAUNCH_APP -> launchApp(config.targetPackage)
            ActionType.SYSTEM_KEY -> performSystemAction(config.systemActionId, config.systemKeyCode)
            ActionType.CLICK_NODE_BY_ID -> clickNodeByViewId(config.nodeIdentifier)
            ActionType.CLICK_NODE_BY_TEXT -> clickNodeByText(config.nodeIdentifier)
            ActionType.SEND_INTENT -> sendCustomIntent(config.targetClassOrIntent)
        }
    }

    private fun launchApp(packageName: String) {
        if (packageName.isBlank()) return

        service.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            runCatching { service.startActivity(this) }
        }
    }

    private fun performSystemAction(actionId: String, legacyKeyCode: Int) {
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
            SystemActionPreset.WIFI_SETTINGS.id ->
                openSettings(Settings.ACTION_WIFI_SETTINGS)
            SystemActionPreset.SETTINGS.id ->
                openSettings(Settings.ACTION_SETTINGS)
            SystemActionPreset.DIALER.id ->
                openSettings(Intent.ACTION_DIAL)
            SystemActionPreset.FLASHLIGHT.id ->
                toggleFlashlight()
            else ->
                performLegacyKey(legacyKeyCode)
        }
    }

    private fun performLegacyKey(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            KeyEvent.KEYCODE_HOME ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            KeyEvent.KEYCODE_APP_SWITCH ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            KeyEvent.KEYCODE_NOTIFICATION ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            KeyEvent.KEYCODE_VOLUME_UP ->
                adjustVolume(AudioManager.ADJUST_RAISE)
            KeyEvent.KEYCODE_VOLUME_DOWN ->
                adjustVolume(AudioManager.ADJUST_LOWER)
        }
    }

    private fun openSettings(action: String) {
        runCatching {
            service.startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
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
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun toggleFlashlight() {
        if (ContextCompat.checkSelfPermission(service, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        val manager = service.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            runCatching {
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }.getOrDefault(false)
        } ?: return

        runCatching {
            val key = "clickplus_flashlight_$cameraId"
            val preferences = service.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            val enabled = preferences.getBoolean(key, false)
            manager.setTorchMode(cameraId, !enabled)
            preferences.edit().putBoolean(key, !enabled).apply()
        }
    }

    private fun clickNodeByViewId(viewId: String) {
        if (viewId.isBlank()) return
        service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId(viewId)
            ?.firstOrNull()
            ?.let(::performClick)
    }

    private fun clickNodeByText(text: String) {
        if (text.isBlank()) return
        service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText(text)
            ?.firstOrNull()
            ?.let(::performClick)
    }

    private fun performClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            parent = parent.parent
        }
        return false
    }

    private fun sendCustomIntent(action: String) {
        if (action.isBlank()) return
        runCatching { service.sendBroadcast(Intent(action)) }
    }
}