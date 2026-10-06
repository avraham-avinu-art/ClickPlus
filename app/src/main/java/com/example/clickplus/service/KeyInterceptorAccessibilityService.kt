package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.clickplus.data.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeyInterceptorAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: KeyInterceptorAccessibilityService? = null
            private set
    }

    private lateinit var actionExecutor: ActionExecutor
    private lateinit var tapDetector: TapDetector
    private lateinit var prefsRepository: AppPreferencesRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    override fun onCreate() {
        super.onCreate()
        instance = this

        actionExecutor = ActionExecutor(this)
        tapDetector = TapDetector(applicationContext, actionExecutor)
        prefsRepository = AppPreferencesRepository(applicationContext)

        startAsForeground()
        observePreferences()
        consumePendingLaunches()
    }

    fun onLauncherEntry() {
        tapDetector.processActivationLaunch()
    }

    fun openMainInterface() {
        runCatching {
            startActivity(
                Intent(this, com.example.clickplus.ui.MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                },
            )
        }
    }

    private fun consumePendingLaunches() {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        val count = prefs.getInt("pending_activation_launches", 0)
        if (count <= 0) return

        prefs.edit().putInt("pending_activation_launches", 0).apply()
        repeat(count.coerceAtMost(10)) {
            tapDetector.processActivationLaunch()
        }
    }

    private fun startAsForeground() {
        val channelId = "clickplus_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "שירות קליק פלוס",
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
        }

        val openIntent = Intent(this, com.example.clickplus.ui.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        startForeground(
            1001,
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("קליק פלוס פעיל")
                .setContentText("עובד ברקע וממתין לכניסה לאפליקציה")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build(),
        )
    }

    private fun observePreferences() {
        serviceScope.launch {
            prefsRepository.tapTimeoutFlow.collectLatest {
                tapDetector.tapTimeoutMs = it
            }
        }

        serviceScope.launch {
            prefsRepository.mappingsFlow.collectLatest {
                tapDetector.updateProfiles(it)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        consumePendingLaunches()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        // מבקש מ-Android לחבר מחדש את שירות הנגישות אם החיבור נותק.
        return true
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
