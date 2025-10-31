package com.example.surveysdk

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class ConfigCacheManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "survey_config_cache"
        private const val KEY_CONFIG_JSON = "cached_config"
        private const val KEY_TIMESTAMP = "cache_timestamp"
        private const val DEFAULT_CACHE_DURATION_HOURS = 24L
    }

    fun saveConfig(config: Config) {
        try {
            val configJson = configToJson(config)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_CONFIG_JSON, configJson)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .apply()
            Log.d("ConfigCache", "Configuration cached successfully - ${config.surveys.size} surveys")
        } catch (e: Exception) {
            Log.e("ConfigCache", "Failed to cache configuration: ${e.message}")
        }
    }

    fun getCachedConfig(): Config? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedJson = prefs.getString(KEY_CONFIG_JSON, null) ?: return null
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)

            // Get cache duration from the config itself
            val config = jsonToConfig(cachedJson)
            val cacheDurationHours = config.cacheDurationHours

            // Check if cache is expired using config value
            val cacheAge = System.currentTimeMillis() - timestamp
            val cacheExpired = cacheAge > TimeUnit.HOURS.toMillis(cacheDurationHours)

            if (cacheExpired) {
                Log.d("ConfigCache", "Cache expired, age: ${cacheAge / 1000 / 60} minutes")
                clearCache()
                return null
            }

            Log.d("ConfigCache", "Using cached configuration - ${config.surveys.size} surveys, age: ${cacheAge / 1000 / 60} minutes")
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
        val jsonObject = JSONObject()

        jsonObject.put("sdkVersion", config.sdkVersion)
        jsonObject.put("cacheDurationHours", config.cacheDurationHours)

        // Serialize surveys array
        val surveysArray = JSONArray()
        config.surveys.forEach { survey ->
            val surveyJson = JSONObject()

            // Basic survey info
            surveyJson.put("surveyId", survey.surveyId)
            surveyJson.put("surveyName", survey.surveyName)
            surveyJson.put("baseUrl", survey.baseUrl)

            // Trigger Settings
            surveyJson.put("enableButtonTrigger", survey.enableButtonTrigger)
            surveyJson.put("enableScrollTrigger", survey.enableScrollTrigger)
            surveyJson.put("enableNavigationTrigger", survey.enableNavigationTrigger)
            surveyJson.put("enableAppLaunchTrigger", survey.enableAppLaunchTrigger)
            surveyJson.put("enableExitTrigger", survey.enableExitTrigger)
            surveyJson.put("enableTabChangeTrigger", survey.enableTabChangeTrigger)

            // Trigger Configuration
            val triggerScreensArray = JSONArray()
            survey.triggerScreens.forEach { screen -> triggerScreensArray.put(screen) }
            surveyJson.put("triggerScreens", triggerScreensArray)

            val triggerTabsArray = JSONArray()
            survey.triggerTabs.forEach { tab -> triggerTabsArray.put(tab) }
            surveyJson.put("triggerTabs", triggerTabsArray)

            surveyJson.put("timeDelay", survey.timeDelay)
            surveyJson.put("scrollThreshold", survey.scrollThreshold)
            surveyJson.put("triggerType", survey.triggerType)

            // Display Settings
            surveyJson.put("modalStyle", survey.modalStyle)
            surveyJson.put("animationType", survey.animationType)
            surveyJson.put("backgroundColor", survey.backgroundColor)

            // Targeting & Limits
            surveyJson.put("probability", survey.probability)
            surveyJson.put("maxShowsPerSession", survey.maxShowsPerSession)
            surveyJson.put("cooldownPeriod", survey.cooldownPeriod)
            surveyJson.put("triggerOnce", survey.triggerOnce)
            surveyJson.put("priority", survey.priority)

            // Data Collection
            surveyJson.put("collectDeviceId", survey.collectDeviceId)
            surveyJson.put("collectDeviceModel", survey.collectDeviceModel)
            surveyJson.put("collectLocation", survey.collectLocation)
            surveyJson.put("collectAppUsage", survey.collectAppUsage)

            // Custom Params
            val customParamsArray = JSONArray()
            survey.customParams.forEach { param ->
                val paramJson = JSONObject()
                paramJson.put("name", param.name)
                paramJson.put("source", param.source.name)
                paramJson.put("key", param.key ?: "")
                paramJson.put("value", param.value ?: "")
                paramJson.put("defaultValue", param.defaultValue ?: "")
                customParamsArray.put(paramJson)
            }
            surveyJson.put("customParams", customParamsArray)

            // Exclusion Rules
            val exclusionRulesArray = JSONArray()
            survey.exclusionRules.forEach { rule ->
                val ruleJson = JSONObject()
                ruleJson.put("name", rule.name)
                ruleJson.put("source", rule.source.name)
                ruleJson.put("key", rule.key ?: "")
                ruleJson.put("value", rule.value ?: "")
                ruleJson.put("operator", rule.operator.name)
                ruleJson.put("matchValue", rule.matchValue)
                ruleJson.put("caseSensitive", rule.caseSensitive)
                exclusionRulesArray.put(ruleJson)
            }
            surveyJson.put("exclusionRules", exclusionRulesArray)

            surveysArray.put(surveyJson)
        }
        jsonObject.put("surveys", surveysArray)

        return jsonObject.toString()
    }

    private fun jsonToConfig(jsonString: String): Config {
        val json = JSONObject(jsonString)

        val surveysArray = json.optJSONArray("surveys")
        val surveys = mutableListOf<SurveyConfig>()

        if (surveysArray != null) {
            for (i in 0 until surveysArray.length()) {
                val surveyJson = surveysArray.getJSONObject(i)

                surveys.add(SurveyConfig(
                    surveyId = surveyJson.getString("surveyId"),
                    surveyName = surveyJson.optString("surveyName", ""),
                    baseUrl = surveyJson.optString("baseUrl", ""),

                    // Trigger Settings
                    enableButtonTrigger = surveyJson.optBoolean("enableButtonTrigger", false),
                    enableScrollTrigger = surveyJson.optBoolean("enableScrollTrigger", false),
                    enableNavigationTrigger = surveyJson.optBoolean("enableNavigationTrigger", false),
                    enableAppLaunchTrigger = surveyJson.optBoolean("enableAppLaunchTrigger", false),
                    enableExitTrigger = surveyJson.optBoolean("enableExitTrigger", false),
                    enableTabChangeTrigger = surveyJson.optBoolean("enableTabChangeTrigger", false),

                    // Trigger Configuration
                    triggerScreens = surveyJson.optJSONArray("triggerScreens")?.let { array ->
                        (0 until array.length()).map { array.getString(it) }.toSet()
                    } ?: emptySet(),
                    triggerTabs = surveyJson.optJSONArray("triggerTabs")?.let { array ->
                        (0 until array.length()).map { array.getString(it) }.toSet()
                    } ?: emptySet(),
                    timeDelay = surveyJson.optLong("timeDelay", 0L),
                    scrollThreshold = surveyJson.optInt("scrollThreshold", 0),
                    triggerType = surveyJson.optString("triggerType", "instant"),

                    // Display Settings
                    modalStyle = surveyJson.optString("modalStyle", "full_screen"),
                    animationType = surveyJson.optString("animationType", "slide_up"),
                    backgroundColor = surveyJson.optString("backgroundColor", "#FFFFFF"),

                    // Targeting & Limits
                    probability = surveyJson.optDouble("probability", 1.0),
                    maxShowsPerSession = surveyJson.optInt("maxShowsPerSession", 0),
                    cooldownPeriod = surveyJson.optLong("cooldownPeriod", 0L),
                    triggerOnce = surveyJson.optBoolean("triggerOnce", false),
                    priority = surveyJson.optInt("priority", 1),

                    // Data Collection
                    collectDeviceId = surveyJson.optBoolean("collectDeviceId", false),
                    collectDeviceModel = surveyJson.optBoolean("collectDeviceModel", false),
                    collectLocation = surveyJson.optBoolean("collectLocation", false),
                    collectAppUsage = surveyJson.optBoolean("collectAppUsage", false),

                    // Custom Params
                    customParams = surveyJson.optJSONArray("customParams")?.let { array ->
                        (0 until array.length()).map { index ->
                            val paramJson = array.getJSONObject(index)
                            CustomParam(
                                name = paramJson.getString("name"),
                                source = ParamSource.valueOf(paramJson.getString("source")),
                                key = paramJson.optString("key", null),
                                value = paramJson.optString("value", null),
                                defaultValue = paramJson.optString("defaultValue", null)
                            )
                        }
                    } ?: emptyList(),

                    // Exclusion Rules
                    exclusionRules = surveyJson.optJSONArray("exclusionRules")?.let { array ->
                        (0 until array.length()).map { index ->
                            val ruleJson = array.getJSONObject(index)
                            ExclusionRule(
                                name = ruleJson.getString("name"),
                                source = ExclusionSource.valueOf(ruleJson.getString("source")),
                                key = ruleJson.optString("key", null),
                                value = ruleJson.optString("value", null),
                                operator = ExclusionOperator.valueOf(ruleJson.getString("operator")),
                                matchValue = ruleJson.getString("matchValue"),
                                caseSensitive = ruleJson.optBoolean("caseSensitive", false)
                            )
                        }
                    } ?: emptyList()
                ))
            }
        }

        return Config(
            sdkVersion = json.optString("sdkVersion", "2.0.0"),
            cacheDurationHours = json.optLong("cacheDurationHours", 24L),
            surveys = surveys
        )
    }
}