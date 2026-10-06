package com.example.clickplus.service

import android.content.Context
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.os.Handler
import android.os.Looper
import com.example.clickplus.data.ContextConditionType
import com.example.clickplus.data.KeyActionConfig

class TapDetector(
    private val context: Context,
    private val actionExecutor: ActionExecutor,
    private val onTapCount: (Int) -> Unit = {},
) {
    var tapTimeoutMs = 650L

    private var tapCount = 0
    private var lastLaunchTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable {
        tapCount = 0
    }

    private var profiles = emptyList<KeyActionConfig>()

    fun updateProfiles(newProfiles: List<KeyActionConfig>) {
        profiles = newProfiles.filter { it.enabled }
    }

    fun processActivationLaunch() {
        val all = profiles
        if (all.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 100L) return
        lastLaunchTime = now

        tapCount = (tapCount + 1).coerceAtMost(10)
        handler.removeCallbacks(resetRunnable)
        onTapCount(tapCount)

        val current = selectProfile(all, tapCount)
        if (current != null) {
            actionExecutor.execute(current)
        }

        handler.postDelayed(resetRunnable, tapTimeoutMs.coerceIn(300L, 1500L))
    }

    private fun selectProfile(
        all: List<KeyActionConfig>,
        count: Int,
    ): KeyActionConfig? {
        val matchingCount = all.filter { it.pressCount == count }
        if (matchingCount.isEmpty()) return null

        val specific = matchingCount
            .asSequence()
            .filter { it.contextConditionType != ContextConditionType.ANY }
            .filter { contextMatches(it) }
            .firstOrNull()

        return specific ?: matchingCount.firstOrNull {
            it.contextConditionType == ContextConditionType.ANY
        }
    }

    private fun contextMatches(config: KeyActionConfig): Boolean {
        val service = KeyInterceptorAccessibilityService.instance ?: return false

        return when (config.contextConditionType) {
            ContextConditionType.ANY -> true

            ContextConditionType.APP -> {
                val currentPackage =
                    service.rootInActiveWindow?.packageName?.toString().orEmpty()
                currentPackage.isNotBlank() &&
                    currentPackage == config.contextConditionValue
            }

            ContextConditionType.MUSIC -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audio?.isMusicActive == true
            }

            ContextConditionType.MUTED -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audio?.let {
                    it.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                        it.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
                } == true
            }

            ContextConditionType.RINGING -> {
                val telephony =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                runCatching {
                    telephony?.callState == TelephonyManager.CALL_STATE_RINGING
                }.getOrDefault(false)
            }

            ContextConditionType.RADIO -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val currentPackage =
                    service.rootInActiveWindow?.packageName?.toString().orEmpty()
                audio?.isMusicActive == true &&
                    if (config.contextConditionValue.isBlank()) {
                        currentPackage.lowercase().let {
                            it.contains("radio") || it.contains("fm") ||
                                it.contains("dab") || it.contains("tuner")
                        }
                    } else {
                        currentPackage == config.contextConditionValue
                    }
            }
        }
    }
}
