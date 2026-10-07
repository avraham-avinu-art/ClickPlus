package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.graphics.Path
import android.accessibilityservice.GestureDescription
import android.os.Build
import android.provider.Settings
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
        tapLearningOverlay = TapLearningOverlay(
            this,
            onTargetTap = { x, y -> captureLearningTap(x, y) },
            onCancel = {
                getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
                    .edit().putBoolean("tap_learning", false).remove("tap_learning_stage").apply()
                tapLearningOverlay.hide()
            },
        )
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
            prefsRepository.tapCountXFlow.collectLatest { x ->
                val y = AppPreferencesRepository.tapCountYSnapshot(applicationContext)
                tapCountOverlay.setPosition(x, y)
            }
        }
        serviceScope.launch {
            prefsRepository.tapCountYFlow.collectLatest { y ->
                val x = AppPreferencesRepository.tapCountXSnapshot(applicationContext)
                tapCountOverlay.setPosition(x, y)
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
                if (shouldTrackExternalPackage(eventPackage) && !isLauncherPackage(eventPackage)) {
                    val previousApp = lastExternalPackage
                    if (eventPackage != lastExternalPackage) {
                        tapDetector.processAppEntry(eventPackage, previousApp)
                    }
                    lastExternalPackage = eventPackage
                    AdvancedRuleRepository.setLastExternalPackage(applicationContext, eventPackage)
                }
                updateLearningOverlay(eventPackage)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Normal clicks inside other apps are deliberately not a trigger.
                // The trigger is app entry; click gestures are actions only.
                captureTapLocationIfRequested(event)
            }
        }
    }

    private fun updateLearningOverlay(eventPackage: String) {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        val learning = prefs.getBoolean("tap_learning", false)
        val target = prefs.getString("tap_learning_package", "").orEmpty()
        if (learning && target.isNotBlank() && target == eventPackage) {
            val stage = prefs.getInt("tap_learning_stage", 1).coerceIn(1, 2)
            tapLearningOverlay.show(stage)
        } else if (!learning) {
            tapLearningOverlay.hide()
        }
    }

    private fun shouldTrackExternalPackage(eventPackage: String): Boolean {
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/').orEmpty()
        return eventPackage != defaultIme &&
            eventPackage != "com.android.systemui" &&
            eventPackage != packageName
    }

    private fun isLauncherPackage(eventPackage: String): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homePackage = packageManager.resolveActivity(
            homeIntent,
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
        return !homePackage.isNullOrBlank() && homePackage == eventPackage
    }

    private fun captureLearningTap(screenX: Float, screenY: Float) {
        val prefs = getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
        if (!prefs.getBoolean("tap_learning", false)) return

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val xRatio = (screenX / width).coerceIn(0f, 1f)
        val yRatio = (screenY / height).coerceIn(0f, 1f)
        val targetPackage = prefs.getString("tap_learning_package", "").orEmpty()
        val stage = prefs.getInt("tap_learning_stage", 1).coerceIn(1, 2)
        if (targetPackage.isBlank()) {
            cancelLearning()
            return
        }

        val appName = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(targetPackage, 0)
            ).toString()
        }.getOrDefault("")

        // Consume this tap exactly once. This prevents the first point from
        // being processed repeatedly while the editor is switching to stage 2.
        prefs.edit().putBoolean("tap_capture_ready", false).apply()

        when {
            stage == 1 && prefs.getBoolean("tap_learning_multi", false) -> {
                prefs.edit()
                    .putBoolean("tap_capture_ready", true)
                    .putFloat("tap_capture_x_ratio", xRatio)
                    .putFloat("tap_capture_y_ratio", yRatio)
                    .putInt("tap_capture_stage", 1)
                    .putString("tap_capture_package", targetPackage)
                    .putString("tap_capture_app_name", appName)
                    .putInt("tap_learning_stage", 2)
                    .apply()
                // Keep the target app and overlay open. Do not navigate home.
                tapLearningOverlay.show(2)
            }
            stage == 1 -> {
                prefs.edit()
                    .putBoolean("tap_learning", false)
                    .putBoolean("tap_capture_ready", true)
                    .putFloat("tap_capture_x_ratio", xRatio)
                    .putFloat("tap_capture_y_ratio", yRatio)
                    .putInt("tap_capture_stage", 1)
                    .putString("tap_capture_package", targetPackage)
                    .putString("tap_capture_app_name", appName)
                    .remove("tap_learning_stage")
                    .remove("tap_learning_multi")
                    .apply()
                tapLearningOverlay.hide()
                openEditor()
            }
            else -> {
                prefs.edit()
                    .putBoolean("tap_learning", false)
                    .putBoolean("tap_capture_ready", true)
                    .putFloat("tap_capture_x_ratio", xRatio)
                    .putFloat("tap_capture_y_ratio", yRatio)
                    .putInt("tap_capture_stage", 2)
                    .putString("tap_capture_package", targetPackage)
                    .putString("tap_capture_app_name", appName)
                    .remove("tap_learning_stage")
                    .remove("tap_learning_multi")
                    .apply()
                tapLearningOverlay.hide()
                openEditor()
            }
        }
    }

    private fun cancelLearning() {
        getSharedPreferences("clickplus_runtime", MODE_PRIVATE)
            .edit()
            .putBoolean("tap_learning", false)
            .putBoolean("tap_capture_ready", false)
            .remove("tap_learning_stage")
            .remove("tap_learning_multi")
            .apply()
        tapLearningOverlay.hide()
    }

    private fun openEditor() {
        runCatching {
            startActivity(
                Intent(this, DashboardActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }
    }

    private fun replayLearningTouch(screenX: Float, screenY: Float) {
        // Intentionally unused: learning captures the point without replaying
        // a click into the target app.
    }
