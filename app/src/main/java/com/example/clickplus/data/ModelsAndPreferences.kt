package com.example.clickplus.data

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "clickplus_settings")

enum class ActionType(val titleHebrew: String) {
    SYSTEM("פעולת מערכת"),
    APP("פתיחת אפליקציה"),
}

enum class ContextConditionType(val titleHebrew: String) {
    ANY("בכל מצב"),
    APP("אפליקציה"),
    MUSIC("מוזיקה פועלת"),
    MUTED("שמע מושתק"),
    RINGING("הטלפון מצלצל"),
    RADIO("רדיו פועל"),
}

enum class SystemActionPreset(val id: String, val titleHebrew: String) {
    HOME("home", "בית"),
    BACK("back", "חזרה"),
    RECENTS("recents", "יישומים אחרונים"),
    NOTIFICATIONS("notifications", "פתיחת התראות"),
    MEDIA_PLAY_PAUSE("media_play_pause", "נגן / השהה"),
    MEDIA_NEXT("media_next", "השיר הבא"),
    MEDIA_PREVIOUS("media_previous", "השיר הקודם"),
    VOLUME_UP("volume_up", "הגברת ווליום"),
    VOLUME_DOWN("volume_down", "הנמכת ווליום"),
    SETTINGS("settings", "הגדרות"),
    DIALER("dialer", "פתיחת חייגן"),
}

data class KeyActionConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val pressCount: Int = 1,
    val actionType: ActionType = ActionType.SYSTEM,
    val systemActionId: String = SystemActionPreset.HOME.id,
    val targetPackage: String = "",
    val targetAppName: String = "",
    val contextConditionType: ContextConditionType = ContextConditionType.ANY,
    val contextConditionValue: String = "",
    val contextConditionName: String = "",
    val enabled: Boolean = true,
) {
    fun pressSummary(): String = if (pressCount == 1) "כניסה אחת" else "$pressCount כניסות"

    fun actionSummary(): String {
        return when (actionType) {
            ActionType.SYSTEM ->
                SystemActionPreset.entries.firstOrNull { it.id == systemActionId }?.titleHebrew ?: "פעולת מערכת"
            ActionType.APP ->
                "פתיחת " + targetAppName.ifBlank { "אפליקציה" }
        }
    }

    fun contextSummary(): String {
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
        put("id", id)
        put("name", name)
        put("pressCount", pressCount)
        put("actionType", actionType.name)
        put("systemActionId", systemActionId)
        put("targetPackage", targetPackage)
        put("targetAppName", targetAppName)
        put("contextConditionType", contextConditionType.name)
        put("contextConditionValue", contextConditionValue)
        put("contextConditionName", contextConditionName)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(json: JSONObject) = KeyActionConfig(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            name = json.optString("name", json.optString("customLabel", "")),
            pressCount = json.optInt("pressCount", json.optInt("tapCount", 1)).coerceIn(1, 10),
            actionType = runCatching {
                ActionType.valueOf(
                    json.optString(
                        "actionType",
                        if (json.has("targetPackage")) ActionType.APP.name else ActionType.SYSTEM.name,
                    ),
                )
            }.getOrDefault(ActionType.SYSTEM),
            systemActionId = json.optString("systemActionId", SystemActionPreset.HOME.id),
            targetPackage = json.optString("targetPackage", ""),
            targetAppName = json.optString("targetAppName", ""),
            contextConditionType = runCatching {
                ContextConditionType.valueOf(
                    json.optString("contextConditionType", ContextConditionType.ANY.name),
                )
            }.getOrDefault(ContextConditionType.ANY),
            contextConditionValue = json.optString("contextConditionValue", ""),
            contextConditionName = json.optString("contextConditionName", ""),
            enabled = json.optBoolean("enabled", json.optBoolean("isEnabled", true)),
        )
    }
}

class AppPreferencesRepository(private val context: Context) {
    companion object {
        val BACKGROUND_ONLY = booleanPreferencesKey("background_only")
        val TAP_TIMEOUT_MS = longPreferencesKey("tap_timeout_ms")
        val SHOW_TAP_COUNT = booleanPreferencesKey("show_tap_count")
        val MAPPINGS_JSON = stringPreferencesKey("mappings_json")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val backgroundOnlyFlow: Flow<Boolean> =
        context.dataStore.data.map { it[BACKGROUND_ONLY] ?: false }

    val tapTimeoutFlow: Flow<Long> =
        context.dataStore.data.map { it[TAP_TIMEOUT_MS] ?: 650L }

    val showTapCountFlow: Flow<Boolean> =
        context.dataStore.data.map { it[SHOW_TAP_COUNT] ?: false }

    val mappingsFlow: Flow<List<KeyActionConfig>> =
        context.dataStore.data.map { prefs ->
            val array = runCatching {
                JSONArray(prefs[MAPPINGS_JSON] ?: "[]")
            }.getOrDefault(JSONArray())

            buildList {
                for (i in 0 until array.length()) {
                    runCatching { add(KeyActionConfig.fromJson(array.getJSONObject(i))) }
                }
            }
        }

    val onboardingCompletedFlow: Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    suspend fun saveBackgroundOnly(enabled: Boolean) {
        context.dataStore.edit { it[BACKGROUND_ONLY] = enabled }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("background_only", enabled)
            .apply()
    }

    suspend fun saveTapTimeout(ms: Long) {
        context.dataStore.edit { it[TAP_TIMEOUT_MS] = ms.coerceIn(300L, 1200L) }
    }

    suspend fun saveShowTapCount(enabled: Boolean) {
        context.dataStore.edit { it[SHOW_TAP_COUNT] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveMappings(mappings: List<KeyActionConfig>) {
        val array = JSONArray()
        mappings.forEach { array.put(it.toJson()) }
        context.dataStore.edit { it[MAPPINGS_JSON] = array.toString() }
    }
}
