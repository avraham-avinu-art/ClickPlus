package com.example.clickplus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyActionConfigTest {
    @Test
    fun screenTapConfigRoundTrips() {
        val original = KeyActionConfig(
            name = "בדיקה",
            pressCount = 3,
            actionType = ActionType.APP,
            targetPackage = "com.example.target",
            targetAppName = "Target",
            triggerType = TriggerType.APP_ENTRY,
            screenTapPackage = "com.example.source",
            screenTapAppName = "Source",
            screenTapXRatio = 0.42f,
            screenTapYRatio = 0.61f,
            screenTapToleranceRatio = 0.11f,
        )

        val restored = KeyActionConfig.fromJson(original.toJson())

        assertEquals(original.name, restored.name)
        assertEquals(original.pressCount, restored.pressCount)
        assertEquals(original.actionType, restored.actionType)
        assertEquals(original.triggerType, restored.triggerType)
        assertEquals(original.screenTapPackage, restored.screenTapPackage)
        assertEquals(original.screenTapAppName, restored.screenTapAppName)
        assertEquals(original.screenTapXRatio, restored.screenTapXRatio, 0.0001f)
        assertEquals(original.screenTapYRatio, restored.screenTapYRatio, 0.0001f)
        assertEquals(original.screenTapToleranceRatio, restored.screenTapToleranceRatio, 0.0001f)
        assertTrue(restored.enabled)
    }
}
