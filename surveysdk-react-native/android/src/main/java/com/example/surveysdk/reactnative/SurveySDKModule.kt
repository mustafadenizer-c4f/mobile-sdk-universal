package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import android.util.Log
import com.example.surveysdk.SurveySDK

@ReactModule(name = "SurveySDK")
class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SurveySDK"

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Initializing core SurveySDK...")
            
            val activity = currentActivity
            if (activity != null) {
                // Initialize core SDK directly
                SurveySDK.initialize(activity.applicationContext, apiKey)
                Log.d("SurveySDK", "RN: Core SDK initialized successfully")
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Initialization failed", e)
            promise.reject("INIT_ERROR", "Initialization failed: ${e.message}")
        }
    }

    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Showing survey via core SDK...")
            
            val activity = currentActivity
            if (activity != null) {
                // Use core SDK directly
                val surveySDK = SurveySDK.getInstance()
                surveySDK.showSurvey(activity)
                Log.d("SurveySDK", "RN: Survey shown via core SDK")
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Show survey failed", e)
            promise.reject("SHOW_ERROR", "Failed to show survey: ${e.message}")
        }
    }

    @ReactMethod
    fun setUserProperty(key: String, value: String, promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Setting user property: $key = $value")
            
            // Store in SharedPreferences directly for React Native
            val activity = currentActivity
            if (activity != null) {
                activity.getSharedPreferences("survey_sdk_data", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString(key, value)
                    .apply()
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Set user property failed", e)
            promise.reject("PROPERTY_ERROR", "Failed to set user property: ${e.message}")
        }
    }

    @ReactMethod
    fun trackEvent(eventName: String, properties: ReadableMap?, promise: Promise) {
        try {
            val props = properties?.toHashMap() ?: emptyMap<String, Any>()
            Log.d("SurveySDK", "RN: Tracking event: $eventName with properties: $props")
            
            // Log event for React Native (can be extended to send to analytics)
            android.util.Log.d("SurveySDK", "Event tracked: $eventName, Properties: $props")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Track event failed", e)
            promise.reject("TRACK_ERROR", "Failed to track event: ${e.message}")
        }
    }

    @ReactMethod
    fun isUserExcluded(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isExcluded = surveySDK.isUserExcluded()
            Log.d("SurveySDK", "RN: User excluded check: $isExcluded")
            promise.resolve(isExcluded)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: User excluded check failed", e)
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion: ${e.message}")
        }
    }

    @ReactMethod
    fun getDebugStatus(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val debugStatus = surveySDK.debugSurveyStatus()
            Log.d("SurveySDK", "RN: Debug status requested")
            promise.resolve(debugStatus)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Debug status failed", e)
            promise.reject("DEBUG_ERROR", "Failed to get debug status: ${e.message}")
        }
    }

    @ReactMethod
    fun autoSetup(promise: Promise) {
        try {
            val activity = currentActivity
            if (activity != null) {
                val surveySDK = SurveySDK.getInstance()
                surveySDK.autoSetup(activity)
                Log.d("SurveySDK", "RN: Auto setup completed")
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available for auto setup")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Auto setup failed", e)
            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
        }
    }

    @ReactMethod
    fun getSessionStats(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val sessionStats = surveySDK.getSessionStats()
            val writableMap = Arguments.createMap()
            sessionStats.forEach { (key, value) ->
                writableMap.putString(key, value)
            }
            promise.resolve(writableMap)
        } catch (e: Exception) {
            promise.reject("SESSION_ERROR", "Failed to get session stats: ${e.message}")
        }
    }

    @ReactMethod
    fun setSessionData(key: String, value: String, promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.setSessionData(key, value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SESSION_ERROR", "Failed to set session data: ${e.message}")
        }
    }

    @ReactMethod
    fun resetSessionData(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.resetSessionData()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SESSION_ERROR", "Failed to reset session data: ${e.message}")
        }
    }
}