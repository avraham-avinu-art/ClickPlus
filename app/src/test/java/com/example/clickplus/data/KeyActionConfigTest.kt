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

    @Test
    fun exactlyOneProfileIsActive() {
        val profiles = listOf(
            ClickPlusProfile("a", "א", enabled = true),
            ClickPlusProfile("b", "ב", enabled = true),
            ClickPlusProfile("c", "ג", enabled = false),
        )

        val (normalized, activeId) =
            AdvancedRuleRepository.normalizeProfiles(profiles, "b")

        assertEquals("b", activeId)
        assertEquals(1, normalized.count { it.enabled })
        assertTrue(normalized.single { it.id == "b" }.enabled)
        assertFalse(normalized.single { it.id == "a" }.enabled)
        assertFalse(normalized.single { it.id == "c" }.enabled)
    }

    @Test
    fun storedActiveProfileWinsOverLegacyFlags() {
        val profiles = listOf(
            ClickPlusProfile("a", "א", enabled = true),
            ClickPlusProfile("b", "ב", enabled = false),
        )

        val (normalized, activeId) =
            AdvancedRuleRepository.normalizeProfiles(profiles, "b")

        assertEquals("b", activeId)
        assertEquals("b", normalized.single { it.enabled }.id)
    }
}
