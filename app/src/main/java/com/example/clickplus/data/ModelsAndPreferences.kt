package com.example.clickplus.data

import android.content.Context
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

enum class OperationMode { MODE_A_MULTI_TAP, MODE_B_CONFIRMATION }
enum class ActionType { LAUNCH_APP, SYSTEM_KEY, CLICK_NODE_BY_ID, CLICK_NODE_BY_TEXT, SEND_INTENT }
enum class HudStyle { NUMBER_ONLY, SHORT_TEXT, ICON_WITH_TEXT }
enum class ThemeOption { AUTO, DARK_OLED, LIGHT }

data class KeyActionConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customLabel: String = "",
    val iconName: String = "ic_default",
    val triggerKeyCode: Int,
    val tapCount: Int,
    val actionType: ActionType,
    val targetPackage: String = "",
    val targetClassOrIntent: String = "",
    val nodeIdentifier: String = "",
    val systemKeyCode: Int = 0,
    val profileName: String = "DEFAULT",
    val isEnabled: Boolean = true
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("customLabel", customLabel); put("iconName", iconName)
        put("triggerKeyCode", triggerKeyCode); put("tapCount", tapCount); put("actionType", actionType.name)
        put("targetPackage", targetPackage); put("targetClassOrIntent", targetClassOrIntent)
        put("nodeIdentifier", nodeIdentifier); put("systemKeyCode", systemKeyCode)
        put("profileName", profileName); put("isEnabled", isEnabled)
    }
    companion object {
        fun fromJson(json: JSONObject) = KeyActionConfig(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            customLabel = json.optString("customLabel", ""), iconName = json.optString("iconName", "ic_default"),
            triggerKeyCode = json.optInt("triggerKeyCode", 0), tapCount = json.optInt("tapCount", 1),
            actionType = runCatching { ActionType.valueOf(json.optString("actionType", ActionType.SYSTEM_KEY.name)) }.getOrDefault(ActionType.SYSTEM_KEY),
            targetPackage = json.optString("targetPackage", ""), targetClassOrIntent = json.optString("targetClassOrIntent", ""),
            nodeIdentifier = json.optString("nodeIdentifier", ""), systemKeyCode = json.optInt("systemKeyCode", 0),
            profileName = json.optString("profileName", "DEFAULT"), isEnabled = json.optBoolean("isEnabled", true)
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
    val operationModeFlow = context.dataStore.data.map { runCatching { OperationMode.valueOf(it[OPERATION_MODE] ?: OperationMode.MODE_A_MULTI_TAP.name) }.getOrDefault(OperationMode.MODE_A_MULTI_TAP) }
    val tapTimeoutFlow = context.dataStore.data.map { it[TAP_TIMEOUT_MS] ?: 450L }
    val debounceMsFlow = context.dataStore.data.map { it[DEBOUNCE_MS] ?: 80L }
    val carFriendlyUiFlow = context.dataStore.data.map { it[CAR_FRIENDLY_UI] ?: true }
    val themeOptionFlow = context.dataStore.data.map { runCatching { ThemeOption.valueOf(it[THEME_OPTION] ?: ThemeOption.DARK_OLED.name) }.getOrDefault(ThemeOption.DARK_OLED) }
    val hudStyleFlow = context.dataStore.data.map { runCatching { HudStyle.valueOf(it[HUD_STYLE] ?: HudStyle.SHORT_TEXT.name) }.getOrDefault(HudStyle.SHORT_TEXT) }
    val onboardingCompletedFlow = context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val mappingsFlow = context.dataStore.data.map { prefs ->
        val array = runCatching { JSONArray(prefs[MAPPINGS_JSON] ?: "[]") }.getOrDefault(JSONArray())
        buildList { for (i in 0 until array.length()) add(KeyActionConfig.fromJson(array.getJSONObject(i))) }
    }
    suspend fun setOnboardingCompleted(completed: Boolean) { context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed } }
    suspend fun saveOperationMode(mode: OperationMode) { context.dataStore.edit { it[OPERATION_MODE] = mode.name } }
    suspend fun saveTapTimeout(ms: Long) { context.dataStore.edit { it[TAP_TIMEOUT_MS] = ms.coerceIn(100L, 2000L) } }
    suspend fun saveDebounce(ms: Long) { context.dataStore.edit { it[DEBOUNCE_MS] = ms.coerceIn(20L, 500L) } }
    suspend fun saveCarFriendlyUi(enabled: Boolean) { context.dataStore.edit { it[CAR_FRIENDLY_UI] = enabled } }
    suspend fun saveThemeOption(option: ThemeOption) { context.dataStore.edit { it[THEME_OPTION] = option.name } }
    suspend fun saveHudStyle(style: HudStyle) { context.dataStore.edit { it[HUD_STYLE] = style.name } }
    suspend fun saveHudPosition(x: Int, y: Int) { context.dataStore.edit { it[HUD_POS_X] = x; it[HUD_POS_Y] = y } }
    suspend fun saveMappings(mappings: List<KeyActionConfig>) { val array = JSONArray(); mappings.forEach { array.put(it.toJson()) }; context.dataStore.edit { it[MAPPINGS_JSON] = array.toString() } }
}
