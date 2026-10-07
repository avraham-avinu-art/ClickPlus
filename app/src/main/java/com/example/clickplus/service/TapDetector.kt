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

    fun processActivationLaunch(previousForegroundPackage: String) {
        val all = profiles.filter { it.triggerType == TriggerType.APP_ENTRY }
        if (all.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 100L) return
        lastLaunchTime = now

        tapCount = (tapCount + 1).coerceAtMost(10)
        pendingForegroundPackage = previousForegroundPackage
        handler.removeCallbacks(resetRunnable)
        onTapCount(tapCount)

        // Do not execute a 1-press rule immediately. Wait for the full global
        // window so a 4-press rule can win when the user continues the sequence.
        handler.postDelayed(
            resetRunnable,
            tapTimeoutMs.coerceIn(300L, 1500L),
        )
    }

    private fun resolveActivationLaunch() {
        val finalCount = tapCount.coerceIn(1, 10)
        val previousForegroundPackage = pendingForegroundPackage
        tapCount = 0
        pendingForegroundPackage = ""

        val all = profiles.filter { it.triggerType == TriggerType.APP_ENTRY }
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

    fun processViewClicked(event: AccessibilityEvent) {
        val all = profiles.filter {
            it.triggerType == TriggerType.SCREEN_TAP || it.triggerType == TriggerType.APP_TAP
        }
        if (all.isEmpty()) return
        val packageName = event.packageName?.toString().orEmpty()
        val source = event.source ?: return
        runCatching { source.refresh() }
        val bounds = Rect()
        runCatching { source.getBoundsInScreen(bounds) }.getOrNull() ?: return
        if (bounds.isEmpty) return

        val width = context.resources.displayMetrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = context.resources.displayMetrics.heightPixels.coerceAtLeast(1).toFloat()
        val xRatio = (bounds.centerX() / width).coerceIn(0f, 1f)
        val yRatio = (bounds.centerY() / height).coerceIn(0f, 1f)

        // Some apps expose a small icon/text node for the click while the
        // actual clickable target is one of its parents. Check the source and
        // a few clickable ancestors without treating a full-screen root as a
        // match.
        val candidateBounds = buildList {
            var node: android.view.accessibility.AccessibilityNodeInfo? = source
            repeat(5) {
                if (node == null) return@repeat
                val nodeBounds = Rect()
                if (runCatching { node?.getBoundsInScreen(nodeBounds) }.isSuccess &&
                    !nodeBounds.isEmpty &&
                    runCatching { node?.isClickable == true }.getOrDefault(false)
                ) {
                    add(nodeBounds)
                }
                node = runCatching { node?.parent }.getOrNull()
            }
            if (isEmpty()) add(bounds)
        }

        all.filter { it.screenTapPackage.isBlank() || it.screenTapPackage == packageName }
            .filter { config ->
                candidateBounds.any { candidate ->
                    tapLocationMatches(config, xRatio, yRatio, candidate, width, height)
                }
            }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
            .forEach { config ->
                val count = (screenTapCounts[config.id] ?: 0) + 1
                screenTapCounts[config.id] = count
                onTapCount(count)
                screenReset[config.id]?.let(handler::removeCallbacks)
                // Resolve only after the global window closes. This prevents a
                // 1-press rule from firing before a longer multi-press sequence is complete.
                val reset = Runnable {
                    val finalCount = screenTapCounts.remove(config.id) ?: 0
                    screenReset.remove(config.id)
                    if (finalCount == config.pressCount) {
                        actionExecutor.execute(config, packageName, "לחיצה במיקום במסך")
                    }
                }
                screenReset[config.id] = reset
                handler.postDelayed(
                    reset,
                    tapTimeoutMs.coerceIn(300L, 1500L),
                )
            }
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
        val targetX = config.screenTapXRatio
        val targetY = config.screenTapYRatio

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
