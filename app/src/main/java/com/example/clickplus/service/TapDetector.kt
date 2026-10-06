package com.example.clickplus.service

import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.OperationMode

class TapDetector(
    private val actionExecutor: ActionExecutor,
    private val overlayManager: OverlayManager
) {
    var currentMode = OperationMode.MODE_A_MULTI_TAP
    var tapTimeoutMs = 450L
    var debounceMs = 80L
    var activeProfile = "DEFAULT"
    private var tapCount = 0
    private var lastKeyPressTime = 0L
    private var lastKeyCode = -1
    private val handler = Handler(Looper.getMainLooper())
    private var mappings = mutableListOf<KeyActionConfig>()
    private val timeoutRunnable = Runnable { executeCurrentTapAction() }

    fun updateMappings(newMappings: List<KeyActionConfig>) {
        mappings = newMappings.filter { it.isEnabled }.toMutableList()
    }

    fun processKeyEvent(keyCode: Int): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastKeyPressTime < debounceMs) return true
        val activeMappings = mappings.filter { it.profileName == activeProfile || it.profileName == "DEFAULT" }
        if (activeMappings.none { it.triggerKeyCode == keyCode }) return false
        if (keyCode != lastKeyCode && tapCount > 0) tapCount = 0
        lastKeyCode = keyCode
        lastKeyPressTime = now

        if (currentMode == OperationMode.MODE_B_CONFIRMATION) {
            val config = activeMappings.find { it.triggerKeyCode == keyCode && it.tapCount == 1 }
            if (config != null) {
                overlayManager.showPill("Confirm", config.customLabel)
                handler.removeCallbacks(timeoutRunnable)
                handler.postDelayed({
                    actionExecutor.execute(config)
                    overlayManager.showPill("Done!", config.customLabel)
                }, 250L)
            }
            return true
        }

        handler.removeCallbacks(timeoutRunnable)
        tapCount++
        val matching = activeMappings.find { it.triggerKeyCode == keyCode && it.tapCount == tapCount }
        overlayManager.showPill("Tap #$tapCount", matching?.customLabel.orEmpty())
        handler.postDelayed(timeoutRunnable, tapTimeoutMs)
        return true
    }

    private fun executeCurrentTapAction() {
        val config = mappings.find {
            (it.profileName == activeProfile || it.profileName == "DEFAULT") &&
                it.triggerKeyCode == lastKeyCode &&
                it.tapCount == tapCount &&
                it.isEnabled
        }
        if (config != null) {
            actionExecutor.execute(config)
            overlayManager.showPill("Done!", config.customLabel)
        }
        tapCount = 0
        lastKeyCode = -1
    }
}
