package com.example.clickplus.data

import java.util.Locale

object ActionTextFormatter {
    fun actionDetails(config: KeyActionConfig, profileName: String? = null): String =
        when (config.actionType) {
            ActionType.SYSTEM -> {
                val action = SystemActionPreset.entries.firstOrNull { it.id == config.systemActionId }
                when (action?.id) {
                    SystemActionPreset.DIAL_NUMBER.id ->
                        "חיוג ל-" + config.actionParameter.ifBlank { "מספר" }
                    SystemActionPreset.DIAL_CONTACT.id ->
                        "חיוג ל-" + config.contactName.ifBlank { "איש קשר" }
                    else -> action?.titleHebrew ?: config.systemActionId.ifBlank { "פעולת מכשיר" }
                }
            }
            ActionType.APP ->
                "פתיחת " + config.targetAppName.ifBlank { "האפליקציה שנבחרה" }
            ActionType.APP_TAP -> {
                val app = config.screenTapAppName.ifBlank { "האפליקציה שנבחרה" }
                "פתיחת $app וביצוע לחיצה במיקום שנלמד"
            }
            ActionType.MULTI_POINT_TAP ->
                "פתיחת " + config.screenTapAppName.ifBlank { "האפליקציה שנבחרה" } +
                    " וביצוע שתי לחיצות במיקומים שנלמדו"
            ActionType.PROFILE ->
                "מעבר לפרופיל " + (profileName?.ifBlank { null } ?: "שנבחר")
        }

    fun actionLabel(config: KeyActionConfig, profileName: String? = null): String =
        config.name.ifBlank { actionDetails(config, profileName) }

    fun durationMs(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        if (safe == 0L) return "ללא השהיה"
        return if (safe % 1000L == 0L) {
            (safe / 1000L).toString() + " שניות"
        } else {
            String.format(Locale.getDefault(), "%.1f שניות", safe / 1000f)
        }
    }
}
