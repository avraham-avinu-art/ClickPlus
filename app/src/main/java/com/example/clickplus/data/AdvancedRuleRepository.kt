package com.example.clickplus.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AppMode(val titleHebrew: String) {
    FULL("מלא"),
    BASIC("בסיסי ללא נגישות")
}

data class RuleAdvancedMetadata(
    val profileId: String = "default",
    val priority: Int = 0,
    val cooldownMs: Long = 0L,
    val delayMs: Long = 0L,
    val retries: Int = 1,
    val portraitX: Float = -1f,
    val portraitY: Float = -1f,
    val landscapeX: Float = -1f,
    val landscapeY: Float = -1f,
    val toleranceXRatio: Float = 0.08f,
    val toleranceYRatio: Float = 0.08f,
)

data class ClickPlusProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
)

class AdvancedRuleRepository(private val context: Context) {
    companion object {
        private const val PREFS = "clickplus_advanced"
        private const val META = "rule_metadata"
        private const val PROFILES = "profiles"
        private const val MODE = "app_mode"
        private const val THEME = "theme_mode"
        private const val LAST_PACKAGE = "last_external_package"
        private const val LOGS = "activity_logs"

        fun currentMode(context: Context): AppMode =
            runCatching {
                AppMode.valueOf(
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getString(MODE, AppMode.FULL.name) ?: AppMode.FULL.name
                )
            }.getOrDefault(AppMode.FULL)

        fun setMode(context: Context, mode: AppMode) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(MODE, mode.name).apply()
        }

        fun lastExternalPackage(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LAST_PACKAGE, "").orEmpty()

        fun setLastExternalPackage(context: Context, packageName: String) {
            if (packageName.isBlank()) return
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LAST_PACKAGE, packageName).apply()
        }

        fun logs(context: Context): List<ActivityLog> =
            runCatching {
                val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LOGS, "[]") ?: "[]"
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        array.optJSONObject(i)?.let { add(ActivityLog.fromJson(it)) }
                    }
                }.sortedByDescending { it.timestamp }
            }.getOrDefault(emptyList())

        fun addLog(context: Context, log: ActivityLog) {
            val current = logs(context).take(119).toMutableList()
            current.add(0, log)
            val array = JSONArray()
            current.take(120).forEach { array.put(it.toJson()) }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LOGS, array.toString()).apply()
        }

        fun clearLogs(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LOGS, "[]").apply()
        }
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getRuleMetadata(ruleId: String): RuleAdvancedMetadata = loadMetadata()[ruleId]?.let(::fromJson) ?: RuleAdvancedMetadata()

    fun saveRuleMetadata(ruleId: String, metadata: RuleAdvancedMetadata) {
        val root = loadMetadata()
        root.put(ruleId, toJson(metadata))
        prefs.edit().putString(META, root.toString()).apply()
    }

    fun removeRuleMetadata(ruleId: String) {
        val root = loadMetadata()
        root.remove(ruleId)
        prefs.edit().putString(META, root.toString()).apply()
    }

    fun profiles(): List<ClickPlusProfile> {
        val raw = prefs.getString(PROFILES, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    add(ClickPlusProfile(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", "פרופיל"),
                        enabled = o.optBoolean("enabled", true)
                    ))
                }
            }
        }.getOrDefault(emptyList()).ifEmpty {
            listOf(ClickPlusProfile("default", "כללי", true)).also { saveProfiles(it) }
        }
    }

    fun saveProfiles(items: List<ClickPlusProfile>) {
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name).put("enabled", it.enabled))
        }
        prefs.edit().putString(PROFILES, array.toString()).apply()
    }

    fun themeMode(): String = prefs.getString(THEME, "system").orEmpty()
    fun saveThemeMode(mode: String) { prefs.edit().putString(THEME, mode).apply() }
    fun clearAllRuleMetadata() { prefs.edit().remove(META).apply() }

    fun exportJson(baseMappingsJson: String, baseSettings: JSONObject): String {
        val bundle = JSONObject()
            .put("version", 2)
            .put("mappings", JSONArray(baseMappingsJson))
            .put("profiles", JSONArray().also { a -> profiles().forEach { p ->
                a.put(JSONObject().put("id", p.id).put("name", p.name).put("enabled", p.enabled))
            }})
            .put("ruleMetadata", loadMetadata())
            .put("settings", baseSettings)
            .put("exportedAt", System.currentTimeMillis())
        return bundle.toString(2)
    }

    fun importBundle(root: JSONObject) {
        val profileArray = root.optJSONArray("profiles")
        if (profileArray != null) {
            val list = buildList {
                for (i in 0 until profileArray.length()) {
                    val o = profileArray.optJSONObject(i) ?: continue
                    add(ClickPlusProfile(
                        o.optString("id", UUID.randomUUID().toString()),
                        o.optString("name", "פרופיל"),
                        o.optBoolean("enabled", true)
                    ))
                }
            }
            if (list.isNotEmpty()) saveProfiles(list)
        }
        val metadata = root.optJSONObject("ruleMetadata")
        if (metadata != null) prefs.edit().putString(META, metadata.toString()).apply()
    }

    private fun loadMetadata(): JSONObject =
        runCatching { JSONObject(prefs.getString(META, "{}") ?: "{}") }.getOrDefault(JSONObject())

    private fun toJson(m: RuleAdvancedMetadata) = JSONObject()
        .put("profileId", m.profileId)
        .put("priority", m.priority)
        .put("cooldownMs", m.cooldownMs)
        .put("delayMs", m.delayMs)
        .put("retries", m.retries)
        .put("portraitX", m.portraitX)
        .put("portraitY", m.portraitY)
        .put("landscapeX", m.landscapeX)
        .put("landscapeY", m.landscapeY)
        .put("toleranceXRatio", m.toleranceXRatio)
        .put("toleranceYRatio", m.toleranceYRatio)

    private fun fromJson(o: JSONObject) = RuleAdvancedMetadata(
        profileId = o.optString("profileId", "default"),
        priority = o.optInt("priority", 0),
        cooldownMs = o.optLong("cooldownMs", 0L).coerceIn(0L, 60_000L),
        delayMs = o.optLong("delayMs", 0L).coerceIn(0L, 10_000L),
        retries = o.optInt("retries", 1).coerceIn(1, 3),
        portraitX = o.optDouble("portraitX", -1.0).toFloat(),
        portraitY = o.optDouble("portraitY", -1.0).toFloat(),
        landscapeX = o.optDouble("landscapeX", -1.0).toFloat(),
        landscapeY = o.optDouble("landscapeY", -1.0).toFloat(),
        toleranceXRatio = o.optDouble("toleranceXRatio", 0.08).toFloat().coerceIn(0.01f, 0.25f),
        toleranceYRatio = o.optDouble("toleranceYRatio", 0.08).toFloat().coerceIn(0.01f, 0.25f),
    )
}

data class ActivityLog(
    val timestamp: Long,
    val type: String,
    val message: String,
    val ruleId: String = "",
    val appPackage: String = "",
    val xRatio: Float = -1f,
    val yRatio: Float = -1f,
    val success: Boolean = true,
) {
    fun toJson() = JSONObject()
        .put("timestamp", timestamp)
        .put("type", type)
        .put("message", message)
        .put("ruleId", ruleId)
        .put("appPackage", appPackage)
        .put("xRatio", xRatio)
        .put("yRatio", yRatio)
        .put("success", success)

    companion object {
        fun fromJson(o: JSONObject) = ActivityLog(
            timestamp = o.optLong("timestamp"),
            type = o.optString("type"),
            message = o.optString("message"),
            ruleId = o.optString("ruleId"),
            appPackage = o.optString("appPackage"),
            xRatio = o.optDouble("xRatio", -1.0).toFloat(),
            yRatio = o.optDouble("yRatio", -1.0).toFloat(),
            success = o.optBoolean("success", true),
        )
    }
}
