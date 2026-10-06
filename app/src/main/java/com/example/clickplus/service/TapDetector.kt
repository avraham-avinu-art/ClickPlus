package com.example.clickplus.service

import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.TriggerType

class TapDetector(
    private val context: Context,
    private val actionExecutor: ActionExecutor,
    private val onTapCount: (Int) -> Unit = {},
) {
    var tapTimeoutMs = 650L

    private var tapCount = 0
    private var lastLaunchTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable {
        tapCount = 0
    }

    private val screenTapCounts = mutableMapOf<String, Int>()
    private val screenTapResetRunnables = mutableMapOf<String, Runnable>()
    private val screenTapLastEventTimes = mutableMapOf<String, Long>()

    private var profiles = emptyList<KeyActionConfig>()

    fun updateProfiles(newProfiles: List<KeyActionConfig>) {
        profiles = newProfiles.filter { it.enabled }
    }

    fun processActivationLaunch(previousForegroundPackage: String) {
        val all = profiles.filter { it.triggerType == TriggerType.APP_ENTRY }
        if (all.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 100L) return
        lastLaunchTime = now

        tapCount = (tapCount + 1).coerceAtMost(10)
        handler.removeCallbacks(resetRunnable)
        onTapCount(tapCount)

        val current = selectProfile(all, tapCount, previousForegroundPackage)
        if (current != null) {
            actionExecutor.execute(current)
        }

        handler.postDelayed(resetRunnable, tapTimeoutMs.coerceIn(300L, 1500L))
    }

    fun processViewClicked(event: AccessibilityEvent) {
        val all = profiles.filter { it.triggerType == TriggerType.SCREEN_TAP }
        if (all.isEmpty()) return

        val source = event.source ?: return
        val bounds = Rect()
        runCatching { source.getBoundsInScreen(bounds) }.getOrNull()
        if (bounds.isEmpty) return

        val eventPackage = event.packageName?.toString().orEmpty()
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val centerXRatio = (bounds.centerX() / width).coerceIn(0f, 1f)
        val centerYRatio = (bounds.centerY() / height).coerceIn(0f, 1f)

        val now = System.currentTimeMillis()
        all.forEach { config ->
            if (config.screenTapPackage.isNotBlank() &&
                config.screenTapPackage != eventPackage
            ) {
                return@forEach
            }

            if (!tapLocationMatches(config, centerXRatio, centerYRatio)) {
                return@forEach
            }

            val lastEvent = screenTapLastEventTimes[config.id] ?: 0L
            if (now - lastEvent < 120L) return@forEach
            screenTapLastEventTimes[config.id] = now

            val count = (screenTapCounts[config.id] ?: 0) + 1
            screenTapCounts[config.id] = count
            onTapCount(count)

            val reset = screenTapResetRunnables[config.id]
            if (reset != null) handler.removeCallbacks(reset)

            val newReset = Runnable {
                screenTapCounts.remove(config.id)
                screenTapResetRunnables.remove(config.id)
            }
            screenTapResetRunnables[config.id] = newReset
            handler.postDelayed(newReset, tapTimeoutMs.coerceIn(300L, 1500L))

            if (count >= config.pressCount) {
                screenTapCounts.remove(config.id)
                screenTapResetRunnables.remove(config.id)
                handler.removeCallbacks(newReset)
                actionExecutor.execute(config)
            }
        }
    }

    private fun tapLocationMatches(
        config: KeyActionConfig,
        xRatio: Float,
        yRatio: Float,
    ): Boolean {
        if (config.screenTapXRatio < 0f || config.screenTapYRatio < 0f) return false
        val dx = xRatio - config.screenTapXRatio
        val dy = yRatio - config.screenTapYRatio
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        return distance <= config.screenTapToleranceRatio.coerceIn(0.01f, 0.25f)
    }

    private fun selectProfile(
        all: List<KeyActionConfig>,
        count: Int,
        foregroundPackage: String,
    ): KeyActionConfig? {
        val matchingCount = all.filter { it.pressCount == count }
        if (matchingCount.isEmpty()) return null

        val specific = matchingCount
            .asSequence()
            .filter { it.contextConditionType != ContextConditionType.ANY }
            .filter { contextMatches(it, foregroundPackage) }
            .firstOrNull()

        return specific ?: matchingCount.firstOrNull {
            it.contextConditionType == ContextConditionType.ANY
        }
    }

    private fun contextMatches(
        config: KeyActionConfig,
        foregroundPackage: String,
    ): Boolean {
        return when (config.contextConditionType) {
            ContextConditionType.ANY -> true

            ContextConditionType.APP ->
                foregroundPackage.isNotBlank() &&
                    foregroundPackage == config.contextConditionValue

            ContextConditionType.MUSIC -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audio?.isMusicActive == true
            }

            ContextConditionType.MUTED -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audio?.let {
                    it.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                        it.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
                } == true
            }

            ContextConditionType.RINGING -> {
                val telephony =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                runCatching {
                    telephony?.callState == TelephonyManager.CALL_STATE_RINGING
                }.getOrDefault(false)
            }

            ContextConditionType.RADIO -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val currentPackage = foregroundPackage
                audio?.isMusicActive == true &&
                    if (config.contextConditionValue.isBlank()) {
                        currentPackage.lowercase().let {
                            it.contains("radio") || it.contains("fm") ||
                                it.contains("dab") || it.contains("tuner")
                        }
                    } else {
                        currentPackage == config.contextConditionValue
                    }
            }
        }
    }
}
