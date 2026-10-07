package com.example.clickplus.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppMode
import com.example.clickplus.service.KeyInterceptorAccessibilityService
import com.example.clickplus.service.StandaloneLauncherEngine

class LauncherActivity : Activity() {
    private var settingsStepActive = false
    private var runtimeRequestActive = false

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        runtimeRequestActive = false
        continueFirstLaunchPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)

        // First launch opens the real UI. The permission bootstrap then runs from
        // the first Dashboard entry and stays pending until all required
        // permissions/special accesses have been addressed.
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

    override fun onResume() {
        super.onResume()
        if (isFinishing || runtimeRequestActive) return
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        if (!prefs.getBoolean("first_ui_opened", false)) return
        if (prefs.getBoolean("permission_bootstrap_done", false)) return
        if (settingsStepActive) {
            settingsStepActive = false
        }
        continueFirstLaunchPermissions()
    }

    private fun continueFirstLaunchPermissions() {
        if (runtimeRequestActive) return

        val runtimePermissions = buildList {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    this@LauncherActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(
                    this@LauncherActivity,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
        }

        if (runtimePermissions.isNotEmpty()) {
            runtimeRequestActive = true
            runtimePermissionLauncher.launch(runtimePermissions.toTypedArray())
            return
        }

        val serviceEnabled = isAccessibilityEnabled()
        if (!serviceEnabled) {
            settingsStepActive = true
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        val usageEnabled = hasUsageAccess()
        if (!usageEnabled) {
            settingsStepActive = true
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
            .edit()
            .putBoolean("permission_bootstrap_done", true)
            .apply()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.contains(packageName, true) }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        return runCatching {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName,
            ) == android.app.AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }
}
