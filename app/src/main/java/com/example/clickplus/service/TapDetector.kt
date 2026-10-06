package com.example.clickplus.service

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.OperationMode

class TapDetector(
    private val actionExecutor: ActionExecutor,
    private val overlayManager: OverlayManager
) {
    companion object {
        const val LONG_PRESS_MS = 600L
    }

    var currentMode = OperationMode.MODE_A_MULTI_TAP
    var tapTimeoutMs = 450L
    var debounceMs = 80L
    var activeProfile = "DEFAULT"

    private var tapCount = 0
    private var activeKeyCode = -1
    private var lastKeyPressTime = 0L
    private var keyDownTime = 0L
    private var longPressTriggered = false

    private val handler = Handler(Looper.getMainLooper())
    private var mappings = emptyList<KeyActionConfig>()

    private val tapTimeoutRunnable = Runnable { resolveTapSequence() }
    private val longPressRunnable = Runnable { resolveLongPress() }

    fun updateMappings(newMappings: List<KeyActionConfig>) {
        mappings = newMappings.filter { it.isEnabled }
    }

    fun processKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val now = System.currentTimeMillis()
        val activeMappings = mappings.filter {
            it.profileName == activeProfile || it.profileName == "DEFAULT"
        }

        val keyIsMapped = activeMappings.any { it.triggerKeyCode == keyCode }
        if (!keyIsMapped) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (now - lastKeyPressTime < debounceMs) return true

            lastKeyPressTime = now
            activeKeyCode = keyCode
            keyDownTime = now
            longPressTriggered = false

            val hasLong = activeMappings.any {
                it.triggerKeyCode == keyCode && it.tapCount == 0
            }

            handler.removeCallbacks(longPressRunnable)
            if (hasLong) {
                handler.postDelayed(longPressRunnable, LONG_PRESS_MS)
            }

            if (!hasLong) {
                registerTap(activeMappings, keyCode)
            } else {
                overlayManager.showPill("לחיצה", "ממתין…")
            }
            return true
        }

        if (event.action == KeyEvent.ACTION_UP) {
            if (keyCode != activeKeyCode) return true

            handler.removeCallbacks(longPressRunnable)

            if (longPressTriggered) {
                reset()
                return true
            }

            val duration = now - keyDownTime
            if (duration < LONG_PRESS_MS) {
                if (activeMappings.any {
                        it.triggerKeyCode == keyCode && it.tapCount == 1
                    } || activeMappings.any {
                        it.triggerKeyCode == keyCode && it.tapCount == 2
                    }
                ) {
                    registerTap(activeMappings, keyCode)
                }
            }
            return true
        }

        return true
    }

    private fun registerTap(activeMappings: List<KeyActionConfig>, keyCode: Int) {
        if (keyCode != activeKeyCode && tapCount > 0) {
            tapCount = 0
        }

        activeKeyCode = keyCode
        tapCount += 1

        val matching = activeMappings.firstOrNull {
            it.triggerKeyCode == keyCode && it.tapCount == tapCount
        }

        overlayManager.showPill(
            "לחיצה #$tapCount",
            matching?.customLabel.orEmpty()
        )

        handler.removeCallbacks(tapTimeoutRunnable)

        if (matching != null || activeMappings.any {
                it.triggerKeyCode == keyCode && it.tapCount == 1
            }) {
            handler.postDelayed(tapTimeoutRunnable, tapTimeoutMs)
        }
    }

    private fun resolveTapSequence() {
        val activeMappings = mappings.filter {
            it.profileName == activeProfile || it.profileName == "DEFAULT"
        }

        val config = activeMappings.firstOrNull {
            it.triggerKeyCode == activeKeyCode &&
                it.tapCount == tapCount &&
                it.tapCount > 0 &&
                it.isEnabled
        }

        if (config != null) {
            if (currentMode == OperationMode.MODE_B_CONFIRMATION) {
                overlayManager.showPill(
                    "נדרש אישור",
                    config.customLabel.ifBlank { config.keyNameHebrew }
                )
                handler.postDelayed({
                    actionExecutor.execute(config)
                    overlayManager.showPill(
                        "בוצע",
                        config.customLabel.ifBlank { config.keyNameHebrew }
                    )
                }, 250L)
            } else {
                actionExecutor.execute(config)
                overlayManager.showPill(
                    "בוצע",
                    config.customLabel.ifBlank { config.keyNameHebrew }
                )
            }
        }

        reset()
    }

    private fun resolveLongPress() {
        val activeMappings = mappings.filter {
            it.profileName == activeProfile || it.profileName == "DEFAULT"
        }

        val config = activeMappings.firstOrNull {
            it.triggerKeyCode == activeKeyCode &&
                it.tapCount == 0 &&
                it.isEnabled
        } ?: return

        longPressTriggered = true
        handler.removeCallbacks(tapTimeoutRunnable)
        overlayManager.showPill(
            "לחיצה ארוכה",
            config.customLabel.ifBlank { config.keyNameHebrew }
        )
        actionExecutor.execute(config)
        overlayManager.showPill(
            "בוצע",
            config.customLabel.ifBlank { config.keyNameHebrew }
        )
    }

    private fun reset() {
        handler.removeCallbacks(tapTimeoutRunnable)
        handler.removeCallbacks(longPressRunnable)
        tapCount = 0
        activeKeyCode = -1
        keyDownTime = 0L
        longPressTriggered = false
    }
}