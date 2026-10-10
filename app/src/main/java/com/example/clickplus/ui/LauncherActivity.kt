package com.example.clickplus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppMode
import android.provider.Settings

class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        val accessibilityEnabled = isAccessibilityEnabled()


        // Every launcher tap is the dedicated "ClickPlus entry" trigger.
        // The app UI remains usable normally even without Accessibility.
        val mode = AdvancedRuleRepository.currentMode(this)
        when {
            mode == AppMode.BASIC -> com.example.clickplus.service.StandaloneLauncherEngine.process(this)
            accessibilityEnabled && KeyInterceptorAccessibilityService.instance != null ->
                KeyInterceptorAccessibilityService.instance?.onClickPlusEntry()
            else -> com.example.clickplus.service.StandaloneLauncherEngine.recordAccessibilityUnavailable(this)
        }

        // Always open the interface normally. First-run setup must remain open.
        openMainInterface()
    }

    private fun openMainInterface() {
        runCatching {
            startActivity(
                Intent(this, DashboardActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
            )
        }
        finish()
        overridePendingTransition(0, 0)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.contains(packageName, true) }
    }
}
