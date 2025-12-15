package com.example.surveysdk

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import org.json.JSONArray

class ConfigCacheManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "survey_config_cache"
        private const val KEY_CONFIG_JSON = "cached_config"
        private const val KEY_TIMESTAMP = "cache_timestamp"
        private const val DEFAULT_CACHE_DURATION_HOURS = 24L // Cache for 24 hours
    }

    fun saveConfig(config: Config) {
        try {
            val configJson = configToJson(config)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_CONFIG_JSON, configJson)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .apply()
            Log.d("ConfigCache", "Configuration cached successfully")
        } catch (e: Exception) {
            Log.e("ConfigCache", "Failed to cache configuration: ${e.message}")
        }
    }

    fun getCachedConfig(): Config? {
        clearCache() // ← Add this line to always clear cache
       // return null  // ← Always return null to force API call
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedJson = prefs.getString(KEY_CONFIG_JSON, null) ?: return null
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)

            // Get cache duration from the config itself
            val config = jsonToConfig(cachedJson)
            val cacheDurationHours = config.cacheDurationHours ?: DEFAULT_CACHE_DURATION_HOURS

            // Check if cache is expired using config value
            val cacheAge = System.currentTimeMillis() - timestamp
            val cacheExpired = cacheAge > TimeUnit.HOURS.toMillis(cacheDurationHours)

            if (cacheExpired) {
                Log.d("ConfigCache", "Cache expired, age: ${cacheAge / 1000 / 60} minutes")
                clearCache()
                return null
            }

            Log.d("ConfigCache", "Using cached configuration, age: ${cacheAge / 1000 / 60} minutes")
            return config
        } catch (e: Exception) {
            Log.e("ConfigCache", "Failed to load cached configuration: ${e.message}")
            return null
        }
    }

    fun clearCache() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        Log.d("ConfigCache", "Configuration cache cleared")
    }

    private fun configToJson(config: Config): String {
        return JSONObject().apply {
            put("baseUrl", config.baseUrl)
            put("sdkVersion", config.sdkVersion)
            put("enableButtonTrigger", config.enableButtonTrigger)
            put("enableScrollTrigger", config.enableScrollTrigger)
            put("enableNavigationTrigger", config.enableNavigationTrigger)
            put("enableAppLaunchTrigger", config.enableAppLaunchTrigger)
            put("enableExitTrigger", config.enableExitTrigger)
            put("enableTabChangeTrigger", config.enableTabChangeTrigger)
            put("timeDelay", config.timeDelay)
            put("scrollThreshold", config.scrollThreshold)
            put("navigationScreens", JSONObject().apply {
                config.navigationScreens.forEachIndexed { index, screen ->
                    put("screen_$index", screen)
                }
            })
            put("triggerType", config.triggerType)
            put("modalStyle", config.modalStyle)
            put("animationType", config.animationType)
            put("backgroundColor", config.backgroundColor)
            put("collectDeviceId", config.collectDeviceId)
            put("collectDeviceModel", config.collectDeviceModel)
            put("probability", config.probability)
            put("maxSurveysPerSession", config.maxSurveysPerSession)
            put("cooldownPeriod", config.cooldownPeriod)
            put("cacheDurationHours", config.cacheDurationHours)
            put("triggerOnce", config.triggerOnce)
            put("exclusionRules", JSONArray().apply {
                config.exclusionRules.forEach { rule ->
                    put(JSONObject().apply {
                        put("name", rule.name)
                        put("source", rule.source.name)
                        put("key", rule.key ?: "")
                        put("value", rule.value ?: "")
                        put("operator", rule.operator.name)
                        put("matchValue", rule.matchValue)
                        put("caseSensitive", rule.caseSensitive)
                    })
                }
            })
        }.toString()

    }

    private fun jsonToConfig(jsonString: String): Config {
        val json = JSONObject(jsonString)

        return Config(
            baseUrl = json.optString("baseUrl", ""),
            sdkVersion = json.optString("sdkVersion", "1.1.5"),
            enableButtonTrigger = json.optBoolean("enableButtonTrigger", true),
            enableScrollTrigger = json.optBoolean("enableScrollTrigger", false),
            enableNavigationTrigger = json.optBoolean("enableNavigationTrigger", false),
            enableAppLaunchTrigger = json.optBoolean("enableAppLaunchTrigger", false),
            enableExitTrigger = json.optBoolean("enableExitTrigger", false),
            enableTabChangeTrigger = json.optBoolean("enableTabChangeTrigger", false),
            timeDelay = json.optLong("timeDelay", 0L),
            scrollThreshold = json.optInt("scrollThreshold", 0),
            navigationScreens = emptySet(), // Simplified for cache
            tabNames = emptySet(),
            triggerType = json.optString("triggerType", "instant"),
            modalStyle = json.optString("modalStyle", "full_screen"),
            animationType = json.optString("animationType", "slide_up"),
            backgroundColor = json.optString("backgroundColor", "#FFFFFF"),
            collectDeviceId = json.optBoolean("collectDeviceId", false),
            collectDeviceModel = json.optBoolean("collectDeviceModel", false),
            collectLocation = false,
            collectAppUsage = false,
            customParams = emptyList(), // Simplified for cache
            probability = json.optDouble("probability", 1.0),
            maxSurveysPerSession = json.optInt("maxSurveysPerSession", 0),
            cooldownPeriod = json.optLong("cooldownPeriod", 0L),
            cacheDurationHours = json.optLong("cacheDurationHours", DEFAULT_CACHE_DURATION_HOURS),
            triggerOnce = json.optBoolean("triggerOnce", false),
            exclusionRules = json.optJSONArray("exclusionRules")?.let { array ->
                (0 until array.length()).map { index ->
                    val ruleJson = array.getJSONObject(index)
                    ExclusionRule(
                        name = ruleJson.optString("name", ""),
                        source = ExclusionSource.valueOf(ruleJson.optString("source", "STATIC")),
                        key = ruleJson.optString("key", null),
                        value = ruleJson.optString("value", null),
                        operator = ExclusionOperator.valueOf(ruleJson.optString("operator", "EQUALS")),
                        matchValue = ruleJson.optString("matchValue", ""),
                        caseSensitive = ruleJson.optBoolean("caseSensitive", false)
                    )
                }
            } ?: emptyList()
        )
    }
}