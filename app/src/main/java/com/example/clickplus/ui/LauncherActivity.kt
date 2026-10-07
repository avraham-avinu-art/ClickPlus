package com.example.clickplus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import android.provider.Settings

class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        val accessibilityEnabled = isAccessibilityEnabled()
        val uiProcessPid = prefs.getInt("clickplus_ui_process_pid", -1)
        val uiIsInThisProcess = uiProcessPid == android.os.Process.myPid()

        // Every launcher tap is the dedicated "ClickPlus entry" trigger.
        // The app UI remains usable normally even without Accessibility.
        if (accessibilityEnabled) {
            KeyInterceptorAccessibilityService.instance?.onClickPlusEntry()
        } else {
            com.example.clickplus.service.StandaloneLauncherEngine.process(this)
        }

        // With Accessibility enabled, keep the brief-flash behavior after the UI
        // process has been recreated. Without Accessibility always open normally.
        openMainInterface(flashOnly = accessibilityEnabled && !uiIsInThisProcess)
    }

    private fun openMainInterface(flashOnly: Boolean) {
        runCatching {
            startActivity(
                Intent(this, DashboardActivity::class.java).apply {
                    putExtra(DashboardActivity.EXTRA_FLASH_ONLY, flashOnly)
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
