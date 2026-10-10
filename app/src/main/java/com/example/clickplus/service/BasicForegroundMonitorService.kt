package com.example.clickplus.service

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.clickplus.data.AdvancedRuleRepository
import com.example.clickplus.data.AppMode
import com.example.clickplus.ui.DashboardActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Usage-Access-only foreground app transition monitor for Basic mode.
 * It does not read screen content or inject gestures; Accessibility remains
 * required only for the Full-mode features.
 */
class BasicForegroundMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statePrefs by lazy { getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE) }
    private var scanStart = 0L
    private var lastHandledEventAt = 0L
    private var foregroundPackage = ""
    private var homePackage = ""

    override fun onCreate() {
        super.onCreate()
        homePackage = resolveHomePackage()
        foregroundPackage = AdvancedRuleRepository.lastExternalPackage(this)
        scanStart = System.currentTimeMillis()
        lastHandledEventAt = scanStart
        startAsForeground()
        markAlive()
        scope.launch {
            while (isActive) {
                try {
                    if (!hasUsageAccess()) {
                        statePrefs.edit()
                            .putBoolean("basic_monitor_usage_access", false)
                            .apply()
                    } else {
                        pollUsageEvents()
                        statePrefs.edit().putBoolean("basic_monitor_usage_access", true).apply()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    statePrefs.edit()
                        .putString("basic_monitor_error", error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName)
                        .apply()
                    android.util.Log.w("ClickPlusBasicMonitor", "Usage events polling failed", error)
                }
                delay(650L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        markAlive()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        statePrefs.edit()
            .putBoolean("basic_monitor_alive", false)
            .remove("basic_monitor_heartbeat")
            .apply()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun pollUsageEvents() {
        val now = System.currentTimeMillis()
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val from = (scanStart - 1200L).coerceAtLeast(now - 15_000L)
        val events = usage.queryEvents(from, now)
        val event = UsageEvents.Event()
        val found = mutableListOf<Pair<Long, String>>()
        var newest = lastHandledEventAt
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_FOREGROUND
        }
        val cutoff = lastHandledEventAt
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != foregroundType) continue
            val pkg = event.packageName.orEmpty()
            if (pkg.isBlank() || event.timeStamp <= cutoff) continue
            found += event.timeStamp to pkg
            if (event.timeStamp > newest) newest = event.timeStamp
        }
        scanStart = now
        lastHandledEventAt = newest
        if (found.isEmpty()) {
            markAlive()
            return
        }

        withContext(Dispatchers.Main.immediate) {
            found.sortedBy { it.first }.forEach { (_, pkg) -> handleForegroundTransition(pkg) }
            markAlive()
        }
    }

    private fun handleForegroundTransition(pkg: String) {
        if (pkg == packageName) return
        val imePackage = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/').orEmpty()
        if (pkg == imePackage) return

        when {
            pkg == homePackage -> {
                StandaloneLauncherEngine.resetOpenAppTracking(this)
                foregroundPackage = ""
            }
            pkg == "com.android.systemui" -> {
                StandaloneLauncherEngine.resetOpenAppTracking(this)
                foregroundPackage = ""
            }
            pkg != foregroundPackage -> {
                val previous = foregroundPackage.ifBlank { AdvancedRuleRepository.lastExternalPackage(this) }
                StandaloneLauncherEngine.processAppEntry(this, pkg, previous)
                foregroundPackage = pkg
                AdvancedRuleRepository.setLastExternalPackage(this, pkg)
                statePrefs.edit().remove("basic_monitor_error").apply()
            }
        }
    }

    private fun resolveHomePackage(): String {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(
            homeIntent,
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName.orEmpty()
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        return runCatching {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    private fun markAlive() {
        statePrefs.edit()
            .putBoolean("basic_monitor_alive", true)
            .putLong("basic_monitor_heartbeat", System.currentTimeMillis())
            .apply()
    }

    private fun startAsForeground() {
        val channelId = "clickplus_basic_monitor_channel_v1"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "זיהוי אפליקציות בקליק פלוס",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "התראה שקטה הדרושה לזיהוי כניסות במצב בסיסי"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = Intent(this, DashboardActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        val pending = PendingIntent.getActivity(
            this,
            2002,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("קליק פלוס · זיהוי אפליקציות")
            .setContentText("מצב בסיסי פעיל")
            .setSmallIcon(com.example.clickplus.R.drawable.ic_notification_transparent)
            .setContentIntent(pending)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setDefaults(0)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(2002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(2002, notification)
            }
        }.onFailure {
            android.util.Log.w("ClickPlusBasicMonitor", "Could not promote monitor to foreground", it)
        }
    }
}
