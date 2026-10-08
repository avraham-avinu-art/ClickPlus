package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.ui.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeyInterceptorAccessibilityService : AccessibilityService() {
    @Volatile private var tapCountOverlayEnabled = false
    @Volatile private var lastExternalPackage = ""
    @Volatile private var currentForegroundPackage = ""

    companion object {
        @Volatile var instance: KeyInterceptorAccessibilityService? = null
            private set
    }

    private lateinit var actionExecutor: ActionExecutor
    private lateinit var executionCoordinator: RuleExecutionCoordinator
    private lateinit var tapDetector: TapDetector
    private lateinit var prefsRepository: AppPreferencesRepository
    private lateinit var tapCountOverlay: TapCountOverlay
    private lateinit var tapLearningOverlay: TapLearningOverlay
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    override fun onCreate() {
        super.onCreate()
        instance = this
        lastExternalPackage = AdvancedRuleRepository.lastExternalPackage(applicationContext)
        actionExecutor = ActionExecutor(this)
        executionCoordinator = RuleExecutionCoordinator(applicationContext, actionExecutor)
        tapCountOverlay = TapCountOverlay(this)
        tapLearningOverlay = TapLearningOverlay(this)
        prefsRepository = AppPreferencesRepository(applicationContext)
        tapDetector = TapDetector(applicationContext, executionCoordinator) { count ->
            if (tapCountOverlayEnabled) tapCountOverlay.show(count)
        }
        startAsForeground()
        observePreferences()
        consumePendingLaunches()
    }

    fun onLauncherEntry() {
        tapDetector.processActivationLaunch(lastExternalPackage)
    }

    fun isPackageInForeground(targetPackage: String): Boolean {
        if (targetPackage.isBlank()) return false
        val rootPackage = rootInActiveWindow?.packageName?.toString().orEmpty()
        return currentForegroundPackage == targetPackage || rootPackage == targetPackage
    }

    fun openMainInterface() {
        runCatching {
            startActivity(
                Intent(this, DashboardActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }
    }

    private fun consumePendingLaunches() {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        val count = prefs.getInt("pending_activation_launches", 0)
        if (count <= 0) return
        prefs.edit().putInt("pending_activation_launches", 0).apply()
        repeat(count.coerceAtMost(10)) { tapDetector.processActivationLaunch(lastExternalPackage) }
    }

    private fun startAsForeground() {
        val channelId = "clickplus_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "שירות קליק פלוס", NotificationManager.IMPORTANCE_LOW)
            )
        }

        val openIntent = Intent(this, DashboardActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        val pendingIntent = PendingIntent.getActivity(
            this, 1001, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("קליק פלוס פעיל")
            .setSmallIcon(com.example.clickplus.R.drawable.ic_notification_transparent)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1001, notification)
            }
        }.onFailure { error ->
            // The AccessibilityService remains bound by Android even if the
            // optional foreground notification cannot be promoted. Never make
            // a notification/FGS incompatibility crash the accessibility service.
            android.util.Log.w("ClickPlus", "Foreground notification unavailable", error)
        }
    }

    private fun observePreferences() {
        serviceScope.launch {
            prefsRepository.tapTimeoutFlow.collectLatest { tapDetector.tapTimeoutMs = it }
        }
        serviceScope.launch {
            prefsRepository.showTapCountFlow.collectLatest { enabled ->
                tapCountOverlayEnabled = enabled
                if (!enabled) tapCountOverlay.hide()
            }
        }
        serviceScope.launch {
            prefsRepository.tapCountSizeFlow.collectLatest { sizeSp ->
                tapCountOverlay.setSizeSp(sizeSp)
            }
        }
        serviceScope.launch {
            prefsRepository.mappingsFlow.collectLatest { tapDetector.updateProfiles(it) }
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
                currentForegroundPackage = if (shouldTrackExternalPackage(eventPackage)) {
                    lastExternalPackage = eventPackage
                    AdvancedRuleRepository.setLastExternalPackage(applicationContext, eventPackage)
                    eventPackage
                } else {
                    ""
                }
                updateLearningOverlay(eventPackage)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (captureTapLocationIfRequested(event)) return
                tapDetector.processViewClicked(event)
            }
        }
    }

    private fun updateLearningOverlay(eventPackage: String) {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        val learning = prefs.getBoolean("tap_learning", false)
        val target = prefs.getString("tap_learning_package", "").orEmpty()
        if (learning && target.isNotBlank() && target == eventPackage) {
            tapLearningOverlay.show()
        } else if (!learning) {
            tapLearningOverlay.hide()
        }
    }

    private fun shouldTrackExternalPackage(eventPackage: String): Boolean {
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/').orEmpty()
        return eventPackage != defaultIme && eventPackage != "com.android.systemui"
    }

    private fun captureTapLocationIfRequested(event: AccessibilityEvent): Boolean {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        if (!prefs.getBoolean("tap_learning", false)) return false
        val requestedPackage = prefs.getString("tap_learning_package", "").orEmpty()
        if (requestedPackage.isNotBlank() && requestedPackage != event.packageName?.toString()) return true

        val source = event.source ?: return true
        val bounds = Rect()
        runCatching { source.getBoundsInScreen(bounds) }.getOrNull() ?: return true
        if (bounds.isEmpty) return true

        val width = resources.displayMetrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = resources.displayMetrics.heightPixels.coerceAtLeast(1).toFloat()
        val xRatio = (bounds.centerX() / width).coerceIn(0f, 1f)
        val yRatio = (bounds.centerY() / height).coerceIn(0f, 1f)
        val appPackage = event.packageName?.toString().orEmpty()

        prefs.edit()
            .putBoolean("tap_learning", false)
            .putBoolean("tap_capture_ready", true)
            .putFloat("tap_capture_x_ratio", xRatio)
            .putFloat("tap_capture_y_ratio", yRatio)
            .putString("tap_capture_package", appPackage)
            .putString(
                "tap_capture_app_name",
                runCatching {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(appPackage, 0)).toString()
                }.getOrDefault("")
            )
            .apply()
        tapLearningOverlay.hide()
        captureScreenPreview()

        runCatching {
            startActivity(
                Intent(this, DashboardActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }
        return true
    }

    private fun captureScreenPreview() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        runCatching {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
                        runCatching {
                            val source = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace,
                            )
                            val bitmap = source?.copy(Bitmap.Config.ARGB_8888, false)
                            screenshot.hardwareBuffer.close()

                            if (bitmap != null) {
                                val file = java.io.File(cacheDir, "tap-preview-" + System.currentTimeMillis() + ".png")
                                file.outputStream().use {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                                }
                                bitmap.recycle()

                                getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
                                    .edit()
                                    .putString("tap_capture_screenshot_path", file.absolutePath)
                                    .apply()
                            }
                        }.onFailure { error ->
                            android.util.Log.w("ClickPlus", "Could not save tap preview", error)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        android.util.Log.w(
                            "ClickPlus",
                            "Screenshot capture failed: " + errorCode,
                        )
                    }
                },
            )
        }.onFailure { error ->
            android.util.Log.w("ClickPlus", "Screenshot capture unavailable", error)
        }
    }

    override fun onInterrupt() = Unit
    override fun onUnbind(intent: Intent?): Boolean = true

    override fun onDestroy() {
        tapLearningOverlay.destroy()
        tapCountOverlay.destroy()
        if (instance === this) instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
