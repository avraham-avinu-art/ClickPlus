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
    val useOrientationSpecificPosition: Boolean = false,
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
        private const val ACTIVE_PROFILE = "active_profile"

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

        fun activeProfileId(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(ACTIVE_PROFILE, "default")
                .orEmpty()
                .ifBlank { "default" }

        fun setActiveProfileId(context: Context, profileId: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(ACTIVE_PROFILE, profileId.ifBlank { "default" }).apply()
        }

        fun isRuleInActiveProfile(context: Context, ruleId: String): Boolean {
            val active = activeProfileId(context)
            return AdvancedRuleRepository(context).getRuleMetadata(ruleId).profileId == active
        }

        fun cycleProfile(context: Context, direction: Int): String {
            val repo = AdvancedRuleRepository(context)
            val profiles = repo.profiles().filter { it.enabled }
            if (profiles.isEmpty()) {
                setActiveProfileId(context, "default")
                return "default"
            }
            val current = activeProfileId(context)
            val index = profiles.indexOfFirst { it.id == current }.let { if (it < 0) 0 else it }
            val next = profiles[(index + direction + profiles.size) % profiles.size]
            setActiveProfileId(context, next.id)
            return next.name
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

        fun updateLatestPendingActionLog(
            context: Context,
            ruleId: String,
            appPackage: String,
            success: Boolean,
            detail: String,
        ) {
            val current = logs(context).toMutableList()
            val index = current.indexOfFirst {
                it.ruleId == ruleId &&
                    it.type == "ACTION" &&
                    it.success == null &&
                    it.message == "הפעולה בביצוע"
            }
            if (index < 0) return
            current[index] = current[index].copy(
                timestamp = System.currentTimeMillis(),
                message = if (success) "הפעולה הצליחה" else "הפעולה נכשלה",
                success = success,
                appPackage = appPackage,
                detail = detail,
            )
            val array = JSONArray()
            current.take(120).forEach { array.put(it.toJson()) }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LOGS, array.toString()).apply()
        }

        fun clearLogs(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LOGS, "[]").apply()
        }
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getRuleMetadata(ruleId: String): RuleAdvancedMetadata {
        val obj = loadMetadata().optJSONObject(ruleId) ?: return RuleAdvancedMetadata()
        return fromJson(obj)
    }

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
        .put("useOrientationSpecificPosition", m.useOrientationSpecificPosition)
        .put("toleranceXRatio", m.toleranceXRatio)
        .put("toleranceYRatio", m.toleranceYRatio)

    private fun fromJson(o: JSONObject): RuleAdvancedMetadata {
        fun ratio(name: String, default: Float = -1f): Float =
            o.optDouble(name, default.toDouble()).toFloat()
                .takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: default
        fun tolerance(name: String): Float =
            o.optDouble(name, 0.08).toFloat()
                .takeIf { it.isFinite() }?.coerceIn(0.01f, 0.25f) ?: 0.08f

        return RuleAdvancedMetadata(
            profileId = o.optString("profileId", "default").ifBlank { "default" },
            priority = o.optInt("priority", 0).coerceIn(0, 10),
            cooldownMs = o.optLong("cooldownMs", 0L).coerceIn(0L, 60_000L),
            delayMs = o.optLong("delayMs", 0L).coerceIn(0L, 10_000L),
            retries = o.optInt("retries", 1).coerceIn(1, 3),
            portraitX = ratio("portraitX"),
            portraitY = ratio("portraitY"),
            landscapeX = ratio("landscapeX"),
            landscapeY = ratio("landscapeY"),
            useOrientationSpecificPosition = o.optBoolean("useOrientationSpecificPosition", false),
            toleranceXRatio = tolerance("toleranceXRatio"),
            toleranceYRatio = tolerance("toleranceYRatio"),
        )
    }
}

data class ActivityLog(
    val timestamp: Long,
    val type: String,
    val message: String,
    val ruleId: String = "",
    val appPackage: String = "",
    val xRatio: Float = -1f,
    val yRatio: Float = -1f,
    val success: Boolean? = null,
    val detail: String = "",
    val actionLabel: String = "",
    val id: String = UUID.randomUUID().toString(),
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("timestamp", timestamp)
        .put("type", type)
        .put("message", message)
        .put("ruleId", ruleId)
        .put("appPackage", appPackage)
        .put("xRatio", xRatio)
        .put("yRatio", yRatio)
        .put("success", success)
        .put("detail", detail)
        .put("actionLabel", actionLabel)

    companion object {
        fun fromJson(o: JSONObject): ActivityLog {
            val type = o.optString("type")
            val storedSuccess = if (o.has("success") && !o.isNull("success")) {
                o.optBoolean("success")
            } else {
                null
            }
            return ActivityLog(
                timestamp = o.optLong("timestamp"),
                type = type,
                message = o.optString("message"),
                ruleId = o.optString("ruleId"),
                appPackage = o.optString("appPackage"),
                xRatio = o.optDouble("xRatio", -1.0).toFloat(),
                yRatio = o.optDouble("yRatio", -1.0).toFloat(),
                success = if (type == "TRIGGER") null else storedSuccess,
                detail = o.optString("detail"),
                actionLabel = o.optString("actionLabel"),
                id = o.optString("id", UUID.randomUUID().toString()),
            )
        }
    }
}
