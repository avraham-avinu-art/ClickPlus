package com.example.clickplus.service

import android.app.usage.UsageStatsManager
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

    fun process(context: Context) {
        if (AdvancedRuleRepository.currentMode(context) != AppMode.BASIC) return
        val mappings = AppPreferencesRepository.mappingsSnapshot(context)
            .filter { it.enabled && it.triggerType == TriggerType.APP_ENTRY }
        if (mappings.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastTime < 100L) return
        lastTime = now
        count = (count + 1).coerceAtMost(10)

        val foreground = lastForegroundPackage(context)
        val advanced = AdvancedRuleRepository(context)
        val chosen = mappings
            .filter { it.pressCount == count }
            .filter { matchesCondition(context, it, foreground) }
            .sortedByDescending { advanced.getRuleMetadata(it.id).priority }
            .firstOrNull()

        if (chosen != null) {
            val performer = BasicActionPerformer(context.applicationContext)
            val coordinator = RuleExecutionCoordinator(context.applicationContext, performer, advanced)
            coordinator.execute(chosen, foreground, "כניסה ל-ClickPlus במצב בסיסי")
        }

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ count = 0 }, 650L)
    }

    private fun lastForegroundPackage(context: Context): String {
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return ""
        val end = System.currentTimeMillis() - 200L
        val start = end - 10_000L
        return runCatching {
            usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .maxByOrNull { it.lastTimeUsed }
                ?.packageName.orEmpty()
        }.getOrDefault("")
    }

    private fun matchesCondition(context: Context, config: KeyActionConfig, foreground: String): Boolean {
        return when (config.contextConditionType) {
            ContextConditionType.ANY -> true
            ContextConditionType.APP -> foreground.isNotBlank() && foreground == config.contextConditionValue
            ContextConditionType.MUSIC,
            ContextConditionType.MUTED,
            ContextConditionType.RINGING,
            ContextConditionType.RADIO -> false
        }
    }
}
