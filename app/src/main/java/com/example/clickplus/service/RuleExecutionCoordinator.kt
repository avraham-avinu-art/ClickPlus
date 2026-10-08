package com.example.clickplus.service

import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.ActivityLog
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.RuleAdvancedMetadata
import java.util.UUID

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
            val activeProfileId = advanced.activeProfileId()
            val selectedProfile = advanced.profiles().firstOrNull { it.id == meta.profileId }
            if (selectedProfile == null) {
                logFailure(config, sourcePackage, "הפרופיל של הפעולה אינו קיים")
                return false
            }
            if (selectedProfile.id != activeProfileId) {
                logFailure(config, sourcePackage, "הפרופיל \"" + selectedProfile.name + "\" אינו הפרופיל הפעיל")
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
            val logId = result.pendingId ?: UUID.randomUUID().toString()
            AdvancedRuleRepository.addLog(
                context,
                ActivityLog(
                    timestamp = System.currentTimeMillis(),
                    type = "ACTION",
                    message = actionMessage,
                    ruleId = config.id,
                    appPackage = sourcePackage,
                    success = if (result.pending) null else result.success,
                    detail = if (test && result.success && result.reason.isBlank()) {
                        "בדיקה ידנית הסתיימה בהצלחה."
                    } else if (test && result.success) {
                        "בדיקה ידנית: " + result.reason
                    } else if (result.pending || result.success) {
                        result.reason
                    } else {
                        result.reason.ifBlank { "לא נמסר הסבר מהמבצע" }
                    },
                    id = logId,
                    triggerDescription = if (test) "בדיקה ידנית" else reason,
                    actionDescription = config.actionSummary(),
                )
            )
            if (!result.success && attempt < attempts) {
                handler.postDelayed({ runAttempt(attempt + 1) }, 250L)
            }
        }

        val delay = AppPreferencesRepository.actionDelaySnapshot(context)
        // For "open app + tap", the delay belongs after the target app is visible,
        // not before opening it. ActionExecutor applies it at the correct point.
        if (config.actionType == com.example.clickplus.data.ActionType.APP_TAP || delay == 0L) {
            runAttempt(1)
        } else {
            handler.postDelayed({ runAttempt(1) }, delay)
        }
        return true
    }

    private fun logFailure(config: KeyActionConfig, sourcePackage: String, detail: String) {
        AdvancedRuleRepository.addLog(
            context,
            ActivityLog(
                timestamp = System.currentTimeMillis(),
                type = "ACTION",
                message = "הפעולה נכשלה",
                ruleId = config.id,
                appPackage = sourcePackage,
                success = false,
                detail = detail,
                triggerDescription = "הפעלה",
                actionDescription = config.actionSummary(),
            ),
        )
    }

    fun metadata(ruleId: String): RuleAdvancedMetadata = advanced.getRuleMetadata(ruleId)
}
