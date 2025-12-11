package com.example.surveysdk

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SurveyApiService(private val apiKey: String) {

    companion object {
        private const val BASE_URL = "https://form.cloud4feed.com/api"
        private const val CONFIG_ENDPOINT = "$BASE_URL/setting/mobilesdk-config"
        private const val TIMEOUT_MS = 10000 // Increased timeout
        private const val MAX_RETRIES = 2
    }

    interface ConfigCallback {
        fun onConfigLoaded(config: Config?)
        fun onError(error: String)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null

    fun fetchConfiguration(callback: ConfigCallback, retryCount: Int = 0) {
        currentJob?.cancel() // Cancel previous request

        currentJob = coroutineScope.launch {
            try {
                val config = withContext(Dispatchers.IO) {
                    fetchConfigFromNetwork(retryCount)
                }
                if (config != null) {
                    callback.onConfigLoaded(config)
                } else {
                    callback.onError("Failed to load configuration")
                    callback.onConfigLoaded(null)
                }
            } catch (e: Exception) {
                Log.e("SurveyApiService", "API call failed: ${e.message}")
                if (retryCount < MAX_RETRIES && e !is CancellationException) {
                    Log.d("SurveyApiService", "Retrying... (${retryCount + 1}/$MAX_RETRIES)")
                    fetchConfiguration(callback, retryCount + 1)
                } else {
                    callback.onError(e.message ?: "Unknown error")
                    callback.onConfigLoaded(null)
                }
            }
        }
    }

    fun cleanup() {
        currentJob?.cancel()
        coroutineScope.cancel()
    }

    private suspend fun fetchConfigFromNetwork(retryCount: Int = 0): Config? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (retryCount > 0) {
                delay(1000L * retryCount)
            }

            val url = URL("$CONFIG_ENDPOINT?apiKey=$apiKey&timestamp=${System.currentTimeMillis()}&sdkVersion=${SDKConstants.SDK_VERSION}")
            Log.d("SurveyApiService", "🔗 Making API request to: $url")
            val connection = url.openConnection() as HttpsURLConnection

            try {
                connection.apply {
                    connectTimeout = SDKConstants.API_TIMEOUT_MS.toInt()
                    readTimeout = SDKConstants.API_TIMEOUT_MS.toInt()
                    requestMethod = "GET"
                    // ✅ Call setRequestProperty on connection object
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", "${SDKConstants.SDK_NAME}/${SDKConstants.SDK_VERSION}")
                    setRequestProperty("Cache-Control", "no-cache")

                    // Security headers
                    instanceFollowRedirects = false
                    useCaches = false
                }

                val responseCode = connection.responseCode
                Log.d(SDKConstants.LOG_TAG_API, "API Response code: $responseCode")

                when (responseCode) {
                    HttpsURLConnection.HTTP_OK -> {
                        val inputStream = connection.inputStream
                        val responseBody = inputStream.bufferedReader().use { it.readText() }
                        Log.d(SDKConstants.LOG_TAG_API, "✅ Multi-survey config loaded from API")
                        parseConfigFromJson(responseBody)
                    }
                    HttpsURLConnection.HTTP_UNAUTHORIZED -> {
                        Log.e(SDKConstants.LOG_TAG_API, "❌ API key invalid")
                        null
                    }
                    else -> {
                        Log.e(SDKConstants.LOG_TAG_API, "❌ API error: $responseCode - ${connection.responseMessage}")
                        null
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(SDKConstants.LOG_TAG_API, "❌ Network error - using fallback: ${e.message}")
            null
        }
    }

    private fun parseConfigFromJson(jsonString: String): Config {
        return try {
            val json = JSONObject(jsonString)

            val surveysArray = json.optJSONArray("surveys")
            val surveys = mutableListOf<SurveyConfig>()

            if (surveysArray != null) {
                for (i in 0 until surveysArray.length()) {
                    try {
                        val surveyJson = surveysArray.getJSONObject(i)
                        val survey = parseSurveyConfigFromJson(surveyJson)

                        // ✅ ADD: Validate survey before adding
                        if (survey.surveyId != null && survey.baseUrl != null) {
                            surveys.add(survey)
                        } else {
                            Log.w("SurveyApiService", "⚠️ Skipping invalid survey in config")
                        }
                    } catch (e: Exception) {
                        Log.e("SurveyApiService", "❌ Error parsing survey at index $i: ${e.message}")
                    }
                }
            }

            Config(
                sdkVersion = json.optString("sdkVersion", "2.0.0") ?: "2.0.0",
                cacheDurationHours = json.optLong("cacheDurationHours", 24L),
                surveys = surveys
            )
        } catch (e: Exception) {
            Log.e("SurveyApiService", "Error parsing config JSON: ${e.message}")
            DefaultConfig.EMPTY_CONFIG
        }
    }

    private fun parseSurveyConfigFromJson(surveyJson: JSONObject): SurveyConfig {
    return SurveyConfig(
        surveyId = surveyJson.opt("surveyId").toString(), // Handle both string and number
        surveyName = surveyJson.optString("surveyName", ""),
        baseUrl = surveyJson.optString("baseUrl", ""),
        status = surveyJson.optBoolean("status", true),
        // Trigger Settings
        enableButtonTrigger = surveyJson.optBoolean("enableButtonTrigger", false),
        enableScrollTrigger = surveyJson.optBoolean("enableScrollTrigger", false),
        enableNavigationTrigger = surveyJson.optBoolean("enableNavigationTrigger", false),
        enableAppLaunchTrigger = surveyJson.optBoolean("enableAppLaunchTrigger", false),
        enableExitTrigger = surveyJson.optBoolean("enableExitTrigger", false),
        enableTabChangeTrigger = surveyJson.optBoolean("enableTabChangeTrigger", false),

        buttonTriggerId = surveyJson.optString("buttonTriggerId", null).takeIf { it.isNotEmpty() },

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
        customParams = parseCustomParams(surveyJson.optJSONArray("customParams")),

        // Exclusion Rules - Handle integer operators
        exclusionRules = parseExclusionRules(surveyJson.optJSONArray("exclusionRules"))
    )
}

   private fun parseExclusionRules(jsonArray: JSONArray?): List<ExclusionRule> {
    return jsonArray?.let { array ->
        (0 until array.length()).mapNotNull { index ->
            try {
                val ruleJson = array.getJSONObject(index)
                
                // Handle operator as both integer and string
                val operator = if (ruleJson.has("operator")) {
                    when (val operatorValue = ruleJson.get("operator")) {
                        is Int -> ExclusionOperator.fromId(operatorValue)
                        is String -> ExclusionOperator.fromName(operatorValue)
                        else -> ExclusionOperator.EQUALS
                    }
                } else {
                    ExclusionOperator.EQUALS
                }

                ExclusionRule(
                    name = ruleJson.optString("name", "rule_$index"),
                    source = ExclusionSource.valueOf(ruleJson.optString("source", "STORAGE")),
                    key = ruleJson.optString("key", null),
                    value = ruleJson.optString("value", null),
                    operator = operator,
                    matchValue = ruleJson.optString("matchValue", ""),
                    caseSensitive = ruleJson.optBoolean("caseSensitive", false)
                )
            } catch (e: Exception) {
                Log.e("SurveyApiService", "Error parsing exclusion rule: ${e.message}")
                null
            }
        }
    } ?: emptyList()
}

    private fun parseCustomParams(jsonArray: JSONArray?): List<CustomParam> {
        return jsonArray?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                try {
                    val paramJson = array.getJSONObject(index)
                    CustomParam(
                        name = paramJson.getString("name"),
                        source = ParamSource.valueOf(paramJson.getString("source")),
                        key = paramJson.optString("key", null),
                        value = paramJson.optString("value", null),
                        defaultValue = paramJson.optString("defaultValue", null)
                    )
                } catch (e: Exception) {
                    Log.e("SurveyApiService", "Error parsing custom param: ${e.message}")
                    null
                }
            }
        } ?: emptyList()
    }
}