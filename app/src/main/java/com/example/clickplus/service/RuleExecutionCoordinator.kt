package com.example.clickplus.service

import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.ActivityLog
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.SystemActionPreset
import com.example.clickplus.data.profileIdFromActionId
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.RuleAdvancedMetadata
import com.example.clickplus.data.ActionTextFormatter

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
            logFailure(config, sourcePackage, reason, "הכלל מושבת")
            return false
        }

        if (!performer.supports(config)) {
            logFailure(config, sourcePackage, reason, "הפעולה אינה נתמכת במצב העבודה הנוכחי")
            return false
        }

        val meta = advanced.getRuleMetadata(config.id)
        if (!test) {
            val profiles = advanced.profiles()
            val selectedProfile = profiles.firstOrNull { it.id == meta.profileId }
            val activeProfileId = AdvancedRuleRepository.activeProfileId(context)
            if (selectedProfile == null) {
                logFailure(config, sourcePackage, reason, "הפעולה אינה משויכת לפרופיל קיים")
                return false
            }
            if (selectedProfile.id != activeProfileId) {
                return false
            }
            if (!selectedProfile.enabled) {
                logFailure(config, sourcePackage, reason, "הפרופיל \"" + selectedProfile.name + "\" מושבת")
                return false
            }

            val now = System.currentTimeMillis()
            val last = lastExecution[config.id] ?: 0L
            if (meta.cooldownMs > 0L && now - last < meta.cooldownMs) {
                val remaining = (meta.cooldownMs - (now - last)).coerceAtLeast(0L)
                logFailure(config, sourcePackage, reason, "הפעולה נחסמה בגלל מרווח ההמתנה; נותרו " + ActionTextFormatter.durationMs(remaining))
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
            val executionId = result.executionId ?: java.util.UUID.randomUUID().toString()
            AdvancedRuleRepository.addLog(
                context,
                ActivityLog(
                    timestamp = System.currentTimeMillis(),
                    type = "ACTION",
                    message = actionMessage,
                    ruleId = config.id,
                    appPackage = sourcePackage,
                    actionLabel = ActionTextFormatter.actionLabel(
                        config,
                        advanced.profiles().firstOrNull { it.id == meta.profileId }?.name,
                    ),
                    success = if (result.pending) null else result.success,
                    detail = ActionTextFormatter.actionDetails(
                        config,
                        advanced.profiles().firstOrNull { it.id == meta.profileId }?.name,
                    ),
                    triggerLabel = if (test) "בדיקה ידנית" else reason,
                    actionDetails = ActionTextFormatter.actionDetails(
                        config,
                        advanced.profiles().firstOrNull { it.id == meta.profileId }?.name,
                    ),
                    actualAction = result.reason.ifBlank { actionMessage },
                    failureReason = if (result.success || result.pending) "" else result.reason,
                    id = executionId,
                )
            )
            if (!result.success && attempt < attempts) {
                handler.postDelayed({ runAttempt(attempt + 1) }, 250L)
            }
        }

        val globalDelay = AppPreferencesRepository.actionDelaySnapshot(context)
        val configuredDelay = if (meta.delayMs > 0L) meta.delayMs.coerceIn(0L, 10_000L) else globalDelay
        val delay = if (
            config.actionType == com.example.clickplus.data.ActionType.APP_TAP ||
            config.actionType == com.example.clickplus.data.ActionType.MULTI_POINT_TAP
        ) {
            0L
        } else {
            configuredDelay
        }
        if (delay == 0L) runAttempt(1) else handler.postDelayed({ runAttempt(1) }, delay)
        return true
    }

    private fun logFailure(
        config: KeyActionConfig,
        sourcePackage: String,
        triggerLabel: String,
        failureReason: String,
    ) {
        val metadata = advanced.getRuleMetadata(config.id)
        val profileName = advanced.profiles().firstOrNull { it.id == metadata.profileId }?.name
        val actionDetails = ActionTextFormatter.actionDetails(config, profileName)
        AdvancedRuleRepository.addLog(
            context,
            ActivityLog(
                timestamp = System.currentTimeMillis(),
                type = "ACTION",
                message = "הפעולה נכשלה",
                ruleId = config.id,
                appPackage = sourcePackage,
                success = false,
                actionLabel = ActionTextFormatter.actionLabel(config, profileName),
                detail = actionDetails,
                triggerLabel = triggerLabel,
                actionDetails = actionDetails,
                actualAction = "הפעולה לא בוצעה",
                failureReason = failureReason,
            )
        )
    }

    fun metadata(ruleId: String): RuleAdvancedMetadata = advanced.getRuleMetadata(ruleId)
}
