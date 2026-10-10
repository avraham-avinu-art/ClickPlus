package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import com.example.clickplus.data.ActionType
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.KeyActionConfig
import com.example.clickplus.data.profileIdFromActionId
import com.example.clickplus.data.SystemActionPreset

class ActionExecutor(private val service: AccessibilityService) : ClickActionPerformer {
    override fun supports(config: KeyActionConfig): Boolean {
        return config.actionType == ActionType.APP_TAP ||
            config.actionType == ActionType.MULTI_POINT_TAP ||
            config.actionType == ActionType.APP ||
            config.actionType == ActionType.PROFILE ||
            SystemActionPreset.entries.any { it.id == config.systemActionId }
    }

    override fun execute(config: KeyActionConfig): ActionExecutionResult {
        if (!config.enabled) return ActionExecutionResult.failure("הכלל מושבת")
        if (!supports(config)) return ActionExecutionResult.failure("פעולת המערכת אינה מוכרת")

        return try {
            when (config.actionType) {
                ActionType.APP_TAP -> LearnedTapExecutor(service).executeAppTap(config)
                ActionType.MULTI_POINT_TAP -> LearnedTapExecutor(service).executeMultiPointTap(config)
                ActionType.APP -> {
                    if (config.targetPackage.isBlank()) {
                        return ActionExecutionResult.failure("לא נבחרה אפליקציית יעד")
                    }
                    val intent = service.packageManager.getLaunchIntentForPackage(config.targetPackage)
                        ?: return ActionExecutionResult.failure("אפליקציית היעד אינה מותקנת או שאין לה מסך פתיחה")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    service.startActivity(intent)
                    ActionExecutionResult.success("אפליקציית היעד נפתחה")
                }
                ActionType.PROFILE -> executeProfile(config.systemActionId)
                ActionType.SYSTEM -> AccessibilitySystemActionExecutor(service).execute(
                    config.systemActionId,
                    if (config.systemActionId == SystemActionPreset.DIAL_CONTACT.id) {
                        config.contactNumber.ifBlank { config.actionParameter }
                    } else config.actionParameter,
                )
            }
        } catch (error: Throwable) {
            ActionExecutionResult.failure(
                "Android לא הצליח לבצע את הפעולה: " +
                    (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
            )
        }
    }

    private fun executeProfile(profileActionId: String): ActionExecutionResult {
        val selectedProfileId = profileIdFromActionId(profileActionId)
        if (selectedProfileId != null) {
            val profile = AdvancedRuleRepository(service).profiles().firstOrNull { it.id == selectedProfileId }
                ?: return ActionExecutionResult.failure("הפרופיל שנבחר אינו קיים")
            if (!profile.enabled) {
                return ActionExecutionResult.failure("הפרופיל שנבחר מושבת")
            }
            AdvancedRuleRepository.setActiveProfileId(service, profile.id)
            return ActionExecutionResult.success("עבר לפרופיל: " + profile.name)
        }

        // Backward compatibility for rules saved by older versions.
        return when (profileActionId) {
            "profile_next" ->
                ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(service, 1))
            "profile_previous" ->
                ActionExecutionResult.success("עבר לפרופיל: " + AdvancedRuleRepository.cycleProfile(service, -1))
            "profile_default" -> {
                AdvancedRuleRepository.setActiveProfileId(service, "default")
                ActionExecutionResult.success("חזר לפרופיל ברירת המחדל")
            }
            else -> ActionExecutionResult.failure("פעולת פרופיל ClickPlus אינה מוכרת")
        }
    }

 catch (error: Throwable) {
        ActionExecutionResult.failure("Android לא הצליח לפתוח את היעד: " + (error.message ?: "שגיאה"))
    }

.getOrElse { ActionExecutionResult.failure("לא ניתן לענות לשיחה: " + (it.message ?: "שגיאה")) }

.getOrElse { ActionExecutionResult.failure("לא ניתן לדחות שיחה: " + (it.message ?: "שגיאה")) }

 catch (error: Throwable) {
        ActionExecutionResult.failure(
            "Android לא הצליח לפתוח את המסך: " +
                (error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
        )
    }

}
