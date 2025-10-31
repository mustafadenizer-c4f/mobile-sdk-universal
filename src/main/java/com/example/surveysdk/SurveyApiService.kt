package com.example.surveysdk

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SurveyApiService(private val apiKey: String) {

    companion object {
        private const val BASE_URL = "https://your-api-domain.com/api"
        private const val CONFIG_ENDPOINT = "$BASE_URL/sdk/config"
        private const val TIMEOUT_MS = 5000
    }

    interface ConfigCallback {
        fun onConfigLoaded(config: Config?)
        fun onError(error: String)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun fetchConfiguration(callback: ConfigCallback) {
        coroutineScope.launch {
            try {
                val config = withContext(Dispatchers.IO) {
                    fetchConfigFromNetwork()
                }
                if (config != null) {
                    callback.onConfigLoaded(config)
                } else {
                    // API failed - return null to trigger fallback in SurveySDK
                    callback.onConfigLoaded(null)
                }
            } catch (e: Exception) {
                Log.e("SurveyApiService", "API call failed: ${e.message}")
                // API failed - return null to trigger fallback in SurveySDK
                callback.onConfigLoaded(null)
            }
        }
    }

    fun cleanup() {
        coroutineScope.cancel()
    }

    private suspend fun fetchConfigFromNetwork(): Config? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("$CONFIG_ENDPOINT?apiKey=$apiKey")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", "SurveySDK/1.0.0")
                }

                val responseCode = connection.responseCode
                Log.d("SurveyApiService", "API Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val responseBody = inputStream.bufferedReader().use { it.readText() }
                    Log.d("SurveyApiService", "✅ Config loaded from API")
                    parseConfigFromJson(responseBody)
                } else {
                    Log.e("SurveyApiService", "❌ API error: $responseCode")
                    null // Return null to trigger fallback
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e("SurveyApiService", "❌ Network error - using fallback: ${e.message}")
            null // Return null to trigger fallback
        }
    }

    // Keep existing parse methods exactly as they are...
    private fun parseConfigFromJson(jsonString: String): Config {
        val json = JSONObject(jsonString)

        return Config(
            baseUrl = json.optString("baseUrl", ""),
            sdkVersion = json.optString("sdkVersion", "1.0.0"),
            enableButtonTrigger = json.optBoolean("enableButtonTrigger", false),
            enableScrollTrigger = json.optBoolean("enableScrollTrigger", false),
            enableNavigationTrigger = json.optBoolean("enableNavigationTrigger", false),
            enableAppLaunchTrigger = json.optBoolean("enableAppLaunchTrigger", false),
            enableExitTrigger = json.optBoolean("enableExitTrigger", false),
            enableTabChangeTrigger = json.optBoolean("enableTabChangeTrigger", false),
            timeDelay = json.optLong("timeDelay", 0L),
            scrollThreshold = json.optInt("scrollThreshold", 0),
            navigationScreens = json.optJSONArray("navigationScreens")?.let { array ->
                (0 until array.length()).map { array.getString(it) }.toSet()
            } ?: emptySet(),
            tabNames = json.optJSONArray("tabNames")?.let { array ->
                (0 until array.length()).map { array.getString(it) }.toSet()
            } ?: emptySet(),
            triggerType = json.optString("triggerType", "instant"),
            modalStyle = json.optString("modalStyle", "full_screen"),
            animationType = json.optString("animationType", "slide_up"),
            backgroundColor = json.optString("backgroundColor", "#FFFFFF"),
            collectDeviceId = json.optBoolean("collectDeviceId", false),
            collectDeviceModel = json.optBoolean("collectDeviceModel", false),
            collectLocation = json.optBoolean("collectLocation", false),
            collectAppUsage = json.optBoolean("collectAppUsage", false),
            customParams = parseCustomParams(json.optJSONArray("customParams")),
            probability = json.optDouble("probability", 1.0),
            maxSurveysPerSession = json.optInt("maxSurveysPerSession", 0),
            cooldownPeriod = json.optLong("cooldownPeriod", 0L),
            cacheDurationHours = json.optLong("cacheDurationHours", 24L),
            triggerOnce = json.optBoolean("triggerOnce", false),
            exclusionRules = parseExclusionRules(json.optJSONArray("exclusionRules"))
        )
    }

    private fun parseExclusionRules(jsonArray: JSONArray?): List<ExclusionRule> {
        return jsonArray?.let { array ->
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
    }

    private fun parseCustomParams(jsonArray: JSONArray?): List<CustomParam> {
        return jsonArray?.let { array ->
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
        } ?: emptyList()
    }
}