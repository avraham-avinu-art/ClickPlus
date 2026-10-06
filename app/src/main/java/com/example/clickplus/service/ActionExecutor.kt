package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.KeyActionConfig

class ActionExecutor(private val service: AccessibilityService) {

    fun execute(config: KeyActionConfig) {
        if (!config.isEnabled) return

        when (config.actionType) {
            ActionType.LAUNCH_APP -> launchApp(config.targetPackage)
            ActionType.SYSTEM_KEY -> performSystemKey(config.systemKeyCode)
            ActionType.CLICK_NODE_BY_ID -> clickNodeByViewId(config.nodeIdentifier)
            ActionType.CLICK_NODE_BY_TEXT -> clickNodeByText(config.nodeIdentifier)
            ActionType.SEND_INTENT -> sendCustomBroadcast(config.targetClassOrIntent)
        }
    }

    private fun launchApp(packageName: String) {
        if (packageName.isBlank()) return

        service.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            runCatching { service.startActivity(this) }
        }
    }

    private fun performSystemKey(keyCode: Int) {
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

    private fun dispatchMediaKey(keyCode: Int) {
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager.adjustSuggestedStreamVolume(
            direction,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun clickNodeByViewId(viewId: String) {
        if (viewId.isBlank()) return
        service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId(viewId)
            ?.firstOrNull()
            ?.let(::performClickRecursively)
    }

    private fun clickNodeByText(text: String) {
        if (text.isBlank()) return
        service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByText(text)
            ?.firstOrNull()
            ?.let(::performClickRecursively)
    }

    private fun performClickRecursively(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            parent = parent.parent
        }
        return false
    }

    private fun sendCustomBroadcast(actionString: String) {
        if (actionString.isNotBlank()) {
            runCatching { service.sendBroadcast(Intent(actionString)) }
        }
    }
}