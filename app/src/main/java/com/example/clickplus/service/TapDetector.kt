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
import kotlin.math.max
import kotlin.math.min

class TapDetector(
    private val context: Context,
    private val actionExecutor: RuleExecutionCoordinator,
    private val onTapCount: (Int) -> Unit = {},
) {
    var tapTimeoutMs = 1200L
    private var tapCount = 0
    private var lastLaunchTime = 0L
    private var pendingForegroundPackage = ""
    private var pendingEnteredPackage = ""
    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable {
        resolveActivationLaunch()
    }
    private val screenTapCounts = mutableMapOf<String, Int>()
    private val screenReset = mutableMapOf<String, Runnable>()
    private var profiles = emptyList<KeyActionConfig>()
    private val advanced = AdvancedRuleRepository(context)

    fun updateProfiles(newProfiles: List<KeyActionConfig>) {
        profiles = newProfiles.filter { it.enabled }
    }

    fun processAppEntry(enteredPackage: String, previousForegroundPackage: String) {
        if (enteredPackage.isBlank()) return
        val all = profiles.filter {
            it.enabled &&
                it.triggerType == TriggerType.APP_ENTRY &&
                it.triggerPackage.isNotBlank() &&
                it.triggerPackage == enteredPackage &&
                AdvancedRuleRepository.isRuleInActiveProfile(context, it.id)
        }
        if (all.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 100L) return
        lastLaunchTime = now

        if (tapCount == 0) {
            pendingEnteredPackage = enteredPackage
        } else if (pendingEnteredPackage != enteredPackage) {
            // A new target app starts a fresh entry sequence.
            tapCount = 0
            pendingForegroundPackage = ""
            pendingEnteredPackage = enteredPackage
        }
        tapCount = (tapCount + 1).coerceAtMost(10)
        pendingForegroundPackage = previousForegroundPackage
        handler.removeCallbacks(resetRunnable)
        onTapCount(tapCount)
        handler.postDelayed(resetRunnable, tapTimeoutMs.coerceIn(300L, 1500L))
    }

    fun processActivationLaunch(previousForegroundPackage: String) {
        // Kept for the legacy launcher entry path.
        processAppEntry("legacy_launcher", previousForegroundPackage)
    }

    private fun resolveActivationLaunch() {
        val finalCount = tapCount.coerceIn(1, 10)
        val previousForegroundPackage = pendingForegroundPackage
        val enteredPackage = pendingEnteredPackage
        tapCount = 0
        pendingForegroundPackage = ""
        pendingEnteredPackage = ""

        val all = profiles.filter {
            it.enabled &&
                it.triggerType == TriggerType.APP_ENTRY &&
                it.triggerPackage == enteredPackage &&
                AdvancedRuleRepository.isRuleInActiveProfile(context, it.id)
        }
        val matchingSpecific = all
            .filter { it.pressCount == finalCount && it.contextConditionType != ContextConditionType.ANY }
            .filter { contextMatches(it, previousForegroundPackage) }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }

        val chosen = matchingSpecific.firstOrNull() ?: all
            .filter { it.pressCount == finalCount && it.contextConditionType == ContextConditionType.ANY }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
            .firstOrNull()

        if (chosen != null) {
            actionExecutor.execute(chosen, previousForegroundPackage, "כניסה ל-ClickPlus")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun processViewClicked(event: AccessibilityEvent) {
        // Intentionally not used: in-app touch is an action mechanism, not a trigger.
    }

    private fun tapLocationMatches(
        config: KeyActionConfig,
        xRatio: Float,
        yRatio: Float,
        bounds: Rect,
        width: Float,
        height: Float,
    ): Boolean {
        val meta = advanced.getRuleMetadata(config.id)
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val targetX = if (meta.useOrientationSpecificPosition && landscape && meta.landscapeX >= 0f) {
            meta.landscapeX
        } else if (meta.useOrientationSpecificPosition && !landscape && meta.portraitX >= 0f) {
            meta.portraitX
        } else config.screenTapXRatio
        val targetY = if (meta.useOrientationSpecificPosition && landscape && meta.landscapeY >= 0f) {
            meta.landscapeY
        } else if (meta.useOrientationSpecificPosition && !landscape && meta.portraitY >= 0f) {
            meta.portraitY
        } else config.screenTapYRatio

        if (!targetX.isFinite() || !targetY.isFinite() || targetX < 0f || targetY < 0f) return false

        val tx = meta.toleranceXRatio.takeIf { it.isFinite() }?.coerceIn(0.01f, 0.25f) ?: 0.08f
        val ty = meta.toleranceYRatio.takeIf { it.isFinite() }?.coerceIn(0.01f, 0.25f) ?: 0.08f

        // Prefer the actual clicked node area. This is much more reliable than
        // comparing node centers when an app exposes a large clickable surface.
        // Ignore a full-screen root/container so one generic node cannot trigger
        // every tap in an app.
        val nodeWidth = bounds.width().coerceAtLeast(1)
        val nodeHeight = bounds.height().coerceAtLeast(1)
        val fullScreenLike = nodeWidth >= width * 0.90f && nodeHeight >= height * 0.90f
        if (!fullScreenLike) {
            val marginX = tx * width
            val marginY = ty * height
            val targetPx = targetX * width
            val targetPy = targetY * height
            val left = bounds.left - marginX
            val right = bounds.right + marginX
            val top = bounds.top - marginY
            val bottom = bounds.bottom + marginY
            if (targetPx in left..right && targetPy in top..bottom) return true
        }

        // Fallback for apps that expose a small node while the click event is
        // represented by its center.
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
            ContextConditionType.BRIGHTNESS_LOW -> {
                val current = runCatching {
                    android.provider.Settings.System.getInt(
                        context.contentResolver,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
                        255,
                    )
                }.getOrDefault(255)
                current <= 64
            }
            ContextConditionType.VOLUME_LEVEL -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audio == null) false else {
                    val expected = config.contextConditionValue.toIntOrNull()?.coerceIn(1, 30) ?: return false
                    val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    kotlin.math.round(current.toDouble() / maxVolume.toDouble() * 30.0).toInt().coerceIn(0, 30) == expected
                }
            }
        }
    }
}
