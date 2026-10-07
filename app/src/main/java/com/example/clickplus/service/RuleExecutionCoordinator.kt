package com.example.clickplus.service

import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.ActivityLog
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.RuleAdvancedMetadata

class RuleExecutionCoordinator(
    private val context: android.content.Context,
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
        if (!config.enabled) {
            logFailure(config, sourcePackage, "הכלל מושבת")
            return false
        }

        if (!performer.supports(config)) {
            logFailure(config, sourcePackage, "הפעולה אינה נתמכת במצב העבודה הנוכחי")
            return false
        }

        if (test) {
            AdvancedRuleRepository.addLog(
                context,
                ActivityLog(
                    System.currentTimeMillis(),
                    "TEST",
                    "בדיקת פעולה: " + config.actionSummary(),
                    config.id,
                    sourcePackage,
                    success = null,
                    detail = "הבדיקה נרשמה ביומן; לא בוצע ניסיון הפעלה בפועל."
                )
            )
            return true
        }

        val meta = advanced.getRuleMetadata(config.id)
        val profiles = advanced.profiles()
        val selectedProfile = profiles.firstOrNull { it.id == meta.profileId }
        if (selectedProfile != null && !selectedProfile.enabled) {
            logFailure(config, sourcePackage, "הפרופיל \"" + selectedProfile.name + "\" מושבת")
            return false
        }

        val now = System.currentTimeMillis()
        val last = lastExecution[config.id] ?: 0L
        if (meta.cooldownMs > 0L && now - last < meta.cooldownMs) {
            val remaining = (meta.cooldownMs - (now - last)).coerceAtLeast(0L)
            logFailure(config, sourcePackage, "הפעולה נחסמה בגלל Cooldown; נותרו " + remaining + "ms")
            return false
        }

        lastExecution[config.id] = now
        AdvancedRuleRepository.addLog(
            context,
            ActivityLog(
                now,
                "TRIGGER",
                "התחיל ניסיון: " + config.actionSummary(),
                config.id,
                sourcePackage,
                success = null,
                detail = reason
            )
        )

        val attempts = meta.retries.coerceIn(1, 3)
        fun runAttempt(attempt: Int) {
            val result = performer.execute(config)
            AdvancedRuleRepository.addLog(
                context,
                ActivityLog(
                    System.currentTimeMillis(),
                    "ACTION",
                    when {
                        result.pending -> "הפעולה בביצוע"
                        result.success -> "הפעולה הצליחה"
                        else -> "הפעולה נכשלה"
                    },
                    config.id,
                    sourcePackage,
                    success = if (result.pending) null else result.success,
                    detail = if (result.pending || result.success) {
                        result.reason
                    } else {
                        result.reason.ifBlank { "לא נמסר הסבר מהמבצע" }
                    }                )
            )
            if (!result.success && attempt < attempts) {
                handler.postDelayed({ runAttempt(attempt + 1) }, 250L)
            }
        }

        val delay = meta.delayMs.coerceIn(0L, 10_000L)
        if (delay == 0L) runAttempt(1) else handler.postDelayed({ runAttempt(1) }, delay)
        return true
    }

    private fun logFailure(config: KeyActionConfig, sourcePackage: String, detail: String) {
        AdvancedRuleRepository.addLog(
            context,
            ActivityLog(
                System.currentTimeMillis(),
                "ACTION",
                "הפעולה נכשלה",
                config.id,
                sourcePackage,
                success = false,
                detail = detail
            )
        )
    }

    fun metadata(ruleId: String): RuleAdvancedMetadata = advanced.getRuleMetadata(ruleId)
}
