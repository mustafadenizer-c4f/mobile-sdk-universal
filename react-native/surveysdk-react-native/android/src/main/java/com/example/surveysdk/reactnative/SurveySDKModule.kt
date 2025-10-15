// In your SDK's react-native bridge folder
package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import android.app.Activity
import com.example.surveysdk.UniversalSurveySDK

class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SurveySDK"

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            val activity = currentActivity
            if (activity != null) {
                UniversalSurveySDK.getInstance().initializeWithActivity(activity, apiKey)
                promise.resolve("SurveySDK initialized successfully")
            } else {
                // Fallback to application context
                val context = reactApplicationContext
                UniversalSurveySDK.getInstance().initialize(context, apiKey)
                promise.resolve("SurveySDK initialized with application context")
            }
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message ?: "Initialization failed")
        }
    }

    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            val activity = currentActivity
            if (activity != null) {
                UniversalSurveySDK.getInstance().showSurvey(activity)
                promise.resolve("Survey shown successfully")
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            promise.reject("SHOW_ERROR", e.message ?: "Failed to show survey")
        }
    }

    @ReactMethod
    fun setUserProperty(key: String, value: String, promise: Promise) {
        try {
            UniversalSurveySDK.getInstance().setUserProperty(key, value)
            promise.resolve("User property set successfully")
        } catch (e: Exception) {
            promise.reject("PROPERTY_ERROR", e.message ?: "Failed to set user property")
        }
    }

    @ReactMethod
    fun trackEvent(eventName: String, properties: ReadableMap?, promise: Promise) {
        try {
            val props = convertReadableMapToMap(properties)
            UniversalSurveySDK.getInstance().trackEvent(eventName, props)
            promise.resolve("Event tracked successfully")
        } catch (e: Exception) {
            promise.reject("TRACK_ERROR", e.message ?: "Failed to track event")
        }
    }

    // Helper method to convert ReadableMap to Map
    private fun convertReadableMapToMap(readableMap: ReadableMap?): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (readableMap != null) {
            val iterator = readableMap.keySetIterator()
            while (iterator.hasNextKey()) {
                val key = iterator.nextKey()
                when (readableMap.getType(key)) {
                    ReadableType.Null -> map[key] = ""
                    ReadableType.Boolean -> map[key] = readableMap.getBoolean(key)
                    ReadableType.Number -> map[key] = readableMap.getDouble(key)
                    ReadableType.String -> map[key] = readableMap.getString(key) ?: ""
                    else -> map[key] = ""
                }
            }
        }
        return map
    }
}