package com.example.clickplus.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.TriggerType
import kotlin.math.sqrt

class TapDetector(
    private val context: Context,
    private val actionExecutor: RuleExecutionCoordinator,
    private val onTapCount: (Int) -> Unit = {},
) {
    var tapTimeoutMs = 650L
    private var tapCount = 0
    private var lastLaunchTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable { tapCount = 0 }
    private val screenTapCounts = mutableMapOf<String, Int>()
    private val screenReset = mutableMapOf<String, Runnable>()
    private var profiles = emptyList<KeyActionConfig>()
    private val advanced = AdvancedRuleRepository(context)

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

        val matchingSpecific = all
            .filter { it.pressCount == tapCount && it.contextConditionType != ContextConditionType.ANY }
            .filter { contextMatches(it, previousForegroundPackage) }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }

        val chosen = matchingSpecific.firstOrNull() ?: all
            .filter { it.pressCount == tapCount && it.contextConditionType == ContextConditionType.ANY }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
            .firstOrNull()

        if (chosen != null) {
            actionExecutor.execute(chosen, previousForegroundPackage, "כניסה ל-ClickPlus")
        }
        handler.postDelayed(resetRunnable, tapTimeoutMs.coerceIn(300L, 1500L))
    }

    fun processViewClicked(event: AccessibilityEvent) {
        val all = profiles.filter { it.triggerType == TriggerType.SCREEN_TAP }
        if (all.isEmpty()) return
        val packageName = event.packageName?.toString().orEmpty()
        val source = event.source ?: return
        val bounds = Rect()
        runCatching { source.getBoundsInScreen(bounds) }.getOrNull() ?: return
        if (bounds.isEmpty) return

        val width = context.resources.displayMetrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = context.resources.displayMetrics.heightPixels.coerceAtLeast(1).toFloat()
        val xRatio = (bounds.centerX() / width).coerceIn(0f, 1f)
        val yRatio = (bounds.centerY() / height).coerceIn(0f, 1f)

        all.filter { it.screenTapPackage.isBlank() || it.screenTapPackage == packageName }
            .filter { tapLocationMatches(it, xRatio, yRatio) }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
            .forEach { config ->
                val count = (screenTapCounts[config.id] ?: 0) + 1
                screenTapCounts[config.id] = count
                onTapCount(count)
                screenReset[config.id]?.let(handler::removeCallbacks)
                val reset = Runnable {
                    screenTapCounts.remove(config.id)
                    screenReset.remove(config.id)
                }
                screenReset[config.id] = reset
                handler.postDelayed(reset, tapTimeoutMs.coerceIn(300L, 1500L))

                if (count >= config.pressCount) {
                    screenTapCounts.remove(config.id)
                    screenReset.remove(config.id)
                    handler.removeCallbacks(reset)
                    actionExecutor.execute(config, packageName, "לחיצה במיקום במסך")
                }
            }
    }

    private fun tapLocationMatches(config: KeyActionConfig, xRatio: Float, yRatio: Float): Boolean {
        if (config.screenTapXRatio < 0f || config.screenTapYRatio < 0f) return false
        val meta = advanced.getRuleMetadata(config.id)
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val targetX = if (landscape && meta.landscapeX >= 0f) meta.landscapeX
        else if (!landscape && meta.portraitX >= 0f) meta.portraitX
        else config.screenTapXRatio
        val targetY = if (landscape && meta.landscapeY >= 0f) meta.landscapeY
        else if (!landscape && meta.portraitY >= 0f) meta.portraitY
        else config.screenTapYRatio

        val tx = meta.toleranceXRatio.coerceIn(0.01f, 0.25f)
        val ty = meta.toleranceYRatio.coerceIn(0.01f, 0.25f)
        val dx = (xRatio - targetX) / tx
        val dy = (yRatio - targetY) / ty
        return sqrt(dx * dx + dy * dy) <= 1f
    }

    private fun contextMatches(config: KeyActionConfig, foregroundPackage: String): Boolean {
        return when (config.contextConditionType) {
            ContextConditionType.ANY -> true
            ContextConditionType.APP ->
                foregroundPackage.isNotBlank() && foregroundPackage == config.contextConditionValue
            ContextConditionType.MUSIC ->
                (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.isMusicActive == true
            ContextConditionType.MUTED -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audio?.let {
                    it.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                        it.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
                } == true
            }
            ContextConditionType.RINGING -> {
                val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                runCatching { telephony?.callState == TelephonyManager.CALL_STATE_RINGING }.getOrDefault(false)
            }
            ContextConditionType.RADIO -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val value = foregroundPackage.lowercase()
                audio?.isMusicActive == true &&
                    if (config.contextConditionValue.isBlank()) {
                        value.contains("radio") || value.contains("fm") || value.contains("dab") || value.contains("tuner")
                    } else foregroundPackage == config.contextConditionValue
            }
        }
    }
}
