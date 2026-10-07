package com.example.clickplus.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Build
import android.provider.Settings
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.AppMode
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.TriggerType

object StandaloneLauncherEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var count = 0
    private var lastTime = 0L
    private var previousForegroundPackage = ""

    fun process(context: Context) {
        if (AdvancedRuleRepository.currentMode(context) != AppMode.BASIC) return
        val mappings = AppPreferencesRepository.mappingsSnapshot(context)
            .filter { it.enabled && it.triggerType == TriggerType.APP_ENTRY }
        if (mappings.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastTime < 100L) return
        lastTime = now
        count = (count + 1).coerceAtMost(10)
        previousForegroundPackage = lastForegroundPackage(context)

        handler.removeCallbacksAndMessages("activation")
        val resolve = Runnable {
            val finalCount = count.coerceIn(1, 10)
            val foreground = previousForegroundPackage
            count = 0
            previousForegroundPackage = ""

            val advanced = AdvancedRuleRepository(context)
            val chosen = mappings
                .filter { it.pressCount == finalCount }
                .filter { matchesCondition(context, it, foreground) }
                .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
                .firstOrNull()

            if (chosen != null) {
                val performer = BasicActionPerformer(context.applicationContext)
                val coordinator = RuleExecutionCoordinator(context.applicationContext, performer, advanced)
                coordinator.execute(chosen, foreground, "כניסה ל-ClickPlus במצב בסיסי")
            }
        }
        handler.postAtTime(
            resolve,
            "activation",
            System.currentTimeMillis() + AppPreferencesRepository.tapTimeoutSnapshot(context),
        )
    }

    fun recordAccessibilityUnavailable(context: Context) {
        if (AdvancedRuleRepository.currentMode(context) != AppMode.FULL) return
        val mappings = AppPreferencesRepository.mappingsSnapshot(context)
            .filter { it.enabled && it.triggerType == TriggerType.APP_ENTRY }
        if (mappings.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastTime < 100L) return
        lastTime = now
        count = (count + 1).coerceAtMost(10)
        previousForegroundPackage = AdvancedRuleRepository.lastExternalPackage(context)

        handler.removeCallbacksAndMessages("missing_accessibility")
        val resolve = Runnable {
            val finalCount = count.coerceIn(1, 10)
            val foreground = previousForegroundPackage
            count = 0
            previousForegroundPackage = ""

            val advanced = AdvancedRuleRepository(context)
            mappings
                .filter { it.pressCount == finalCount }
                .filter { matchesCondition(context, it, foreground) }
                .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
                .firstOrNull()
                ?.let { rule ->
                    AdvancedRuleRepository.addLog(
                        context,
                        com.example.clickplus.data.ActivityLog(
                            timestamp = System.currentTimeMillis(),
                            type = "ACTION",
                            message = "הפעולה נכשלה",
                            ruleId = rule.id,
                            appPackage = foreground,
                            success = false,
                            detail = "הכלל הופעל, אך שירות הנגישות אינו פעיל ולכן אי אפשר לבצע את הפעולה במצב מלא.",
                        ),
                    )
                }
        }
        handler.postDelayed(resolve, AppPreferencesRepository.tapTimeoutSnapshot(context))
    }

    private fun lastForegroundPackage(context: Context): String {
        if (!hasUsageAccess(context)) return ""
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return ""
        val end = System.currentTimeMillis()
        val start = end - 30_000L
        return runCatching {
            val events = usage.queryEvents(start, end)
            val event = UsageEvents.Event()
            var latestPackage = ""
            var latestTimestamp = Long.MIN_VALUE
            val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType != foregroundType) continue
                val pkg = event.packageName.orEmpty()
                if (pkg.isBlank() || pkg == context.packageName || pkg == "com.android.systemui") continue
                if (event.timeStamp >= latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    latestPackage = pkg
                }
            }
            latestPackage
        }.getOrDefault("")
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        return runCatching {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    private fun matchesCondition(context: Context, config: KeyActionConfig, foreground: String): Boolean {
        return when (config.contextConditionType) {
            ContextConditionType.ANY -> true
            ContextConditionType.APP -> foreground.isNotBlank() && foreground == config.contextConditionValue
            ContextConditionType.MUSIC,
            ContextConditionType.MUTED,
            ContextConditionType.RINGING,
            ContextConditionType.RADIO -> false
            ContextConditionType.BRIGHTNESS_LOW -> {
                val brightness = runCatching {
                    Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        255,
                    )
                }.getOrDefault(255)
                brightness <= 64
            }
            ContextConditionType.VOLUME_LEVEL -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    ?: return false
                val expected = config.contextConditionValue.toIntOrNull()?.coerceIn(1, 30) ?: return false
                val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val current = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                kotlin.math.round(current.toDouble() / max.toDouble() * 30.0).toInt().coerceIn(0, 30) == expected
            }
        }
    }
}
