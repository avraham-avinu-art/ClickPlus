package com.example.clickplus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityLogTest {
    @Test
    fun logKeepsTriggerAndActionDescriptions() {
        val original = ActivityLog(
            timestamp = 1234L,
            type = "ACTION",
            message = "הפעולה בביצוע",
            ruleId = "rule-1",
            appPackage = "com.example.target",
            success = null,
            detail = "ממתינים לפתיחת האפליקציה",
            id = "execution-1",
            triggerDescription = "כניסה לאפליקציה",
            actionDescription = "פתיחה+3 לחיצות · Target",
        )

        val restored = ActivityLog.fromJson(original.toJson())

        assertEquals(original.id, restored.id)
        assertEquals(original.triggerDescription, restored.triggerDescription)
        assertEquals(original.actionDescription, restored.actionDescription)
        assertEquals(original.success, restored.success)
    }

    @Test
    fun oldLogWithoutNewFieldsStillLoads() {
        val old = org.json.JSONObject()
            .put("timestamp", 1234L)
            .put("type", "ACTION")
            .put("message", "הפעולה הצליחה")
            .put("ruleId", "rule-1")
            .put("success", true)
            .put("detail", "בוצע")

        val restored = ActivityLog.fromJson(old)

        assertEquals("הפעולה הצליחה", restored.message)
        assertEquals("", restored.triggerDescription)
        assertEquals("", restored.actionDescription)
    }
}
