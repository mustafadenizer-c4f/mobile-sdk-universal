package com.example.surveysdk

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object ExclusionRuleEvaluator {

    // Session storage for in-memory data during app lifetime
    private val sessionStorage = mutableMapOf<String, String>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Add session data (for SESSION source)
    fun setSessionData(key: String, value: String) {
        sessionStorage[key] = value
        Log.d("SurveySDK", "Session data set: $key = $value")
    }

    fun clearSessionData() {
        sessionStorage.clear()
        Log.d("SurveySDK", "Session data cleared")
    }

    fun shouldExcludeSurvey(context: Context, rules: List<ExclusionRule>): Boolean {
        if (rules.isEmpty()) return false

        Log.d("SurveySDK", "=== CHECKING ${rules.size} EXCLUSION RULES ===")

        for (rule in rules) {
            if (evaluateRule(context, rule)) {
                Log.d("SurveySDK", "🚫 User excluded by rule: ${rule.name}")
                return true
            }
        }
        return false
    }

    private fun evaluateRule(context: Context, rule: ExclusionRule): Boolean {
        return try {
            val actualValue = getValueFromSource(context, rule)
            val matchValue = if (rule.caseSensitive) rule.matchValue else rule.matchValue.toLowerCase()
            val compareValue = if (rule.caseSensitive) actualValue else actualValue.toLowerCase()

            Log.d("SurveySDK", "🔍 Rule '${rule.name}': '$actualValue' ${rule.operator} '$matchValue'")

            val result = when (rule.operator) {
                ExclusionOperator.EQUALS -> compareValue == matchValue
                ExclusionOperator.NOT_EQUALS -> compareValue != matchValue
                ExclusionOperator.CONTAINS -> compareValue.contains(matchValue)
                ExclusionOperator.NOT_CONTAINS -> !compareValue.contains(matchValue)
                ExclusionOperator.STARTS_WITH -> compareValue.startsWith(matchValue)
                ExclusionOperator.ENDS_WITH -> compareValue.endsWith(matchValue)
                ExclusionOperator.GREATER_THAN -> compareNumericValues(actualValue, matchValue) { a, b -> a > b }
                ExclusionOperator.LESS_THAN -> compareNumericValues(actualValue, matchValue) { a, b -> a < b }
                ExclusionOperator.GREATER_OR_EQUAL -> compareNumericValues(actualValue, matchValue) { a, b -> a >= b }
                ExclusionOperator.LESS_OR_EQUAL -> compareNumericValues(actualValue, matchValue) { a, b -> a <= b }
                ExclusionOperator.IN -> isValueInList(compareValue, matchValue)
                ExclusionOperator.NOT_IN -> !isValueInList(compareValue, matchValue)
            }

            Log.d("SurveySDK", "📊 Rule '${rule.name}' result: $result")
            result

        } catch (e: Exception) {
            Log.e("SurveySDK", "Error evaluating exclusion rule '${rule.name}': ${e.message}")
            false
        }
    }

    // Check if value is in comma-separated list
    private fun isValueInList(value: String, listString: String): Boolean {
        return try {
            val items = listString.split(",").map { it.trim() }
            items.any { it.equals(value, ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }

    private fun getValueFromSource(context: Context, rule: ExclusionRule): String {
        return when (rule.source) {
            ExclusionSource.STORAGE -> {
                StorageUtils.findSpecificData(context, rule.key ?: rule.name) ?: ""
            }
            ExclusionSource.DEVICE -> {
                getDeviceValue(context, rule.key ?: rule.name) ?: ""
            }
            ExclusionSource.URL -> {
                getUrlParameter(context, rule.key ?: rule.name) ?: ""
            }
            ExclusionSource.TIMESTAMP -> {
                getTimestampValue(rule.key ?: "millis")
            }
            ExclusionSource.SESSION -> {
                sessionStorage[rule.key ?: rule.name] ?: ""
            }
            ExclusionSource.INSTALL_TIME -> {
                getInstallTimeValue(context, rule.key ?: "days")
            }
            ExclusionSource.APP_USAGE -> {
                getAppUsageValue(context, rule.key ?: "sessions") ?: ""
            }
        }
    }

    // ===== DEVICE VALUES =====
    private fun getDeviceValue(context: Context, key: String): String? {
        val deviceInfo = DeviceUtils.getDeviceInfo(context)
        return deviceInfo[key] ?: run {
            // Additional device-specific values
            when (key.toLowerCase()) {
                "battery_level" -> getBatteryLevel(context)
                "is_charging" -> isDeviceCharging(context).toString()
                "is_wifi" -> isWifiConnected(context).toString()
                "is_roaming" -> isNetworkRoaming(context).toString()
                "charge_type" -> getBatteryChargeType(context)
                "latitude" -> getLocationValue(context, "latitude")
                "longitude" -> getLocationValue(context, "longitude")
                "location_accuracy" -> getLocationValue(context, "location_accuracy")
                else -> null
            }
        }
    }

    private fun getBatteryLevel(context: Context): String {
        return try {
            val batteryInfo = DeviceUtils.getBatteryInfo(context)
            batteryInfo["battery_level"] ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getBatteryChargeType(context: Context): String {
        return try {
            val batteryInfo = DeviceUtils.getBatteryInfo(context)
            batteryInfo["charge_type"] ?: "none"
        } catch (e: Exception) {
            "none"
        }
    }

    private fun isDeviceCharging(context: Context): Boolean {
        return try {
            val batteryInfo = DeviceUtils.getBatteryInfo(context)
            batteryInfo["is_charging"] == "true"
        } catch (e: Exception) {
            false
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.getNetworkInfo(android.net.ConnectivityManager.TYPE_WIFI)
            networkInfo?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isNetworkRoaming(context: Context): Boolean {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            telephonyManager.isNetworkRoaming
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocationValue(context: Context, key: String): String {
        return try {
            val locationData = DeviceUtils.getLocationData(context)
            locationData[key] ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // ===== URL PARAMETERS =====
    private fun getUrlParameter(context: Context, key: String): String? {
        return try {
            // Check if context is an Activity and get intent data
            if (context is android.app.Activity) {
                // Check deep link URL parameters
                context.intent?.data?.getQueryParameter(key)?.let { return it }

                // Check intent extras
                context.intent?.extras?.getString(key)?.let { return it }

                // Check intent data string
                context.intent?.getStringExtra(key)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ===== TIMESTAMP VALUES =====
    private fun getTimestampValue(format: String): String {
        return when (format) {
            "millis" -> System.currentTimeMillis().toString()
            "seconds" -> (System.currentTimeMillis() / 1000).toString()
            "iso" -> dateFormatter.format(Date(System.currentTimeMillis()))
            "hour" -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toString()
            "day_of_week" -> Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
            "day_of_month" -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
            "month" -> (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
            else -> System.currentTimeMillis().toString()
        }
    }

    // ===== INSTALL TIME VALUES =====
    private fun getInstallTimeValue(context: Context, unit: String): String {
        return try {
            val installInfo = AppUsageUtils.getAppInstallInfo(context)
            when (unit) {
                "days" -> installInfo["days_since_install"] ?: "0"
                "hours" -> {
                    val days = installInfo["days_since_install"]?.toLongOrNull() ?: 0L
                    (days * 24).toString()
                }
                "minutes" -> {
                    val days = installInfo["days_since_install"]?.toLongOrNull() ?: 0L
                    (days * 24 * 60).toString()
                }
                "millis" -> installInfo["app_install_time"] ?: "0"
                else -> installInfo["days_since_install"] ?: "0"
            }
        } catch (e: Exception) {
            "0"
        }
    }

    // ===== APP USAGE VALUES =====
    private fun getAppUsageValue(context: Context, metric: String): String? {
        return try {
            when (metric) {
                "sessions" -> {
                    val sessionData = AppUsageUtils.getEnhancedSessionData(context)
                    sessionData["session_count"] ?: "0"
                }
                "last_session" -> {
                    val sessionData = AppUsageUtils.getEnhancedSessionData(context)
                    sessionData["time_since_last_session_minutes"] ?: "0"
                }
                "screen_views" -> {
                    val sessionData = AppUsageUtils.getEnhancedSessionData(context)
                    sessionData["screen_view_count"] ?: "0"
                }
                "app_usage_time" -> {
                    val usageData = AppUsageUtils.getAppUsageData(context)
                    usageData["app_usage_time_minutes"] ?: "0"
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ===== NUMERIC COMPARISON =====
    private fun compareNumericValues(
        actualValue: String,
        matchValue: String,
        comparator: (Double, Double) -> Boolean
    ): Boolean {
        return try {
            val actualNum = actualValue.toDoubleOrNull()
            val matchNum = matchValue.toDoubleOrNull()
            actualNum != null && matchNum != null && comparator(actualNum, matchNum)
        } catch (e: Exception) {
            false
        }
    }

    // ===== SESSION TRACKING METHODS =====
    fun trackSessionStart(context: Context) {
        val prefs = context.getSharedPreferences("survey_sdk_session", Context.MODE_PRIVATE)
        val sessionCount = prefs.getInt("session_count", 0) + 1
        prefs.edit()
            .putInt("session_count", sessionCount)
            .putLong("last_session_time", System.currentTimeMillis())
            .apply()

        Log.d("SurveySDK", "Session tracked: #$sessionCount")
    }

    fun trackScreenView(context: Context) {
        val prefs = context.getSharedPreferences("survey_sdk_session", Context.MODE_PRIVATE)
        val screenViews = prefs.getInt("screen_view_count", 0) + 1
        prefs.edit().putInt("screen_view_count", screenViews).apply()

        Log.d("SurveySDK", "Screen view tracked: #$screenViews")
    }

    fun resetSessionData(context: Context) {
        val prefs = context.getSharedPreferences("survey_sdk_session", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        sessionStorage.clear()

        Log.d("SurveySDK", "Session data reset")
    }
}