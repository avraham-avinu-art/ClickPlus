package com.example.clickplus.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "clickplus_settings")

enum class ActionType(val titleHebrew: String) { SYSTEM("פעולת מערכת"), APP("פתיחת אפליקציה"), APP_TAP("לחיצה באפליקציה") }
enum class TriggerType(val titleHebrew: String) { APP_ENTRY("כניסה לאפליקציה"), SCREEN_TAP("לחיצה במיקום במסך") }

enum class ContextConditionType(val titleHebrew: String) {
    ANY("בכל מצב"), APP("אפליקציה"), MUSIC("מוזיקה פועלת"), MUTED("שמע מושתק"),
    RINGING("הטלפון מצלצל"), RADIO("רדיו פועל")
}

enum class SystemActionPreset(val id: String, val titleHebrew: String) {
    HOME("home", "בית"), BACK("back", "חזרה"), RECENTS("recents", "יישומים אחרונים"),
    NOTIFICATIONS("notifications", "פתיחת התראות"), MEDIA_PLAY_PAUSE("media_play_pause", "נגן / השהה"),
    MEDIA_NEXT("media_next", "השיר הבא"), MEDIA_PREVIOUS("media_previous", "השיר הקודם"),
    VOLUME_UP("volume_up", "הגברת ווליום"), VOLUME_DOWN("volume_down", "הנמכת ווליום"),
    SETTINGS("settings", "הגדרות"), DIALER("dialer", "פתיחת חייגן")
}

data class KeyActionConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    /** Number of trigger presses/entries required to activate the rule. */
    val pressCount: Int = 1,
    /** Number of taps performed by an APP_TAP action after the target app is visible. */
    val actionTapCount: Int = 1,
    val actionType: ActionType = ActionType.SYSTEM,
    val systemActionId: String = SystemActionPreset.HOME.id,
    val targetPackage: String = "",
    val targetAppName: String = "",
    val contextConditionType: ContextConditionType = ContextConditionType.ANY,
    val contextConditionValue: String = "",
    val contextConditionName: String = "",
    val enabled: Boolean = true,
    val triggerType: TriggerType = TriggerType.APP_ENTRY,
    val screenTapPackage: String = "",
    val screenTapAppName: String = "",
    val screenTapXRatio: Float = -1f,
    val screenTapYRatio: Float = -1f,
    val screenTapToleranceRatio: Float = 0.08f
) {
    fun pressSummary(): String = when {
        triggerType == TriggerType.SCREEN_TAP && pressCount == 1 -> "לחיצה אחת"
        triggerType == TriggerType.SCREEN_TAP -> pressCount.toString() + " לחיצות"
        pressCount == 1 -> "כניסה אחת"
        else -> pressCount.toString() + " כניסות"
    }

    fun triggerSummary(): String = when (triggerType) {
        TriggerType.APP_ENTRY -> {
            val condition = contextSummary()
            "כניסה לאפליקציה · " + condition + " · " + pressSummary()
        }
        TriggerType.SCREEN_TAP -> {
            "לחיצה במיקום · " + screenTapAppName.ifBlank { "אפליקציה" } + " · " + pressSummary()
        }
    }

    fun actionSummary(): String = when (actionType) {
        ActionType.SYSTEM -> SystemActionPreset.entries.firstOrNull { it.id == systemActionId }?.titleHebrew ?: "פעולת מערכת"
        ActionType.APP -> "פתיחת " + targetAppName.ifBlank { "אפליקציה" }
        ActionType.APP_TAP -> {
            val countText = if (actionTapCount == 1) "לחיצה" else actionTapCount.toString() + " לחיצות"
            "פתיחה+" + countText + " · " + screenTapAppName.ifBlank { "אפליקציה" }
        }
    }

    fun contextSummary(): String {
        if (triggerType == TriggerType.SCREEN_TAP) {
            val x = if (screenTapXRatio >= 0f) (screenTapXRatio * 100f).toInt().toString() + "%" else "לא הוגדר"
            val y = if (screenTapYRatio >= 0f) (screenTapYRatio * 100f).toInt().toString() + "%" else "לא הוגדר"
            return "לחיצה ב-" + screenTapAppName.ifBlank { "אפליקציה" } + " · X " + x + " · Y " + y
        }
        return when (contextConditionType) {
            ContextConditionType.ANY -> "בכל מצב"
            ContextConditionType.APP -> "אפליקציה: " + contextConditionName.ifBlank { contextConditionValue }
            ContextConditionType.MUSIC -> "מוזיקה פועלת"
            ContextConditionType.MUTED -> "שמע מושתק"
            ContextConditionType.RINGING -> "הטלפון מצלצל"
            ContextConditionType.RADIO -> "רדיו: " + contextConditionName.ifBlank { "האפליקציה שנבחרה" }
        }
    }
    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("pressCount", pressCount); put("actionTapCount", actionTapCount)
        put("actionType", actionType.name); put("systemActionId", systemActionId)
        put("targetPackage", targetPackage); put("targetAppName", targetAppName)
        put("contextConditionType", contextConditionType.name)
        put("contextConditionValue", contextConditionValue); put("contextConditionName", contextConditionName)
        put("enabled", enabled); put("triggerType", triggerType.name)
        put("screenTapPackage", screenTapPackage); put("screenTapAppName", screenTapAppName)
        put("screenTapXRatio", screenTapXRatio); put("screenTapYRatio", screenTapYRatio)
        put("screenTapToleranceRatio", screenTapToleranceRatio)
    }
    companion object {
        fun fromJson(json: JSONObject) = KeyActionConfig(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            name = json.optString("name", json.optString("customLabel", "")),
            pressCount = json.optInt("pressCount", json.optInt("tapCount", 1)).coerceIn(1, 10),
            actionTapCount = json.optInt("actionTapCount", 1).coerceIn(1, 10),
            actionType = runCatching { ActionType.valueOf(json.optString("actionType", ActionType.SYSTEM.name)) }.getOrDefault(ActionType.SYSTEM),
            systemActionId = json.optString("systemActionId", SystemActionPreset.HOME.id),
            targetPackage = json.optString("targetPackage", ""),
            targetAppName = json.optString("targetAppName", ""),
            contextConditionType = runCatching { ContextConditionType.valueOf(json.optString("contextConditionType", ContextConditionType.ANY.name)) }.getOrDefault(ContextConditionType.ANY),
            contextConditionValue = json.optString("contextConditionValue", ""),
            contextConditionName = json.optString("contextConditionName", ""),
            enabled = json.optBoolean("enabled", json.optBoolean("isEnabled", true)),
            triggerType = runCatching { TriggerType.valueOf(json.optString("triggerType", TriggerType.APP_ENTRY.name)) }.getOrDefault(TriggerType.APP_ENTRY),
            screenTapPackage = json.optString("screenTapPackage", ""),
            screenTapAppName = json.optString("screenTapAppName", ""),
            screenTapXRatio = json.optDouble("screenTapXRatio", -1.0).toFloat()
                .takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: -1f,
            screenTapYRatio = json.optDouble("screenTapYRatio", -1.0).toFloat()
                .takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: -1f,
            screenTapToleranceRatio = json.optDouble("screenTapToleranceRatio", 0.08).toFloat().coerceIn(0.01f, 0.25f)
        )
    }
}

class AppPreferencesRepository(private val context: Context) {
    companion object {
        val BACKGROUND_ONLY = booleanPreferencesKey("background_only")
        val TAP_TIMEOUT_MS = longPreferencesKey("tap_timeout_ms")
        val ACTION_DELAY_MS = longPreferencesKey("action_delay_ms")
        val SHOW_TAP_COUNT = booleanPreferencesKey("show_tap_count")
        val MAPPINGS_JSON = stringPreferencesKey("mappings_json")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        fun tapTimeoutSnapshot(context: Context): Long =
            runBlocking {
                context.dataStore.data.first()[TAP_TIMEOUT_MS]
                    ?.coerceIn(300L, 1500L)
                    ?: 1200L
            }

        fun actionDelaySnapshot(context: Context): Long =
            runBlocking {
                context.dataStore.data.first()[ACTION_DELAY_MS]
                    ?.coerceIn(0L, 5000L)
                    ?: 0L
            }

        fun mappingsSnapshot(context: Context): List<KeyActionConfig> =
            runBlocking {
                runCatching {
                    val prefs = context.dataStore.data.first()
                    val array = JSONArray(prefs[MAPPINGS_JSON] ?: "[]")
                    buildList {
                        for (i in 0 until array.length()) {
                            array.optJSONObject(i)?.let { add(KeyActionConfig.fromJson(it)) }
                        }
                    }
                }.getOrDefault(emptyList())
            }
    }

    val backgroundOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[BACKGROUND_ONLY] ?: true }
    val tapTimeoutFlow: Flow<Long> = context.dataStore.data.map { it[TAP_TIMEOUT_MS] ?: 1200L }
    val actionDelayFlow: Flow<Long> = context.dataStore.data.map { it[ACTION_DELAY_MS] ?: 0L }
    val showTapCountFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_TAP_COUNT] ?: false }
    val mappingsFlow: Flow<List<KeyActionConfig>> = context.dataStore.data.map { prefs ->
        val array = runCatching { JSONArray(prefs[MAPPINGS_JSON] ?: "[]") }.getOrDefault(JSONArray())
        buildList {
            for (i in 0 until array.length()) {
                runCatching { array.getJSONObject(i) }.getOrNull()?.let { add(KeyActionConfig.fromJson(it)) }
            }
        }
    }
    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    suspend fun saveBackgroundOnly(enabled: Boolean) {
        context.dataStore.edit { it[BACKGROUND_ONLY] = enabled }
    }
    suspend fun saveTapTimeout(ms: Long) {
        val safe = ms.coerceIn(300L, 1500L)
        context.dataStore.edit { it[TAP_TIMEOUT_MS] = safe }
    }
    suspend fun saveActionDelay(ms: Long) {
        val safe = ms.coerceIn(0L, 5000L)
        context.dataStore.edit { it[ACTION_DELAY_MS] = safe }
    }
    suspend fun saveShowTapCount(enabled: Boolean) { context.dataStore.edit { it[SHOW_TAP_COUNT] = enabled } }
    suspend fun setOnboardingCompleted(completed: Boolean) { context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed } }
    suspend fun saveMappings(mappings: List<KeyActionConfig>) {
        val array = JSONArray()
        mappings.forEach { array.put(it.toJson()) }
        context.dataStore.edit { it[MAPPINGS_JSON] = array.toString() }
    }
}
