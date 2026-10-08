package com.example.clickplus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyActionConfigTest {
    @Test
    fun appTapSummaryContainsSelectedTapCount() {
        val config = KeyActionConfig(
            name = "בדיקה",
            pressCount = 3,
            actionTapCount = 4,
            actionType = ActionType.APP_TAP,
            triggerType = TriggerType.APP_ENTRY,
            screenTapAppName = "Source",
            contextConditionType = ContextConditionType.APP,
            contextConditionName = "Target",
        )

        assertEquals(
            "כניסה לאפליקציה · אפליקציה: Target · 3 כניסות",
            config.triggerSummary(),
        )
        assertEquals(
            "פתיחה+4 לחיצות · Source",
            config.actionSummary(),
        )
    }

    @Test
    fun singleTapUsesSingularWording() {
        val config = KeyActionConfig(
            actionTapCount = 1,
            actionType = ActionType.APP_TAP,
            triggerType = TriggerType.SCREEN_TAP,
            screenTapAppName = "Source",
        )

        assertEquals("לחיצה אחת", config.pressSummary())
        assertTrue(config.actionSummary().contains("פתיחה+לחיצה"))
    }
}
