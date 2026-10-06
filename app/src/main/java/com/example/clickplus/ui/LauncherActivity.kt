package com.example.clickplus.ui

import android.app.Activity
import android.os.Bundle
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppMode
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import com.example.clickplus.service.StandaloneLauncherEngine

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        prefs.edit().putBoolean("background_only", true).apply()

        if (AdvancedRuleRepository.currentMode(this) == AppMode.BASIC) {
            StandaloneLauncherEngine.process(this)
        } else {
            val service = KeyInterceptorAccessibilityService.instance
            if (service != null) {
                service.onLauncherEntry()
            } else {
                val pending = prefs.getInt("pending_activation_launches", 0)
                prefs.edit().putInt("pending_activation_launches", (pending + 1).coerceAtMost(10)).apply()
            }
        }

        finish()
        overridePendingTransition(0, 0)
    }
}
