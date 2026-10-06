package com.example.clickplus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        prefs.edit().putBoolean("background_only", true).apply()

        val service = com.example.clickplus.service.KeyInterceptorAccessibilityService.instance
        if (service != null) {
            // גם כשהשירות כבר מחובר, הכניסה מהלאנצ'ר היא טריגר רגיל.
            service.onLauncherEntry()
        } else {
            // אם התהליך של השירות נסגר, נשמור את הכניסה ונעבד אותה כשהשירות יחזור.
            val pending = prefs.getInt("pending_activation_launches", 0)
            prefs.edit()
                .putInt("pending_activation_launches", (pending + 1).coerceAtMost(10))
                .apply()
        }

        finish()
        overridePendingTransition(0, 0)
    }
}
