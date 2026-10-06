package com.example.clickplus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class SettingsLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            },
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
