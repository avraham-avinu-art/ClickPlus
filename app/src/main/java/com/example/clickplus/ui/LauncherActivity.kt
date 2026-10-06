package com.example.clickplus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.clickplus.data.AppPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        val backgroundOnly = if (prefs.contains("background_only")) {
            prefs.getBoolean("background_only", false)
        } else {
            runBlocking {
                AppPreferencesRepository(applicationContext).backgroundOnlyFlow.first()
            }.also {
                prefs.edit().putBoolean("background_only", it).apply()
            }
        }

        if (backgroundOnly) {
            val service = com.example.clickplus.service.KeyInterceptorAccessibilityService.instance

            if (service != null) {
                service.onLauncherEntry()
            } else {
                val pending = prefs.getInt("pending_activation_launches", 0) + 1
                prefs.edit()
                    .putInt("pending_activation_launches", pending)
                    .apply()
            }

            finish()
            overridePendingTransition(0, 0)
            return
        }

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            },
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
