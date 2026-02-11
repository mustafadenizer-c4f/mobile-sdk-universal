package com.example.surveysdk

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.nio.charset.StandardCharsets

class SurveyApiService(private val apiKey: String) {

    companion object {
        private const val BASE_URL = "https://anonym.cloud4feed.com/api"
        private const val CONFIG_ENDPOINT = "$BASE_URL/anonym/get-mobilesdk-surveys"
        private const val TIMEOUT_MS = 10000
        private const val MAX_RETRIES = 2
    }

    interface ConfigCallback {
        fun onConfigLoaded(config: Config?)
        fun onError(error: String)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null

    fun fetchConfiguration(
        callback: ConfigCallback,
        retryCount: Int = 0,
        params: Map<String, String> = emptyMap()
    ) {
        currentJob?.cancel()

        currentJob = coroutineScope.launch {
            try {
                val config = withContext(Dispatchers.IO) {
                    fetchConfigFromNetwork(retryCount, params)
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
                    fetchConfiguration(callback, retryCount + 1, params)
                } else {
                    callback.onError(e.message ?: "Unknown error")
                    callback.onConfigLoaded(null)
                }
            }
        }
    }

    private suspend fun fetchConfigFromNetwork(
        retryCount: Int = 0,
        params: Map<String, String> = emptyMap()
        ): Config? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("SurveyApiService", "🌐 ========== NETWORK REQUEST ==========")
            Log.d("SurveyApiService", "   • URL: $CONFIG_ENDPOINT")
            Log.d("SurveyApiService", "   • Retry attempt: $retryCount")
            
            if (retryCount > 0) {
                delay(1000L * retryCount)
                Log.d(SDKConstants.LOG_TAG_API, "🔄 Retry attempt $retryCount after delay")
            }

            // Build JSON request body
            val requestBody = buildRequestBody(params)
            val requestBodyString = requestBody.toString()
            
            // Log request details
            logRequestDetails(params, requestBody)
            
            val url = URL(CONFIG_ENDPOINT)
            val connection = url.openConnection() as HttpsURLConnection

            try {
                // Configure connection for POST
                configurePostConnection(connection, requestBodyString)
                
                // Send request body
                sendRequestBody(connection, requestBodyString)
                
                // Handle response
                val config = handleResponse(connection, params)
                
                if (config != null) {
                    Log.d("SurveyApiService", "✅ REQUEST SUCCESSFUL")
                    Log.d("SurveyApiService", "   • Surveys received: ${config.surveys.size}")
                } else {
                    Log.w("SurveyApiService", "⚠️ REQUEST RETURNED NULL CONFIG")
                }
                
                config

            } finally {
                connection.disconnect()
                Log.d(SDKConstants.LOG_TAG_API, "🔌 Connection closed")
            }
        } catch (e: Exception) {
            Log.e("SurveyApiService", "❌ NETWORK REQUEST FAILED: ${e.message}")
            Log.e("SurveyApiService", "❌ Stack trace:", e)
            handleNetworkError(e)
            null
        }
    }

    private fun buildRequestBody(params: Map<String, String>): JSONObject {
        return JSONObject().apply {
            put("apiKey", apiKey)
            // Convert timestamp to STRING (not number)
            put("timestamp", System.currentTimeMillis().toString())
            put("sdkVersion", SDKConstants.SDK_VERSION)
            
            // Build sdkParams array
            val sdkParamsArray = JSONArray()
            params.forEach { (paramName, paramValue) ->
                val paramObj = JSONObject().apply {
                    put("paramName", paramName)
                    put("paramValue", paramValue)
                }
                sdkParamsArray.put(paramObj)
            }
            put("sdkParams", sdkParamsArray)
            
            Log.d("SurveyApiService", "✅ Built request with timestamp as String")
        }
    }

    private fun logRequestDetails(params: Map<String, String>, requestBody: JSONObject) {
        Log.d("SurveyApiService", "📤 ========== REQUEST DETAILS ==========")
        Log.d("SurveyApiService", "   • Endpoint: $CONFIG_ENDPOINT")
        Log.d("SurveyApiService", "   • API Key: ${apiKey.take(5)}...")
        Log.d("SurveyApiService", "   • Timestamp type: ${requestBody.get("timestamp")::class.java.simpleName}")
        Log.d("SurveyApiService", "   • Timestamp value: ${requestBody.get("timestamp")}")
        
        Log.d("SurveyApiService", "   • Request Body JSON:")
        Log.d("SurveyApiService", requestBody.toString(2))
        
        // Verify format matches Swagger
        val timestamp = requestBody.get("timestamp")
        if (timestamp is String) {
            Log.d("SurveyApiService", "✅ Timestamp is STRING (correct)")
        } else {
            Log.e("SurveyApiService", "❌ ERROR: Timestamp is ${timestamp::class.java.simpleName}, should be STRING")
        }
        
        Log.d("SurveyApiService", "=====================================")
    }

    private fun configurePostConnection(connection: HttpsURLConnection, requestBodyString: String) {
        connection.apply {
            connectTimeout = SDKConstants.API_TIMEOUT_MS.toInt()
            readTimeout = SDKConstants.API_TIMEOUT_MS.toInt()
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "${SDKConstants.SDK_NAME}/${SDKConstants.SDK_VERSION}")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Content-Length", requestBodyString.length.toString())
            doOutput = true
            doInput = true
            instanceFollowRedirects = false
            useCaches = false
        }
        
        Log.d(SDKConstants.LOG_TAG_API, "🔌 Connection configured for POST")
    }

    private fun sendRequestBody(connection: HttpsURLConnection, requestBodyString: String) {
        try {
            val outputStream = connection.outputStream
            outputStream.write(requestBodyString.toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()
            outputStream.close()
            Log.d(SDKConstants.LOG_TAG_API, "✅ Request body sent (${requestBodyString.length} bytes)")
        } catch (e: Exception) {
            throw IOException("Failed to send request body: ${e.message}", e)
        }
    }

    private fun handleResponse(connection: HttpsURLConnection, params: Map<String, String>): Config? {
        val startTime = System.currentTimeMillis()
        val responseCode = connection.responseCode
        val responseTime = System.currentTimeMillis() - startTime
        
        Log.d("SurveyApiService", "📡 ========== RESPONSE RECEIVED ==========")
        Log.d("SurveyApiService", "   • Status: $responseCode")
        Log.d("SurveyApiService", "   • Time: ${responseTime}ms")
        Log.d("SurveyApiService", "   • Headers:")
        
        // Log response headers
        connection.headerFields?.forEach { (key, value) ->
            if (key != null) {
                Log.d("SurveyApiService", "     - $key: ${value?.firstOrNull()}")
            }
        }
        
        return when (responseCode) {
            HttpsURLConnection.HTTP_OK -> {
                Log.d("SurveyApiService", "✅ HTTP 200 OK")
                handleSuccessResponse(connection, params)
            }
            else -> {
                Log.e("SurveyApiService", "❌ HTTP Error ($responseCode)")
                handleErrorResponse(connection, responseCode)
                null
            }
        }
    }

    private fun logResponseDetails(responseBody: String) {
        Log.d("SurveyApiService", "📄 ========== RESPONSE BODY ==========")
        try {
            val json = JSONObject(responseBody)
            Log.d("SurveyApiService", json.toString(2))
        } catch (e: Exception) {
            Log.d("SurveyApiService", responseBody.take(1000))
        }
        Log.d("SurveyApiService", "==================================")
    }

    private fun handleSuccessResponse(connection: HttpsURLConnection, params: Map<String, String>): Config? {
        return try {
            val inputStream = connection.inputStream
            val responseBody = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            inputStream.close()
            
            Log.d("SurveyApiService", "✅ SUCCESS! Response received")
            Log.d("SurveyApiService", "📄 Full Response Body:")
            Log.d("SurveyApiService", responseBody)
            
            // Parse the config from the response
            val config = parseConfigFromJsonResponse(responseBody)
            
            if (config.surveys.isEmpty()) {
                Log.w("SurveyApiService", "⚠️ No surveys in response, backend may have issues")
            }
            
            config
        } catch (e: Exception) {
            Log.e("SurveyApiService", "❌ Failed to parse response: ${e.message}")
            null
        }
    }

    private fun handleErrorResponse(connection: HttpsURLConnection, responseCode: Int) {
        Log.e(SDKConstants.LOG_TAG_API, "❌ HTTP Error ($responseCode)")
        try {
            val errorStream = connection.errorStream
            if (errorStream != null) {
                val errorBody = errorStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                errorStream.close()
                if (errorBody.isNotEmpty()) {
                    Log.e(SDKConstants.LOG_TAG_API, "   └── Error details: $errorBody")
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun handleNetworkError(e: Exception) {
        Log.e(SDKConstants.LOG_TAG_API, "❌ NETWORK ERROR: ${e.message}")
    }

    fun cleanup() {
        currentJob?.cancel()
        coroutineScope.cancel()
    }

    private fun parseConfigFromJsonResponse(jsonString: String): Config {
        return try {

/////////////////// TEST JSON ///////////////
            val testJsonString = """
        {
          "duration": "00:00:00.016",
          "data": {
            "sdkVersion": "2.0.0",
            "cacheDurationHours": 24,
            "surveys": [
              {
                "baseUrl": "https://cx.cloud4feed.com/?c=7i1C-MDAwMDU1-MDAy-D1FD",
                "status": true,
                "enableButtonTrigger": false,
                "enableScrollTrigger": false,
                "enableNavigationTrigger": false,
                "enableAppLaunchTrigger": true,
                "enableExitTrigger": false,
                "enableTabChangeTrigger": false,
                "buttonTriggerId": "",
                "triggerScreens": [],
                "triggerTabs": [],
                "timeDelay": 0,
                "scrollThreshold": 500,
                "triggerType": "instant",
                "modalStyle": "dialog",
                "animationType": "slide_up",
                "backgroundColor": "#FFFFFF",
                "exclusionRules": [],
                "probability": 1,
                "maxShowsPerSession": 50,
                "cooldownPeriod": 0,
                "triggerOnce": false,
                "priority": "2",
                "collectDeviceId": false,
                "collectDeviceModel": false,
                "collectLocation": false,
                "collectAppUsage": false,
                "customParams": []
              },              
              {
                "baseUrl": "https://cx.cloud4feed.com/?c=PTzb-MDAwMDAy-MDEx-0zMC",
                "status": true,
                "enableButtonTrigger": true,
                "enableScrollTrigger": true,
                "enableNavigationTrigger": false,
                "enableAppLaunchTrigger": false,
                "enableExitTrigger": false,
                "enableTabChangeTrigger": false,
                "buttonTriggerId": "my_custom_button",
                "triggerScreens": [],
                "triggerTabs": [],
                "timeDelay": 0,
                "scrollThreshold": 500,
                "triggerType": "instant",
                "modalStyle": "dialog",
                "animationType": "slide_up",
                "backgroundColor": "#FFFFFF",
                "exclusionRules": [],
                "probability": 1,
                "maxShowsPerSession": 50,
                "cooldownPeriod": 0,
                "triggerOnce": false,
                "priority": "2",
                "collectDeviceId": false,
                "collectDeviceModel": false,
                "collectLocation": false,
                "collectAppUsage": false,
                "customParams": []
                },
                {
                "baseUrl": "https://cx.cloud4feed.com/?c=uGTx-MDAwMDU1-MDA1-Nsg7",
                "status": true,
                "enableButtonTrigger": false,
                "enableScrollTrigger": false,
                "enableNavigationTrigger": true,
                "enableAppLaunchTrigger": false,
                "enableExitTrigger": false,
                "enableTabChangeTrigger": false,
                "buttonTriggerId": "",
                "triggerScreens": ["notifications"],
                "triggerTabs": [],
                "timeDelay": 0,
                "scrollThreshold": 500,
                "triggerType": "instant",
                "modalStyle": "dialog",
                "animationType": "slide_up",
                "backgroundColor": "#FFFFFF",
                "exclusionRules": [],
                "probability": 1,
                "maxShowsPerSession": 50,
                "cooldownPeriod": 0,
                "triggerOnce": false,
                "priority": "2",
                "collectDeviceId": false,
                "collectDeviceModel": false,
                "collectLocation": false,
                "collectAppUsage": false,
                "customParams": []
              }         
            ]
          },
          "dataCount": 1,
          "dataPage": 1,
          "totalPage": 1,
          "pageSize": 10,
          "messages": [
            {
              "message": "Process completed with no error.",
              "messageCode": "",
              "messageType": 1,
              "success": true,
              "ticks": 639058869824820500,
              "datetime": "2026-02-05 11:16:22.482",
              "timestamp": 1770290182
            },
            {
                "baseUrl": "https://cx.cloud4feed.com/?c=uGTx-MDAwMDU1-MDA1-Nsg7",
                "status": true,
                "enableButtonTrigger": false,
                "enableScrollTrigger": false,
                "enableNavigationTrigger": true,
                "enableAppLaunchTrigger": false,
                "enableExitTrigger": false,
                "enableTabChangeTrigger": false,
                "buttonTriggerId": "",
                "triggerScreens": ["notifications"],
                "triggerTabs": [],
                "timeDelay": 0,
                "scrollThreshold": 500,
                "triggerType": "instant",
                "modalStyle": "dialog",
                "animationType": "slide_up",
                "backgroundColor": "#FFFFFF",
                "exclusionRules": [],
                "probability": 1,
                "maxShowsPerSession": 50,
                "cooldownPeriod": 0,
                "triggerOnce": false,
                "priority": "2",
                "collectDeviceId": false,
                "collectDeviceModel": false,
                "collectLocation": false,
                "collectAppUsage": false,
                "customParams": []
              }
          ],
          "success": true
        }
        """.trimIndent()
        
        // Use test JSON instead of real response
        val json = JSONObject(testJsonString)
        ///////////////////////////////////////////

            //val json = JSONObject(jsonString)  // PRODUCTION
            
            // Check for backend errors first
            val success = json.optBoolean("success", true)
            
            // Check messages array for errors
            val messagesArray = json.optJSONArray("messages")
            if (messagesArray != null) {
                for (i in 0 until messagesArray.length()) {
                    val messageObj = messagesArray.getJSONObject(i)
                    val messageSuccess = messageObj.optBoolean("success", true)
                    val message = messageObj.optString("message", "")
                    
                    if (!messageSuccess && message.isNotEmpty()) {
                        Log.e("SurveyApiService", "❌ Backend error in message: $message")
                        throw IOException("Backend error: ${message.take(100)}")
                    }
                }
            }
            
            // Check if response has "data" wrapper
            val configJson = if (json.has("data") && !json.isNull("data")) {
                json.getJSONObject("data")
            } else {
                json // Fallback to direct config
            }
            
            Log.d("SurveyApiService", "📦 Parsing config from ${if (json.has("data")) "data wrapper" else "direct JSON"}")
            
            val surveysArray = configJson.optJSONArray("surveys")
            val surveys = mutableListOf<SurveyConfig>()

            if (surveysArray != null && !surveysArray.toString().equals("null", ignoreCase = true)) {
                Log.d("SurveyApiService", "📊 Found ${surveysArray.length()} surveys in response")
                for (i in 0 until surveysArray.length()) {
                    try {
                        val surveyJson = surveysArray.getJSONObject(i)
                        val survey = parseSurveyConfigFromJson(surveyJson)
                        surveys.add(survey)
                    } catch (e: Exception) {
                        Log.e("SurveyApiService", "Error parsing survey at index $i: ${e.message}")
                    }
                }
            } else {
                Log.w("SurveyApiService", "⚠️ No surveys array found or surveys is null")
                // Return empty config if surveys is null
                return DefaultConfig.EMPTY_CONFIG
            }
            
            // Validate required fields
            val sdkVersion = configJson.optString("sdkVersion", SDKConstants.SDK_VERSION)
            val cacheDurationHours = configJson.optLong("cacheDurationHours", SDKConstants.CACHE_DURATION_HOURS)
            
            if (sdkVersion == "null" || cacheDurationHours <= 0) {
                Log.w("SurveyApiService", "⚠️ Invalid config values, using defaults")
                return DefaultConfig.EMPTY_CONFIG
            }

            Config(
                sdkVersion = sdkVersion,
                cacheDurationHours = cacheDurationHours,
                surveys = surveys
            )
        } catch (e: Exception) {
            Log.e("SurveyApiService", "❌ Error parsing config JSON: ${e.message}")
            DefaultConfig.EMPTY_CONFIG
        }
    }

    // JSON parsing methods (keep your existing ones)
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
                        surveys.add(survey)
                    } catch (e: Exception) {
                        Log.e("SurveyApiService", "Error parsing survey at index $i: ${e.message}")
                    }
                }
            }

            Config(
                sdkVersion = json.optString("sdkVersion", "2.0.0"),
                cacheDurationHours = json.optLong("cacheDurationHours", 24L),
                surveys = surveys
            )
        } catch (e: Exception) {
            Log.e("SurveyApiService", "Error parsing config JSON: ${e.message}")
            DefaultConfig.EMPTY_CONFIG
        }
    }

    private fun parseSurveyConfigFromJson(surveyJson: JSONObject): SurveyConfig {
        Log.d("TRACE", "📍 STEP 2: parseSurveyConfigFromJson() called")
        
        // Check if customParams exists in JSON
        if (surveyJson.has("customParams")) {
            Log.d("TRACE", "   ✅ JSON has 'customParams' field")
            val customParamsArray = surveyJson.optJSONArray("customParams")
            Log.d("TRACE", "   • customParamsArray: $customParamsArray")
            Log.d("TRACE", "   • customParamsArray is null: ${customParamsArray == null}")
            
            if (customParamsArray != null) {
                Log.d("TRACE", "   • customParamsArray length: ${customParamsArray.length()}")
                Log.d("TRACE", "   • customParamsArray content: ${customParamsArray.toString(2)}")
            }
        } else {
            Log.d("TRACE", "   ❌ JSON does NOT have 'customParams' field")
            Log.d("TRACE", "   • All JSON keys: ${surveyJson.keys().asSequence().toList()}")
        }

        // Handle priority which can be String or Int
        val priorityValue = surveyJson.opt("priority")
        val priority = when (priorityValue) {
            is String -> priorityValue.toIntOrNull() ?: SDKConstants.DEFAULT_PRIORITY
            is Int -> priorityValue
            is Double -> priorityValue.toInt()
            else -> SDKConstants.DEFAULT_PRIORITY
        }

         // Get surveyId from backend, generate random if empty/null
            val backendSurveyId = surveyJson.optString("surveyId", "")
            val surveyId = if (backendSurveyId.isNotEmpty()) {
                backendSurveyId  // Use backend ID if provided
            } else {
                // Generate random ID: "survey_timestamp_random"
                "survey_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
            }
    
        return SurveyConfig(
            surveyId = surveyId,  // Add surveyId field
            surveyName = surveyJson.optString("surveyName", ""),  // Add surveyName field
            baseUrl = surveyJson.optString("baseUrl", ""),
            status = surveyJson.optBoolean("status", true),
            enableButtonTrigger = surveyJson.optBoolean("enableButtonTrigger", false),
            enableScrollTrigger = surveyJson.optBoolean("enableScrollTrigger", false),
            enableNavigationTrigger = surveyJson.optBoolean("enableNavigationTrigger", false),
            enableAppLaunchTrigger = surveyJson.optBoolean("enableAppLaunchTrigger", false),
            enableExitTrigger = surveyJson.optBoolean("enableExitTrigger", false),
            enableTabChangeTrigger = surveyJson.optBoolean("enableTabChangeTrigger", false),
            buttonTriggerId = surveyJson.optString("buttonTriggerId", null).takeIf { it.isNotEmpty() },
            triggerScreens = surveyJson.optJSONArray("triggerScreens")?.let { array ->
                (0 until array.length()).mapNotNull { 
                    if (!array.isNull(it)) array.getString(it) else null 
                }.toSet()
            } ?: emptySet(),
            triggerTabs = surveyJson.optJSONArray("triggerTabs")?.let { array ->
                (0 until array.length()).mapNotNull { 
                    if (!array.isNull(it)) array.getString(it) else null 
                }.toSet()
            } ?: emptySet(),
            timeDelay = surveyJson.optLong("timeDelay", 0L),
            scrollThreshold = surveyJson.optInt("scrollThreshold", 0),
            triggerType = surveyJson.optString("triggerType", "instant"),
            modalStyle = surveyJson.optString("modalStyle", "full_screen"),
            animationType = surveyJson.optString("animationType", "slide_up"),
            backgroundColor = surveyJson.optString("backgroundColor", "#FFFFFF"),
            probability = surveyJson.optDouble("probability", 1.0),
            maxShowsPerSession = surveyJson.optInt("maxShowsPerSession", 0),
            cooldownPeriod = surveyJson.optLong("cooldownPeriod", 0L),
            triggerOnce = surveyJson.optBoolean("triggerOnce", false),
            priority = priority,
            collectDeviceId = surveyJson.optBoolean("collectDeviceId", false),
            collectDeviceModel = surveyJson.optBoolean("collectDeviceModel", false),
            collectLocation = surveyJson.optBoolean("collectLocation", false),
            collectAppUsage = surveyJson.optBoolean("collectAppUsage", false),
            customParams = parseCustomParams(surveyJson.optJSONArray("customParams")),
            exclusionRules = parseExclusionRules(surveyJson.optJSONArray("exclusionRules"))
        )
    }

    private fun parseCustomParams(jsonArray: JSONArray?): List<CustomParam> {
        Log.d("TRACE", "📍 STEP 1: parseCustomParams() called")
        Log.d("TRACE", "   • jsonArray: $jsonArray")
        Log.d("TRACE", "   • jsonArray is null: ${jsonArray == null}")
        
        return jsonArray?.let { array ->
            Log.d("TRACE", "   • Array length: ${array.length()}")
            
            (0 until array.length()).mapNotNull { index ->
                try {
                    val paramJson = array.getJSONObject(index)
                    Log.d("TRACE", "   📦 Parsing index $index: ${paramJson.toString(2)}")
                    
                    val param = CustomParam(
                        name = paramJson.getString("name"),
                        source = ParamSource.valueOf(paramJson.getString("source")),
                        key = paramJson.optString("key", null),
                        value = paramJson.optString("value", null),
                        defaultValue = paramJson.optString("defaultValue", null)
                    )
                    
                    Log.d("TRACE", "   ✅ Created CustomParam: name=${param.name}, source=${param.source}, key=${param.key}")
                    param
                } catch (e: Exception) {
                    Log.e("TRACE", "   ❌ Error parsing: ${e.message}")
                    null
                }
            }
        } ?: emptyList()
    }

    private fun parseExclusionRules(jsonArray: JSONArray?): List<ExclusionRule> {
        return jsonArray?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                try {
                    val ruleJson = array.getJSONObject(index)
                    val operatorFieldName = if (ruleJson.has("ruleOperator")) "ruleOperator" else "operator"
                    val operator = if (ruleJson.has(operatorFieldName)) {
                        when (val operatorValue = ruleJson.get(operatorFieldName)) {
                            is Int -> ExclusionOperator.fromId(operatorValue)
                            is String -> {
                                val intValue = operatorValue.toIntOrNull()
                                if (intValue != null) {
                                    ExclusionOperator.fromId(intValue)
                                } else {
                                    try {
                                        ExclusionOperator.valueOf(operatorValue.uppercase())
                                    } catch (e: Exception) {
                                        ExclusionOperator.EQUALS
                                    }
                                }
                            }
                            else -> ExclusionOperator.EQUALS
                        }
                    } else {
                        ExclusionOperator.EQUALS
                    }

                    ExclusionRule(
                        name = ruleJson.optString("name", "rule_$index"),
                        source = ExclusionSource.valueOf(ruleJson.optString("source", "STORAGE").uppercase()),
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
}