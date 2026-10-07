package com.example.clickplus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppMode
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import com.example.clickplus.service.StandaloneLauncherEngine

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)

        // First launch opens the real UI. All first-run permission setup is handled
        // by DashboardActivity so it cannot interfere with background triggering.
        val firstLaunch = !prefs.getBoolean("first_ui_opened", false)
        if (firstLaunch) {
            prefs.edit()
                .putBoolean("background_only", false)
                .putBoolean("permission_bootstrap_done", false)
                .apply()

            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
            finish()
            overridePendingTransition(0, 0)
            return
        }

        prefs.edit().putBoolean("background_only", true).apply()

        if (AdvancedRuleRepository.currentMode(this) == AppMode.BASIC) {
            StandaloneLauncherEngine.process(this)
        } else {
            val service = KeyInterceptorAccessibilityService.instance
            if (service != null) {
                service.onLauncherEntry()
            } else {
                val pending = prefs.getInt("pending_activation_launches", 0)
                prefs.edit()
                    .putInt("pending_activation_launches", (pending + 1).coerceAtMost(10))
                    .apply()
            }
        }

        finish()
        overridePendingTransition(0, 0)
    }
}
