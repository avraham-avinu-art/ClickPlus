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

        // Until Accessibility is granted, the launcher behaves like a normal
        // app icon: the first (and every later) tap opens the app UI.
        if (!accessibilityEnabled) {
            openMainInterface(flashOnly = false)
            return
        }

        // Once Accessibility is enabled, entering another app is detected by the
        // AccessibilityService. The launcher icon is no longer used as a click
        // trigger. If the UI process is already alive, open it normally; if the
        // app was fully killed, show the UI briefly and remove the task again.
        openMainInterface(flashOnly = !uiIsInThisProcess)
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
