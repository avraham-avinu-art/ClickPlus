package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeyInterceptorAccessibilityService : AccessibilityService() {

    @Volatile
    private var tapCountOverlayEnabled = false

    @Volatile
    private var lastExternalPackage = ""

    companion object {
        @Volatile
        var instance: KeyInterceptorAccessibilityService? = null
            private set
    }

    private lateinit var actionExecutor: ActionExecutor
    private lateinit var tapDetector: TapDetector
    private lateinit var prefsRepository: AppPreferencesRepository
    private lateinit var tapCountOverlay: TapCountOverlay
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    override fun onCreate() {
        super.onCreate()
        instance = this

        actionExecutor = ActionExecutor(this)
        tapCountOverlay = TapCountOverlay(this)
        tapDetector = TapDetector(applicationContext, actionExecutor) { count ->
            if (tapCountOverlayEnabled && ::tapCountOverlay.isInitialized) {
                tapCountOverlay.show(count)
            }
        }
        prefsRepository = AppPreferencesRepository(applicationContext)

        startAsForeground()
        observePreferences()
        consumePendingLaunches()
    }

    fun onLauncherEntry() {
        tapDetector.processActivationLaunch(lastExternalPackage)
    }

    fun openMainInterface() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
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
            tapDetector.processActivationLaunch(lastExternalPackage)
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

        val openIntent = Intent(this, MainActivity::class.java).apply {
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
                .setSmallIcon(com.example.clickplus.R.drawable.ic_notification_transparent)
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
            prefsRepository.showTapCountFlow.collectLatest { enabled ->
                tapCountOverlayEnabled = enabled
                if (!enabled && ::tapCountOverlay.isInitialized) {
                    tapCountOverlay.hide()
                }
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
        startAsForeground()
        consumePendingLaunches()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val eventPackage = event.packageName?.toString().orEmpty()
        if (eventPackage.isBlank() || eventPackage == packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (shouldTrackExternalPackage(eventPackage)) {
                    lastExternalPackage = eventPackage
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (captureTapLocationIfRequested(event)) return
                tapDetector.processViewClicked(event)
            }
        }
    }

    private fun shouldTrackExternalPackage(eventPackage: String): Boolean {
        val defaultIme = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        )?.substringBefore('/').orEmpty()

        return eventPackage != defaultIme && eventPackage != "com.android.systemui"
    }

    private fun captureTapLocationIfRequested(event: AccessibilityEvent): Boolean {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        if (!prefs.getBoolean("tap_learning", false)) return false

        val requestedPackage = prefs.getString("tap_learning_package", "").orEmpty()
        if (requestedPackage.isNotBlank() && requestedPackage != event.packageName?.toString()) {
            return true
        }

        val source = event.source ?: return true
        val bounds = Rect()
        runCatching { source.getBoundsInScreen(bounds) }.getOrNull()
        if (bounds.isEmpty) return true

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = displayMetrics.heightPixels.coerceAtLeast(1).toFloat()
        val xRatio = (bounds.centerX() / width).coerceIn(0f, 1f)
        val yRatio = (bounds.centerY() / height).coerceIn(0f, 1f)
        val appPackage = event.packageName?.toString().orEmpty()

        prefs.edit()
            .putBoolean("tap_learning", false)
            .putBoolean("tap_capture_ready", true)
            .putFloat("tap_capture_x_ratio", xRatio)
            .putFloat("tap_capture_y_ratio", yRatio)
            .putString("tap_capture_package", appPackage)
            .putString("tap_capture_app_name", runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(appPackage, 0)).toString()
            }.getOrDefault(""))
            .apply()

        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                },
            )
        }

        return true
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onDestroy() {
        if (::tapCountOverlay.isInitialized) {
            tapCountOverlay.destroy()
        }
        if (instance === this) instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
