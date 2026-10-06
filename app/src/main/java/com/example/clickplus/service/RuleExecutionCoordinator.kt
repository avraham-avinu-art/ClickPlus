package com.example.clickplus.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.ActivityLog
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.RuleAdvancedMetadata

class RuleExecutionCoordinator(
    private val context: Context,
    private val performer: ClickActionPerformer,
    private val advanced: AdvancedRuleRepository = AdvancedRuleRepository(context),
) {
    private val handler = Handler(Looper.getMainLooper())
    private val lastExecution = mutableMapOf<String, Long>()

    fun supports(config: KeyActionConfig): Boolean = performer.supports(config)

    fun execute(
        config: KeyActionConfig,
        sourcePackage: String = "",
        reason: String = "הפעלה",
        test: Boolean = false,
    ): Boolean {
        if (!config.enabled) return false
        if (test) {
            AdvancedRuleRepository.addLog(
                context,
                ActivityLog(System.currentTimeMillis(), "TEST", "בדיקת פעולה: " + config.actionSummary(), config.id, sourcePackage)
            )
            return true
        }

        val meta = advanced.getRuleMetadata(config.id)
        val profiles = advanced.profiles()
        val selectedProfile = profiles.firstOrNull { it.id == meta.profileId }
        if (selectedProfile != null && !selectedProfile.enabled) return false

        val now = System.currentTimeMillis()
        val last = lastExecution[config.id] ?: 0L
        if (meta.cooldownMs > 0L && now - last < meta.cooldownMs) return false

        lastExecution[config.id] = now
        AdvancedRuleRepository.addLog(
            context,
            ActivityLog(now, "TRIGGER", reason + ": " + config.actionSummary(), config.id, sourcePackage)
        )

        val attempts = meta.retries.coerceIn(1, 3)
        fun runAttempt(remaining: Int) {
            val success = performer.execute(config)
            AdvancedRuleRepository.addLog(
                context,
                ActivityLog(
                    System.currentTimeMillis(),
                    "ACTION",
                    if (success) "פעולה בוצעה" else "פעולה נכשלה",
                    config.id,
                    sourcePackage,
                    success = success
                )
            )
            if (!success && remaining > 1) {
                handler.postDelayed({ runAttempt(remaining - 1) }, 250L)
            }
        }

        val delay = meta.delayMs.coerceIn(0L, 10_000L)
        if (delay == 0L) runAttempt(attempts) else handler.postDelayed({ runAttempt(attempts) }, delay)
        return true
    }

    fun metadata(ruleId: String): RuleAdvancedMetadata = advanced.getRuleMetadata(ruleId)
}
