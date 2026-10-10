package com.example.clickplus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test
import com.example.clickplus.service.BasicActionCapabilities

class KeyActionConfigTest {

    @Test
    fun screenTapConfigRoundTrips() {
        val original = KeyActionConfig(
            name = "בדיקה",
            pressCount = 3,
            actionType = ActionType.MULTI_POINT_TAP,
            targetPackage = "com.example.target",
            targetAppName = "Target",
            triggerType = TriggerType.APP_ENTRY,
            triggerPackage = "com.example.trigger",
            triggerAppName = "Trigger",
            screenTapPackage = "com.example.source",
            screenTapAppName = "Source",
            screenTapXRatio = 0.42f,
            screenTapYRatio = 0.61f,
            screenTapSecondXRatio = 0.72f,
            screenTapSecondYRatio = 0.31f,
            screenTapIntervalMs = 1750L,
            screenTapCount = 1,
            screenTapToleranceRatio = 0.11f,
            enabled = false,
        )

        val restored = KeyActionConfig.fromJson(original.toJson())

        assertEquals(original.name, restored.name)
        assertEquals(original.pressCount, restored.pressCount)
        assertEquals(original.actionType, restored.actionType)
        assertEquals(original.triggerType, restored.triggerType)
        assertEquals(original.triggerPackage, restored.triggerPackage)
        assertEquals(original.screenTapPackage, restored.screenTapPackage)
        assertEquals(original.screenTapAppName, restored.screenTapAppName)
        assertEquals(original.screenTapXRatio, restored.screenTapXRatio, 0.0001f)
        assertEquals(original.screenTapYRatio, restored.screenTapYRatio, 0.0001f)
        assertEquals(original.screenTapSecondXRatio, restored.screenTapSecondXRatio, 0.0001f)
        assertEquals(original.screenTapSecondYRatio, restored.screenTapSecondYRatio, 0.0001f)
        assertEquals(original.screenTapIntervalMs, restored.screenTapIntervalMs)
        assertEquals(original.screenTapCount, restored.screenTapCount)
        assertEquals(original.screenTapToleranceRatio, restored.screenTapToleranceRatio, 0.0001f)
        assertFalse(restored.enabled)
    }

    @Test
    fun actionTextIsConsistentAcrossActionTypes() {
        val app = KeyActionConfig(
            name = "פתיחה",
            actionType = ActionType.APP,
            targetAppName = "מפות",
        )
        val tap = KeyActionConfig(
            name = "לחיצה",
            actionType = ActionType.APP_TAP,
            screenTapAppName = "מפות",
            screenTapXRatio = 0.5f,
            screenTapYRatio = 0.5f,
        )
        val multi = tap.copy(actionType = ActionType.MULTI_POINT_TAP, screenTapSecondXRatio = 0.7f, screenTapSecondYRatio = 0.6f)
        val system = KeyActionConfig(
            name = "בית",
            actionType = ActionType.SYSTEM,
            systemActionId = SystemActionPreset.HOME.id,
        )

        assertEquals("פתיחת מפות", ActionTextFormatter.actionDetails(app))
        assertEquals("פתיחת מפות וביצוע לחיצה במיקום שנלמד", ActionTextFormatter.actionDetails(tap))
        assertEquals("פתיחת מפות וביצוע שתי לחיצות במיקומים שנלמדו", ActionTextFormatter.actionDetails(multi))
        assertEquals("בית", ActionTextFormatter.actionDetails(system))
    }

    @Test
    fun activityLogPreservesTriggerActionActualAndFailure() {
        val log = ActivityLog(
            timestamp = 100L,
            type = "ACTION",
            message = "הפעולה נכשלה",
            ruleId = "rule-1",
            success = false,
            actionLabel = "פעולה לדוגמה",
            triggerLabel = "לחיצות כניסה לקליק פלוס",
            actionDetails = "פתיחת מפות",
            actualAction = "הפעולה לא בוצעה",
            failureReason = "שירות הנגישות לא פעיל",
        )

        val restored = ActivityLog.fromJson(log.toJson())

        assertEquals(log.triggerLabel, restored.triggerLabel)
        assertEquals(log.actionDetails, restored.actionDetails)
        assertEquals(log.actualAction, restored.actualAction)
        assertEquals(log.failureReason, restored.failureReason)
        assertFalse(restored.success == true)
    }

    @Test
    fun durationUsesHumanReadableUnits() {
        assertEquals("ללא השהיה", ActionTextFormatter.durationMs(0L))
        assertEquals("שנייה", ActionTextFormatter.durationMs(1000L))
        assertEquals("1.5 שניות", ActionTextFormatter.durationMs(1500L))
    }


    @Test
    fun basicModeExcludesAccessibilityAndCallControlActions() {
        assertTrue(BasicActionCapabilities.supports(KeyActionConfig(actionType = ActionType.APP)))
        assertTrue(BasicActionCapabilities.supports(KeyActionConfig(actionType = ActionType.PROFILE)))
        assertFalse(BasicActionCapabilities.supports(KeyActionConfig(actionType = ActionType.APP_TAP)))
        assertFalse(BasicActionCapabilities.supports(KeyActionConfig(actionType = ActionType.MULTI_POINT_TAP)))
        assertFalse(BasicActionCapabilities.supports(KeyActionConfig(
            actionType = ActionType.SYSTEM,
            systemActionId = SystemActionPreset.ANSWER_CALL.id,
        )))
        assertFalse(BasicActionCapabilities.supports(KeyActionConfig(
            actionType = ActionType.SYSTEM,
            systemActionId = SystemActionPreset.DECLINE_CALL.id,
        )))
    }

    @Test
    fun invalidLegacyCoordinatesAreClampedOrRejectedSafely() {
        val restored = KeyActionConfig.fromJson(
            JSONObject()
                .put("actionType", ActionType.APP_TAP.name)
                .put("screenTapXRatio", 1.8)
                .put("screenTapYRatio", -0.4)
                .put("screenTapSecondXRatio", "bad")
                .put("screenTapSecondYRatio", Double.NaN)
        )
        assertEquals(1f, restored.screenTapXRatio, 0.0001f)
        assertEquals(-0.4f, restored.screenTapYRatio, 0.0001f)
        assertEquals(-1f, restored.screenTapSecondXRatio, 0.0001f)
        assertEquals(-1f, restored.screenTapSecondYRatio, 0.0001f)
    }

    @Test
    fun oldActivityLogsLoadWithSafeDefaults() {
        val restored = ActivityLog.fromJson(
            JSONObject()
                .put("timestamp", 123L)
                .put("type", "ACTION")
                .put("message", "הפעולה נכשלה")
                .put("success", false)
        )
        assertEquals("", restored.triggerLabel)
        assertEquals("", restored.actionDetails)
        assertEquals("", restored.actualAction)
        assertEquals("", restored.failureReason)
        assertNull(restored.ruleId.takeIf { it.isNotBlank() })
    }

    @Test
    fun defaultActionIsEnabled() {
        assertTrue(KeyActionConfig().enabled)
    }
}
