package com.example.clickplus.data

import android.content.Context
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
    SYSTEM("פעולת מכשיר"),
    APP("פתיחת אפליקציה"),
    APP_TAP("לחיצה אוטומטית באפליקציה"),
    MULTI_POINT_TAP("שתי לחיצות אוטומטיות"),
    PROFILE("ניהול פרופילי ClickPlus"),
}

enum class TriggerType(val titleHebrew: String) {
    CLICKPLUS_ENTRY("לחיצות כניסה לקליק פלוס"),
    APP_ENTRY("לחיצות כניסה לאפליקציה אחרת"),
}

enum class ClickPlusProfileAction(
    val id: String,
    val titleHebrew: String,
) {
    NEXT("profile_next", "מעבר לפרופיל הבא"),
    PREVIOUS("profile_previous", "מעבר לפרופיל הקודם"),
    DEFAULT("profile_default", "חזרה לפרופיל ברירת המחדל"),
}

enum class ContextConditionType(val titleHebrew: String) {
    ANY("בכל מצב"),
    APP("כשהאפליקציה פתוחה"),
    MUSIC("כשהמוזיקה פועלת"),
    MUTED("כשהשמיעה מושתק"),
    RINGING("כשהטלפון מצלצל"),
    RADIO("כשהרדיו פועל"),
    BRIGHTNESS_LOW("כשהבהירות נמוכה"),
    VOLUME_LEVEL("כשעוצמת השמע ברמה מסוימת"),
}

enum class SystemActionPreset(
    val id: String,
    val titleHebrew: String,
    val categoryHebrew: String,
) {
    HOME("home", "בית", "ניווט"),
    BACK("back", "חזרה", "ניווט"),
    RECENTS("recents", "יישומים אחרונים", "ניווט"),
    NOTIFICATIONS("notifications", "פתיחת התראות", "ניווט"),
    LOCK_SCREEN("lock_screen", "נעילת המסך", "מכשיר"),
    POWER_MENU("power_menu", "תפריט כיבוי", "מכשיר"),
    SCREENSHOT("screenshot", "צילום מסך", "מכשיר"),

    MEDIA_STOP("media_stop", "עצור השמעה", "נגן"),
    MEDIA_PLAY("media_play", "הפעל השמעה", "נגן"),
    MEDIA_RESUME("media_resume", "המשך השמעה", "נגן"),
    MEDIA_PLAY_PAUSE("media_play_pause", "נגן / השהה", "נגן"),
    MEDIA_NEXT("media_next", "השיר הבא", "נגן"),
    MEDIA_PREVIOUS("media_previous", "השיר הקודם", "נגן"),
    MEDIA_FAST_FORWARD("media_fast_forward", "הרצה קדימה", "נגן"),
    MEDIA_REWIND("media_rewind", "הרצה לאחור", "נגן"),

    VOLUME_UP("volume_up", "הגברת ווליום", "ווליום"),
    VOLUME_DOWN("volume_down", "הנמכת ווליום", "ווליום"),
    VOLUME_MUTE("volume_mute", "השתקת שמע", "ווליום"),
    VOLUME_STATUS("volume_status", "הצגת סטטוס עוצמת השמע", "ווליום"),

    BRIGHTNESS_UP("brightness_up", "הוספת בהירות", "בהירות מסך"),
    BRIGHTNESS_DOWN("brightness_down", "החלשת בהירות", "בהירות מסך"),
    BRIGHTNESS_SET("brightness_set", "שינוי בהירות", "בהירות מסך"),

    SETTINGS("settings", "הגדרות", "הגדרות"),
    WIFI_SETTINGS("wifi_settings", "הגדרות Wi‑Fi", "הגדרות"),
    BLUETOOTH_SETTINGS("bluetooth_settings", "הגדרות Bluetooth", "הגדרות"),
    DISPLAY_SETTINGS("display_settings", "הגדרות תצוגה", "הגדרות"),
    SOUND_SETTINGS("sound_settings", "הגדרות שמע", "הגדרות"),
    BATTERY_SETTINGS("battery_settings", "הגדרות סוללה", "הגדרות"),
    APP_SETTINGS("app_settings", "הגדרות אפליקציות", "הגדרות"),

    DIALER("dialer", "פתיחת החייגן", "חייגן"),
    ANSWER_CALL("answer_call", "מענה לשיחה", "חייגן"),
    DECLINE_CALL("decline_call", "דחיית שיחה", "חייגן"),
    DIAL_NUMBER("dial_number", "חיוג למספר", "חייגן"),
    DIAL_CONTACT("dial_contact", "חיוג לאיש קשר", "חייגן"),

    PROFILE_NEXT("profile_next", "פרופיל הבא", "פרופילים"),
    PROFILE_PREVIOUS("profile_previous", "פרופיל קודם", "פרופילים"),
    PROFILE_DEFAULT("profile_default", "חזרה לפרופיל ברירת המחדל", "פרופילים"),
}

data class KeyActionConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val pressCount: Int = 1,
    val actionType: ActionType = ActionType.SYSTEM,
    val systemActionId: String = SystemActionPreset.HOME.id,
    val actionParameter: String = "",
    val contactName: String = "",
    val contactNumber: String = "",
    val targetPackage: String = "",
    val targetAppName: String = "",
    val contextConditionType: ContextConditionType = ContextConditionType.ANY,
    val contextConditionValue: String = "",
    val contextConditionName: String = "",
    val enabled: Boolean = true,
    val triggerType: TriggerType = TriggerType.CLICKPLUS_ENTRY,
    val triggerPackage: String = "",
    val triggerAppName: String = "",
    val screenTapPackage: String = "",
    val screenTapAppName: String = "",
    val screenTapXRatio: Float = -1f,
    val screenTapYRatio: Float = -1f,
    val screenTapSecondXRatio: Float = -1f,
    val screenTapSecondYRatio: Float = -1f,
    val screenTapIntervalMs: Long = 1000L,
    val screenTapToleranceRatio: Float = 0.08f
) {
    fun pressSummary(): String =
        if (pressCount == 1) "לחיצה אחת" else pressCount.toString() + " לחיצות"

    fun actionSummary(): String = when (actionType) {
        ActionType.SYSTEM -> {
            val action = SystemActionPreset.entries.firstOrNull { it.id == systemActionId }
            when (action?.id) {
                SystemActionPreset.DIAL_NUMBER.id ->
                    "חיוג ל-" + actionParameter.ifBlank { "מספר" }
                SystemActionPreset.DIAL_CONTACT.id ->
                    "חיוג ל-" + contactName.ifBlank { "איש קשר" }
                else -> action?.titleHebrew ?: "פעולת מערכת"
            }
        }
        ActionType.APP -> "פתיחת " + targetAppName.ifBlank { "אפליקציה" }
        ActionType.APP_TAP -> "לחיצה אוטומטית ב-" + screenTapAppName.ifBlank { "אפליקציה" }
        ActionType.MULTI_POINT_TAP -> "שתי לחיצות אוטומטיות ב-" + screenTapAppName.ifBlank { "אפליקציה" }
        ActionType.PROFILE -> {
            ClickPlusProfileAction.entries.firstOrNull { it.id == systemActionId }?.titleHebrew
                ?: "ניהול פרופיל ClickPlus"
        }
    }

    fun contextSummary(): String {
        val entryText = when (triggerType) {
            TriggerType.CLICKPLUS_ENTRY -> "בלחיצה על ClickPlus"
            TriggerType.APP_ENTRY -> "בכניסה ל-" + triggerAppName.ifBlank { "אפליקציה" }
        }
        val conditionText = when (contextConditionType) {
            ContextConditionType.ANY -> "בכל מצב"
            ContextConditionType.APP -> "כשהאפליקציה הקודמת פתוחה: " + contextConditionName.ifBlank { contextConditionValue }
            ContextConditionType.MUSIC -> "כשהמוזיקה פועלת"
            ContextConditionType.MUTED -> "כשהשמיעה מושתק"
            ContextConditionType.RINGING -> "כשהטלפון מצלצל"
            ContextConditionType.RADIO -> "כשהרדיו פועל" + contextConditionName.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
            ContextConditionType.BRIGHTNESS_LOW -> "כשהבהירות נמוכה"
            ContextConditionType.VOLUME_LEVEL -> "כשעוצמת השמע ברמה " + contextConditionValue.ifBlank { "1" }
        }
        return if (actionType == ActionType.SYSTEM && systemActionId == SystemActionPreset.DIAL_CONTACT.id) {
            entryText + " · " + conditionText + " · חיוג לאיש הקשר " + contactName.ifBlank { "שנבחר" }
        } else {
            entryText + " · " + conditionText
        }
    }

    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("pressCount", pressCount)
        put("actionType", actionType.name); put("systemActionId", systemActionId)
        put("actionParameter", actionParameter)
        put("contactName", contactName); put("contactNumber", contactNumber)
        put("targetPackage", targetPackage); put("targetAppName", targetAppName)
        put("contextConditionType", contextConditionType.name)
        put("contextConditionValue", contextConditionValue); put("contextConditionName", contextConditionName)
        put("enabled", enabled); put("triggerType", triggerType.name)
        put("triggerPackage", triggerPackage); put("triggerAppName", triggerAppName)
        put("screenTapPackage", screenTapPackage); put("screenTapAppName", screenTapAppName)
        put("screenTapXRatio", screenTapXRatio); put("screenTapYRatio", screenTapYRatio)
        put("screenTapSecondXRatio", screenTapSecondXRatio); put("screenTapSecondYRatio", screenTapSecondYRatio)
        put("screenTapIntervalMs", screenTapIntervalMs)
        put("screenTapToleranceRatio", screenTapToleranceRatio)
    }

    companion object {
        fun fromJson(json: JSONObject) = KeyActionConfig(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            name = json.optString("name", json.optString("customLabel", "")),
            pressCount = json.optInt("pressCount", json.optInt("tapCount", 1)).coerceIn(1, 10),
            actionType = run {
                val storedType = runCatching {
                    ActionType.valueOf(json.optString("actionType", ActionType.SYSTEM.name))
                }.getOrDefault(ActionType.SYSTEM)
                val storedSystemAction = json.optString("systemActionId", SystemActionPreset.HOME.id)
                if (storedType == ActionType.SYSTEM && ClickPlusProfileAction.entries.any { it.id == storedSystemAction }) {
                    ActionType.PROFILE
                } else {
                    storedType
                }
            },
            systemActionId = json.optString("systemActionId", SystemActionPreset.HOME.id),
            actionParameter = json.optString("actionParameter", ""),
            contactName = json.optString("contactName", ""),
            contactNumber = json.optString("contactNumber", ""),
            targetPackage = json.optString("targetPackage", ""),
            targetAppName = json.optString("targetAppName", ""),
            contextConditionType = runCatching { ContextConditionType.valueOf(json.optString("contextConditionType", ContextConditionType.ANY.name)) }.getOrDefault(ContextConditionType.ANY),
            contextConditionValue = json.optString("contextConditionValue", ""),
            contextConditionName = json.optString("contextConditionName", ""),
            enabled = json.optBoolean("enabled", json.optBoolean("isEnabled", true)),
            triggerType = runCatching {
                TriggerType.valueOf(json.optString("triggerType", TriggerType.CLICKPLUS_ENTRY.name))
            }.getOrDefault(TriggerType.CLICKPLUS_ENTRY),
            triggerPackage = json.optString("triggerPackage", ""),
            triggerAppName = json.optString("triggerAppName", ""),
            screenTapPackage = json.optString("screenTapPackage", ""),
            screenTapAppName = json.optString("screenTapAppName", ""),
            screenTapXRatio = json.optDouble("screenTapXRatio", -1.0).toFloat().takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: -1f,
            screenTapYRatio = json.optDouble("screenTapYRatio", -1.0).toFloat().takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: -1f,
            screenTapSecondXRatio = json.optDouble("screenTapSecondXRatio", -1.0).toFloat().takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: -1f,
            screenTapSecondYRatio = json.optDouble("screenTapSecondYRatio", -1.0).toFloat().takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: -1f,
            screenTapIntervalMs = json.optLong("screenTapIntervalMs", 1000L).coerceIn(500L, 10_000L),
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
        val TAP_COUNT_X = longPreferencesKey("tap_count_x")
        val TAP_COUNT_Y = longPreferencesKey("tap_count_y")
        val TAP_COUNT_POSITION = longPreferencesKey("tap_count_position")
        val MAPPINGS_JSON = stringPreferencesKey("mappings_json")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        fun tapTimeoutSnapshot(context: Context): Long =
            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .getLong("tap_timeout_ms", 1200L)
                .coerceIn(300L, 1500L)

        fun actionDelaySnapshot(context: Context): Long =
            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .getLong("action_delay_ms", 0L)
                .coerceIn(0L, 5000L)

        fun tapCountXSnapshot(context: Context): Int =
            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .getLong("tap_count_x", 50L)
                .toInt()
                .coerceIn(0, 100)

        fun tapCountYSnapshot(context: Context): Int =
            context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .getLong("tap_count_y", 65L)
                .toInt()
                .coerceIn(0, 100)

        fun tapCountPositionSnapshot(context: Context): Int =
            (100 - tapCountYSnapshot(context)).coerceIn(0, 100)

        fun mappingsSnapshot(context: Context): List<KeyActionConfig> {
            val raw = context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
                .getString("mappings_json", "[]") ?: "[]"
            return runCatching {
                val array = JSONArray(raw)
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
    val tapCountXFlow: Flow<Int> = context.dataStore.data.map { it[TAP_COUNT_X]?.toInt()?.coerceIn(0, 100) ?: 50 }
    val tapCountYFlow: Flow<Int> = context.dataStore.data.map {
        it[TAP_COUNT_Y]?.toInt()?.coerceIn(0, 100)
            ?: (100 - (it[TAP_COUNT_POSITION]?.toInt()?.coerceIn(5, 90) ?: 35))
    }
    val tapCountPositionFlow: Flow<Int> = tapCountYFlow.map { (100 - it).coerceIn(0, 100) }
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
        context.dataStore.edit { it[BACKGROUND_ONLY] = true }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE).edit().putBoolean("background_only", true).apply()
    }
    suspend fun saveTapTimeout(ms: Long) {
        val safe = ms.coerceIn(300L, 1500L)
        context.dataStore.edit { it[TAP_TIMEOUT_MS] = safe }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit().putLong("tap_timeout_ms", safe).apply()
    }
    suspend fun saveActionDelay(ms: Long) {
        val safe = ms.coerceIn(0L, 5000L)
        context.dataStore.edit { it[ACTION_DELAY_MS] = safe }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit().putLong("action_delay_ms", safe).apply()
    }
    suspend fun saveShowTapCount(enabled: Boolean) {
        context.dataStore.edit { it[SHOW_TAP_COUNT] = enabled }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit().putBoolean("show_tap_count", enabled).apply()
    }
    suspend fun saveTapCountPosition(percentFromBottom: Int) {
        val safe = percentFromBottom.coerceIn(0, 100)
        saveTapCountY(100 - safe)
    }

    suspend fun saveTapCountX(percentFromLeft: Int) {
        val safe = percentFromLeft.coerceIn(0, 100)
        context.dataStore.edit { it[TAP_COUNT_X] = safe.toLong() }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit().putLong("tap_count_x", safe.toLong()).apply()
    }

    suspend fun saveTapCountY(percentFromTop: Int) {
        val safe = percentFromTop.coerceIn(0, 100)
        context.dataStore.edit {
            it[TAP_COUNT_Y] = safe.toLong()
            it[TAP_COUNT_POSITION] = (100 - safe).toLong()
        }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE)
            .edit()
            .putLong("tap_count_y", safe.toLong())
            .putLong("tap_count_position", (100 - safe).toLong())
            .apply()
    }
    suspend fun setOnboardingCompleted(completed: Boolean) { context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed } }
    suspend fun saveMappings(mappings: List<KeyActionConfig>) {
        val array = JSONArray()
        mappings.forEach { array.put(it.toJson()) }
        val raw = array.toString()
        context.dataStore.edit { it[MAPPINGS_JSON] = raw }
        context.getSharedPreferences("clickplus_runtime", Context.MODE_PRIVATE).edit().putString("mappings_json", raw).apply()
    }
}
