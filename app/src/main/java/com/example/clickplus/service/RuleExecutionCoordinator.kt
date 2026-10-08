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
            val executionId = result.executionId ?: java.util.UUID.randomUUID().toString()
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
                    detail = if (test) "בדיקה ידנית" else actualActionDetails(config),
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
                detail = actualActionDetails(config)
            )
        )
    }

    private fun actualActionDetails(config: KeyActionConfig): String = when (config.actionType) {
        ActionType.SYSTEM -> when (config.systemActionId) {
            SystemActionPreset.HOME.id -> "פתיחת מסך הבית"
            SystemActionPreset.BACK.id -> "חזרה למסך הקודם"
            SystemActionPreset.RECENTS.id -> "פתיחת היישומים האחרונים"
            SystemActionPreset.NOTIFICATIONS.id -> "פתיחת חלונית ההתראות"
            SystemActionPreset.LOCK_SCREEN.id -> "נעילת המסך"
            SystemActionPreset.POWER_MENU.id -> "פתיחת תפריט הכיבוי"
            SystemActionPreset.SCREENSHOT.id -> "צילום מסך"
            else -> config.actionSummary()
        }
        ActionType.APP -> "פתיחת " + config.targetAppName.ifBlank { "האפליקציה שנבחרה" }
        ActionType.APP_TAP -> "פתיחה + " + config.screenTapCount.coerceIn(1, 10) + " לחיצות ב-" + config.screenTapAppName.ifBlank { "האפליקציה שנבחרה" } + " במיקום שנלמד"
        ActionType.MULTI_POINT_TAP -> "פתיחה + שתי לחיצות ב-" + config.screenTapAppName.ifBlank { "האפליקציה שנבחרה" } + " במיקומים שנלמדו"
        ActionType.PROFILE -> {
            val targetProfileId = profileIdFromActionId(config.systemActionId)
            val targetProfile = targetProfileId?.let { id ->
                advanced.profiles().firstOrNull { it.id == id }
            }
            "מעבר לפרופיל " + (targetProfile?.name ?: "שנבחר")
        }
    }

    fun metadata(ruleId: String): RuleAdvancedMetadata = advanced.getRuleMetadata(ruleId)
}
