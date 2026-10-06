package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.view.KeyEvent
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

    private lateinit var actionExecutor: ActionExecutor
    private lateinit var overlayManager: OverlayManager
    private lateinit var tapDetector: TapDetector
    private lateinit var prefsRepository: AppPreferencesRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        actionExecutor = ActionExecutor(this)
        overlayManager = OverlayManager(this)
        tapDetector = TapDetector(actionExecutor, overlayManager)
        prefsRepository = AppPreferencesRepository(applicationContext)
        observePreferences()
    }

    private fun startAsForeground() {
        val channelId = "clickplus_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "שירות Click+",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        startForeground(
            1001,
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("Click+ פעיל")
                .setContentText("מנוע המיפוי מאזין למקשי חומרה")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()
        )
    }

    private fun observePreferences() {
        serviceScope.launch {
            prefsRepository.operationModeFlow.collectLatest { tapDetector.currentMode = it }
        }
        serviceScope.launch {
            prefsRepository.tapTimeoutFlow.collectLatest { tapDetector.tapTimeoutMs = it }
        }
        serviceScope.launch {
            prefsRepository.debounceMsFlow.collectLatest { tapDetector.debounceMs = it }
        }
        serviceScope.launch {
            prefsRepository.hudStyleFlow.collectLatest { overlayManager.hudStyle = it }
        }
        serviceScope.launch {
            prefsRepository.mappingsFlow.collectLatest { tapDetector.updateMappings(it) }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && tapDetector.processKeyEvent(event.keyCode)) {
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::overlayManager.isInitialized) overlayManager.dismiss()
        serviceScope.cancel()
        super.onDestroy()
    }
}