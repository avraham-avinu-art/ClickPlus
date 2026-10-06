package com.example.clickplus.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.clickplus.data.AppPreferencesRepository
import com.example.clickplus.data.KeyActionConfig
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
    private lateinit var overlayManager: OverlayManager
    private lateinit var tapDetector: TapDetector
    private lateinit var prefsRepository: AppPreferencesRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private var keyCaptureCallback: ((Int, String) -> Unit)? = null
    private var suppressKeyUpCode: Int? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        startAsForeground()
        actionExecutor = ActionExecutor(this)
        overlayManager = OverlayManager(this)
        tapDetector = TapDetector(actionExecutor, overlayManager)
        prefsRepository = AppPreferencesRepository(applicationContext)
        observePreferences()
    }

    fun startKeyCapture(onCaptured: (Int, String) -> Unit) {
        keyCaptureCallback = onCaptured
        suppressKeyUpCode = null
        overlayManager.showPill("לכידת מקש", "לחץ עכשיו על הכפתור")
    }

    fun cancelKeyCapture() {
        keyCaptureCallback = null
        suppressKeyUpCode = null
    }

    fun testMapping(config: KeyActionConfig) {
        actionExecutor.execute(config)
        overlayManager.showPill("בוצע", config.customLabel.ifBlank { config.keyNameHebrew })
    }

    fun sampleNode(onResult: (text: String, viewId: String) -> Unit) {
        overlayManager.startNodeSampler { sampled ->
            onResult(sampled?.text.orEmpty(), sampled?.viewId.orEmpty())
        }
    }

    fun stopNodeSampling() {
        overlayManager.stopNodeSampler()
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
        val capture = keyCaptureCallback

        if (capture != null) {
            if (event.action == KeyEvent.ACTION_DOWN && !event.repeatCount.let { it > 0 }) {
                keyCaptureCallback = null
                suppressKeyUpCode = event.keyCode
                val name = when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> "ווליום עליון"
                    KeyEvent.KEYCODE_VOLUME_DOWN -> "ווליום תחתון"
                    KeyEvent.KEYCODE_BACK -> "חזרה"
                    KeyEvent.KEYCODE_HOME -> "בית"
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "נגן / השהה מדיה"
                    KeyEvent.KEYCODE_APP_SWITCH -> "יישומים אחרונים"
                    else -> KeyEvent.keyCodeToString(event.keyCode)
                        .removePrefix("KEYCODE_")
                        .replace('_', ' ')
                }
                capture.invoke(event.keyCode, name)
                return true
            }
            return true
        }

        if (event.action == KeyEvent.ACTION_UP && suppressKeyUpCode == event.keyCode) {
            suppressKeyUpCode = null
            return true
        }

        if (tapDetector.processKeyEvent(event)) {
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        keyCaptureCallback = null
        suppressKeyUpCode = null
        if (instance === this) instance = null
        if (::overlayManager.isInitialized) overlayManager.dismiss()
        serviceScope.cancel()
        super.onDestroy()
    }
}