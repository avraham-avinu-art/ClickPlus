package com.example.clickplus.data

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "clickplus_settings")

enum class OperationMode(val titleHebrew: String) {
    MODE_A_MULTI_TAP("ריבוי לחיצות"),
    MODE_B_CONFIRMATION("לחיצה עם אישור")
}

enum class ActionType(val titleHebrew: String) {
    LAUNCH_APP("פתיחת אפליקציה"),
    SYSTEM_KEY("פעולת מערכת"),
    CLICK_NODE_BY_ID("לחיצה לפי מזהה רכיב"),
    CLICK_NODE_BY_TEXT("לחיצה לפי טקסט במסך"),
    SEND_INTENT("שליחת פקודת מערכת")
}

enum class HudStyle(val titleHebrew: String) {
    NUMBER_ONLY("מספר לחיצה בלבד"),
    SHORT_TEXT("טקסט קצר"),
    ICON_WITH_TEXT("אייקון עם תיאור")
}

enum class ThemeOption(val titleHebrew: String) {
    AUTO("אוטומטי"),
    LIGHT("בהיר"),
    DARK("כהה"),
    DARK_OLED("שחור OLED")
}

enum class SystemActionPreset(val id: String, val titleHebrew: String, val legacyKeyCode: Int = 0) {
    HOME("home", "בית", KeyEvent.KEYCODE_HOME),
    BACK("back", "חזרה", KeyEvent.KEYCODE_BACK),
    RECENTS("recents", "יישומים אחרונים", KeyEvent.KEYCODE_APP_SWITCH),
    NOTIFICATIONS("notifications", "פתיחת התראות", KeyEvent.KEYCODE_NOTIFICATION),
    MEDIA_PLAY_PAUSE("media_play_pause", "נגן / השהה מדיה", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
    MEDIA_NEXT("media_next", "רצועה הבאה"),
    MEDIA_PREVIOUS("media_previous", "רצועה קודמת"),
    VOLUME_UP("volume_up", "הגברת ווליום", KeyEvent.KEYCODE_VOLUME_UP),
    VOLUME_DOWN("volume_down", "הנמכת ווליום", KeyEvent.KEYCODE_VOLUME_DOWN),
    WIFI_SETTINGS("wifi_settings", "פתיחת הגדרות Wi‑Fi"),
    SETTINGS("settings", "פתיחת הגדרות"),
    DIALER("dialer", "פתיחת חייגן"),
    FLASHLIGHT("flashlight", "הדלקה / כיבוי פנס")
}

fun systemActionTitle(id: String, legacyKeyCode: Int = 0): String =
    SystemActionPreset.entries.firstOrNull { it.id == id }?.titleHebrew
        ?: SystemActionPreset.entries.firstOrNull { it.legacyKeyCode == legacyKeyCode }?.titleHebrew
        ?: "פעולת מערכת"

enum class SystemActionPreset(val titleHebrew: String, val keyCode: Int) {
    HOME("בית", KeyEvent.KEYCODE_HOME),
    BACK("חזרה", KeyEvent.KEYCODE_BACK),
    RECENTS("יישומים אחרונים", KeyEvent.KEYCODE_APP_SWITCH),
    NOTIFICATIONS("פתיחת התראות", KeyEvent.KEYCODE_NOTIFICATION),
    MEDIA_PLAY_PAUSE("נגן / השהה מדיה", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
    VOLUME_UP("הגברת ווליום", KeyEvent.KEYCODE_VOLUME_UP),
    VOLUME_DOWN("הנמכת ווליום", KeyEvent.KEYCODE_VOLUME_DOWN)
}

fun keyCodeTitleHebrew(keyCode: Int): String =
    SystemActionPreset.entries.firstOrNull { it.keyCode == keyCode }?.titleHebrew ?: "מקש $keyCode"

fun actionSummaryHebrew(config: KeyActionConfig): String = when (config.actionType) {
    ActionType.LAUNCH_APP ->
        "פתיחת ${config.targetAppName.ifBlank { config.targetPackage.ifBlank { "אפליקציה" } }}"
    ActionType.SYSTEM_KEY ->
        keyCodeTitleHebrew(config.systemKeyCode)
    ActionType.CLICK_NODE_BY_ID ->
        "לחיצה לפי ID: ${config.nodeIdentifier}"
    ActionType.CLICK_NODE_BY_TEXT ->
        "לחיצה על: ${config.nodeIdentifier}"
    ActionType.SEND_INTENT ->
        "Intent: ${config.targetClassOrIntent}"
}

data class KeyActionConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customLabel: String = "",
    val iconName: String = "ic_default",
    val triggerKeyCode: Int,
    val keyNameHebrew: String = "מקש $triggerKeyCode",
    val tapCount: Int = 1,
    val actionType: ActionType = ActionType.SYSTEM_KEY,
    val targetPackage: String = "",
    val targetAppName: String = "",
    val targetClassOrIntent: String = "",
    val nodeIdentifier: String = "",
    val systemKeyCode: Int = KeyEvent.KEYCODE_HOME,
    val profileName: String = "DEFAULT",
    val isEnabled: Boolean = true
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("customLabel", customLabel)
        put("iconName", iconName)
        put("triggerKeyCode", triggerKeyCode)
        put("keyNameHebrew", keyNameHebrew)
        put("tapCount", tapCount)
        put("actionType", actionType.name)
        put("targetPackage", targetPackage)
        put("targetAppName", targetAppName)
        put("targetClassOrIntent", targetClassOrIntent)
        put("nodeIdentifier", nodeIdentifier)
        put("systemKeyCode", systemKeyCode)
        put("profileName", profileName)
        put("isEnabled", isEnabled)
    }

    companion object {
        fun fromJson(json: JSONObject) = KeyActionConfig(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            customLabel = json.optString("customLabel", ""),
            iconName = json.optString("iconName", "ic_default"),
            triggerKeyCode = json.optInt("triggerKeyCode", 0),
            keyNameHebrew = json.optString("keyNameHebrew", "").ifBlank {
                "מקש ${json.optInt("triggerKeyCode", 0)}"
            },
            tapCount = json.optInt("tapCount", 1).coerceAtLeast(1),
            actionType = runCatching {
                ActionType.valueOf(json.optString("actionType", ActionType.SYSTEM_KEY.name))
            }.getOrDefault(ActionType.SYSTEM_KEY),
            targetPackage = json.optString("targetPackage", ""),
            targetAppName = json.optString("targetAppName", ""),
            targetClassOrIntent = json.optString("targetClassOrIntent", ""),
            nodeIdentifier = json.optString("nodeIdentifier", ""),
            systemKeyCode = json.optInt("systemKeyCode", KeyEvent.KEYCODE_HOME),
            profileName = json.optString("profileName", "DEFAULT"),
            isEnabled = json.optBoolean("isEnabled", true)
        )
    }
}

class AppPreferencesRepository(private val context: Context) {
    companion object {
        val OPERATION_MODE = stringPreferencesKey("operation_mode")
        val TAP_TIMEOUT_MS = longPreferencesKey("tap_timeout_ms")
        val DEBOUNCE_MS = longPreferencesKey("debounce_ms")
        val CAR_FRIENDLY_UI = booleanPreferencesKey("car_friendly_ui")
        val THEME_OPTION = stringPreferencesKey("theme_option")
        val HUD_STYLE = stringPreferencesKey("hud_style")
        val HUD_POS_X = intPreferencesKey("hud_pos_x")
        val HUD_POS_Y = intPreferencesKey("hud_pos_y")
        val MAPPINGS_JSON = stringPreferencesKey("mappings_json")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val operationModeFlow: Flow<OperationMode> = context.dataStore.data.map {
        runCatching {
            OperationMode.valueOf(it[OPERATION_MODE] ?: OperationMode.MODE_A_MULTI_TAP.name)
        }.getOrDefault(OperationMode.MODE_A_MULTI_TAP)
    }

    val tapTimeoutFlow: Flow<Long> = context.dataStore.data.map { it[TAP_TIMEOUT_MS] ?: 450L }
    val debounceMsFlow: Flow<Long> = context.dataStore.data.map { it[DEBOUNCE_MS] ?: 80L }
    val carFriendlyUiFlow: Flow<Boolean> = context.dataStore.data.map { it[CAR_FRIENDLY_UI] ?: true }

    val themeOptionFlow: Flow<ThemeOption> = context.dataStore.data.map {
        runCatching {
            ThemeOption.valueOf(it[THEME_OPTION] ?: ThemeOption.DARK_OLED.name)
        }.getOrDefault(ThemeOption.DARK_OLED)
    }

    val hudStyleFlow: Flow<HudStyle> = context.dataStore.data.map {
        runCatching {
            HudStyle.valueOf(it[HUD_STYLE] ?: HudStyle.SHORT_TEXT.name)
        }.getOrDefault(HudStyle.SHORT_TEXT)
    }

    val onboardingCompletedFlow: Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    val mappingsFlow: Flow<List<KeyActionConfig>> = context.dataStore.data.map { prefs ->
        val array = runCatching { JSONArray(prefs[MAPPINGS_JSON] ?: "[]") }.getOrDefault(JSONArray())
        buildList {
            for (i in 0 until array.length()) {
                runCatching { add(KeyActionConfig.fromJson(array.getJSONObject(i))) }
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveOperationMode(mode: OperationMode) {
        context.dataStore.edit { it[OPERATION_MODE] = mode.name }
    }

    suspend fun saveTapTimeout(ms: Long) {
        context.dataStore.edit { it[TAP_TIMEOUT_MS] = ms.coerceIn(100L, 2000L) }
    }

    suspend fun saveDebounce(ms: Long) {
        context.dataStore.edit { it[DEBOUNCE_MS] = ms.coerceIn(20L, 500L) }
    }

    suspend fun saveCarFriendlyUi(enabled: Boolean) {
        context.dataStore.edit { it[CAR_FRIENDLY_UI] = enabled }
    }

    suspend fun saveThemeOption(option: ThemeOption) {
        context.dataStore.edit { it[THEME_OPTION] = option.name }
    }

    suspend fun saveHudStyle(style: HudStyle) {
        context.dataStore.edit { it[HUD_STYLE] = style.name }
    }

    suspend fun saveHudPosition(x: Int, y: Int) {
        context.dataStore.edit {
            it[HUD_POS_X] = x
            it[HUD_POS_Y] = y
        }
    }

    suspend fun saveMappings(mappings: List<KeyActionConfig>) {
        val array = JSONArray()
        mappings.forEach { array.put(it.toJson()) }
        context.dataStore.edit { it[MAPPINGS_JSON] = array.toString() }
    }
}