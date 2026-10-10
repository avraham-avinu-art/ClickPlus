package com.example.clickplus.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.ActivityLog
import com.example.clickplus.data.ActionTextFormatter
import com.example.clickplus.data.AppMode
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.TriggerType
import kotlinx.coroutines.flow.first
import kotlin.math.round

/**
 * Lightweight trigger path used when Accessibility is not enabled.
 * State is serialized on the main looper and app-entry history is persisted.
 */
object StandaloneLauncherEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private var lastTriggerAt = 0L
    private var pendingForegroundPackage = ""
    private var pendingEnteredPackage = ""
    private var pendingTriggerType = TriggerType.CLICKPLUS_ENTRY
    private var pendingFailureReason = ""

    fun process(context: Context) {
        val previousForeground = lastForegroundPackage(context)
        registerTrigger(
            context = context.applicationContext,
            triggerType = TriggerType.CLICKPLUS_ENTRY,
            enteredPackage = "",
            previousForegroundPackage = previousForeground,
        )
    }

    /**
     * When Full mode is selected but Accessibility is unavailable, still resolve
     * ClickPlus-entry sequences and write an explicit failure instead of silently
     * running the reduced Basic executor by accident.
     */
    fun recordAccessibilityUnavailable(context: Context) {
        if (AdvancedRuleRepository.currentMode(context) != AppMode.FULL) return
        registerTrigger(
            context = context.applicationContext,
            triggerType = TriggerType.CLICKPLUS_ENTRY,
            enteredPackage = "",
            previousForegroundPackage = lastForegroundPackage(context),
            failureReason = "שירות הנגישות אינו פעיל או שאינו מגיב; הפעולה לא בוצעה.",
        )
    }

    /**
     * Called by BasicForegroundMonitorService when Usage Access reports a
     * transition into a different foreground application. First entry after
     * reset is remembered, not counted as a trigger.
     */
    fun processAppEntry(
        context: Context,
        enteredPackage: String,
        previousForegroundPackage: String,
    ) {
        if (enteredPackage.isBlank() || enteredPackage == context.packageName) return

        val mappings = AppPreferencesRepository.mappingsSnapshot(context).filter { item ->
            item.enabled &&
                item.triggerType == TriggerType.APP_ENTRY &&
                item.triggerPackage == enteredPackage &&
                AdvancedRuleRepository.isRuleInActiveProfile(context, item.id)
        }
        if (mappings.isEmpty()) return

        val opened = AdvancedRuleRepository.openedAppPackages(context).toMutableSet()
        val targetPackages = AppPreferencesRepository.mappingsSnapshot(context)
            .filter {
                it.enabled &&
                    it.triggerType == TriggerType.APP_ENTRY &&
                    AdvancedRuleRepository.isRuleInActiveProfile(context, it.id)
            }
            .map { it.triggerPackage }
            .filter { it.isNotBlank() }
            .toSet()
        opened.retainAll(targetPackages)
        val alreadyOpened = opened.contains(enteredPackage)
        if (opened.add(enteredPackage)) {
            AdvancedRuleRepository.setOpenedAppPackages(context, opened)
        }
        if (!alreadyOpened) return

        registerTrigger(
            context = context.applicationContext,
            triggerType = TriggerType.APP_ENTRY,
            enteredPackage = enteredPackage,
            previousForegroundPackage = previousForegroundPackage,
        )
    }

    fun resetOpenAppTracking(context: Context) {
        AdvancedRuleRepository.clearOpenedAppPackages(context)
        AdvancedRuleRepository.setLastExternalPackage(context, "")
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit().remove("basic_last_foreground_package").apply()
    }

    @Synchronized
    private fun registerTrigger(
        context: Context,
        triggerType: TriggerType,
        enteredPackage: String,
        previousForegroundPackage: String,
        failureReason: String = "",
    ) {
        val mappings = AppPreferencesRepository.mappingsSnapshot(context).filter { item ->
            item.enabled &&
                item.triggerType == triggerType &&
                (triggerType == TriggerType.CLICKPLUS_ENTRY ||
                    (item.triggerPackage.isNotBlank() && item.triggerPackage == enteredPackage)) &&
                AdvancedRuleRepository.isRuleInActiveProfile(context, item.id)
        }
        if (mappings.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerAt < 100L) return
        lastTriggerAt = now

        if (tapCount == 0) {
            pendingEnteredPackage = enteredPackage
            pendingTriggerType = triggerType
            pendingFailureReason = failureReason
        } else if (
            pendingTriggerType != triggerType ||
            (triggerType == TriggerType.APP_ENTRY && pendingEnteredPackage != enteredPackage)
        ) {
            tapCount = 0
            pendingEnteredPackage = enteredPackage
            pendingTriggerType = triggerType
            pendingFailureReason = failureReason
        } else if (failureReason.isNotBlank()) {
            pendingFailureReason = failureReason
        }

        tapCount = (tapCount + 1).coerceAtMost(10)
        pendingForegroundPackage = previousForegroundPackage
        handler.removeCallbacksAndMessages("activation")
        handler.postAtTime(
            { resolveActivation(context.applicationContext) },
            "activation",
            SystemClock.uptimeMillis() + AppPreferencesRepository.tapTimeoutSnapshot(context).coerceIn(300L, 1500L),
        )
    }

    private fun resolveActivation(context: Context) {
        val finalCount = tapCount.coerceIn(1, 10)
        val foreground = pendingForegroundPackage
        val enteredPackage = pendingEnteredPackage
        val triggerType = pendingTriggerType
        val failureReason = pendingFailureReason
        tapCount = 0
        pendingForegroundPackage = ""
        pendingEnteredPackage = ""
        pendingFailureReason = ""

        val advanced = AdvancedRuleRepository(context)
        val matching = AppPreferencesRepository.mappingsSnapshot(context).filter { item ->
            item.enabled &&
                item.triggerType == triggerType &&
                (triggerType == TriggerType.CLICKPLUS_ENTRY || item.triggerPackage == enteredPackage) &&
                item.pressCount == finalCount &&
                AdvancedRuleRepository.isRuleInActiveProfile(context, item.id)
        }

        val chosen = matching
            .filter { it.contextConditionType != ContextConditionType.ANY }
            .filter { contextMatches(context, it, foreground) }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
            .firstOrNull()
            ?: matching
                .filter { it.contextConditionType == ContextConditionType.ANY }
                .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
                .firstOrNull()

        if (chosen != null) {
            val reason = when (triggerType) {
                TriggerType.CLICKPLUS_ENTRY -> "כניסה ל-ClickPlus במצב בסיסי"
                TriggerType.APP_ENTRY -> "כניסה חוזרת ל-" + chosen.triggerAppName.ifBlank { "האפליקציה שנבחרה" } + " במצב בסיסי"
            }
            if (failureReason.isNotBlank()) {
                val profileName = advanced.profiles().firstOrNull {
                    it.id == advanced.getRuleMetadata(chosen.id).profileId
                }?.name
                val details = ActionTextFormatter.actionDetails(chosen, profileName)
                AdvancedRuleRepository.addLog(
                    context,
                    ActivityLog(
                        timestamp = System.currentTimeMillis(),
                        type = "ACTION",
                        message = "הפעולה נכשלה",
                        ruleId = chosen.id,
                        appPackage = foreground,
                        success = false,
                        detail = details,
                        actionLabel = ActionTextFormatter.actionLabel(chosen, profileName),
                        triggerLabel = "לחיצות כניסה לקליק פלוס",
                        actionDetails = details,
                        actualAction = "הפעולה לא בוצעה",
                        failureReason = failureReason,
                    ),
                )
            } else {
                val performer = BasicActionPerformer(context.applicationContext)
                RuleExecutionCoordinator(context.applicationContext, performer, advanced)
                    .execute(chosen, foreground, reason)
            }
        }
    }

    private fun lastForegroundPackage(context: Context): String {
        if (!hasUsageAccess(context)) return AdvancedRuleRepository.lastExternalPackage(context)
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return AdvancedRuleRepository.lastExternalPackage(context)
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
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    private fun contextMatches(context: Context, config: KeyActionConfig, foregroundPackage: String): Boolean {
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
                val foreground = foregroundPackage.lowercase()
                audio?.isMusicActive == true &&
                    if (config.contextConditionValue.isBlank()) {
                        foreground.contains("radio") || foreground.contains("fm") ||
                            foreground.contains("dab") || foreground.contains("tuner")
                    } else foregroundPackage == config.contextConditionValue
            }
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
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
                val expected = config.contextConditionValue.toIntOrNull()?.coerceIn(1, 30) ?: return false
                val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                round(current.toDouble() / maxVolume.toDouble() * 30.0).toInt().coerceIn(0, 30) == expected
            }
        }
    }
}
