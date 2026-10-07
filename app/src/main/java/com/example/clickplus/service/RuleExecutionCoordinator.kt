package com.example.clickplus.service

import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.ActivityLog
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppPreferencesRepository
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

        val meta = advanced.getRuleMetadata(config.id)
        if (!test) {
            val profiles = advanced.profiles()
            val selectedProfile = profiles.firstOrNull { it.id == meta.profileId }
            val activeProfileId = AdvancedRuleRepository.activeProfileId(context)
            if (selectedProfile == null) {
                logFailure(config, sourcePackage, "הפעולה אינה משויכת לפרופיל קיים")
                return false
            }
            if (selectedProfile.id != activeProfileId) {
                return false
            }
            if (!selectedProfile.enabled) {
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
        }

        val attempts = meta.retries.coerceIn(1, 3)
        fun runAttempt(attempt: Int) {
            val result = performer.execute(config)
            val actionMessage = when {
                result.pending -> "הפעולה בביצוע"
                result.success -> "הפעולה הצליחה"
                else -> "הפעולה נכשלה"
            }
            AdvancedRuleRepository.addLog(
                context,
                ActivityLog(
                    timestamp = System.currentTimeMillis(),
                    type = "ACTION",
                    message = actionMessage,
                    ruleId = config.id,
                    appPackage = sourcePackage,
                    actionLabel = config.name.ifBlank { config.actionSummary() },
                    success = if (result.pending) null else result.success,
                    detail = if (test && result.success && result.reason.isBlank()) {
                        "בדיקה ידנית הסתיימה בהצלחה."
                    } else if (test && result.success) {
                        "בדיקה ידנית: " + result.reason
                    } else if (result.pending || result.success) {
                        result.reason
                    } else {
                        result.reason.ifBlank { "לא נמסר הסבר מהמבצע" }
                    }
                )
            )
            if (!result.success && attempt < attempts) {
                handler.postDelayed({ runAttempt(attempt + 1) }, 250L)
            }
        }

        val globalDelay = AppPreferencesRepository.actionDelaySnapshot(context)
        val delay = if (meta.delayMs > 0L) meta.delayMs.coerceIn(0L, 10_000L) else globalDelay
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
                actionLabel = config.name.ifBlank { config.actionSummary() },
                detail = detail
            )
        )
    }

    fun metadata(ruleId: String): RuleAdvancedMetadata = advanced.getRuleMetadata(ruleId)
}
