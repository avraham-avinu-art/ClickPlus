package com.example.clickplus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityLogTest {
    @Test
    fun logCarriesTriggerAndActionDescriptions() {
        val log = ActivityLog(
            timestamp = 1234L,
            type = "ACTION",
            message = "הפעולה הצליחה",
            ruleId = "rule-1",
            appPackage = "com.example.target",
            success = true,
            detail = "בוצע",
            id = "execution-1",
            triggerDescription = "כניסה לאפליקציה",
            actionDescription = "פתיחה+3 לחיצות · Target",
        )

        assertEquals("execution-1", log.id)
        assertEquals("כניסה לאפליקציה", log.triggerDescription)
        assertEquals("פתיחה+3 לחיצות · Target", log.actionDescription)
        assertEquals(true, log.success)
    }
}
