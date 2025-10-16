package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import android.util.Log

@ReactModule(name = "SurveySDK")
class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SurveySDK"

    private var isInitialized = false

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            Log.d("SurveySDK", "React Native Bridge initialized with API key")
            isInitialized = true
            promise.resolve("SurveySDK React Native bridge initialized successfully")
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message ?: "Initialization failed")
        }
    }

    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            if (!isInitialized) {
                promise.reject("NOT_INITIALIZED", "SDK not initialized. Call initialize() first.")
                return
            }

            val activity = currentActivity
            if (activity != null) {
                Log.d("SurveySDK", "Survey would be shown in: ${activity.javaClass.simpleName}")
                promise.resolve("Survey ready - Main SDK integration pending")
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
            if (!isInitialized) {
                promise.reject("NOT_INITIALIZED", "SDK not initialized")
                return
            }
            
            Log.d("SurveySDK", "User property set: $key = $value")
            
            // Store in shared preferences
            val prefs = reactApplicationContext.getSharedPreferences("survey_sdk_data", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
            
            promise.resolve("User property '$key' set successfully")
        } catch (e: Exception) {
            promise.reject("PROPERTY_ERROR", e.message ?: "Failed to set user property")
        }
    }

    @ReactMethod
    fun trackEvent(eventName: String, properties: ReadableMap?, promise: Promise) {
        try {
            if (!isInitialized) {
                promise.reject("NOT_INITIALIZED", "SDK not initialized")
                return
            }
            
            val props = properties?.toHashMap() ?: emptyMap<String, Any>()
            Log.d("SurveySDK", "Event tracked: $eventName - $props")
            promise.resolve("Event '$eventName' tracked successfully")
        } catch (e: Exception) {
            promise.reject("TRACK_ERROR", e.message ?: "Failed to track event")
        }
    }

    @ReactMethod
    fun isUserExcluded(promise: Promise) {
        try {
            promise.resolve(false) // Simple implementation for now
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", e.message ?: "Failed to check exclusion")
        }
    }

    @ReactMethod
    fun getDebugStatus(promise: Promise) {
        try {
            val status = """
                SurveySDK React Native Bridge
                Initialized: $isInitialized
                Package: com.example.surveysdk.reactnative
                Status: Self-contained build
            """.trimIndent()
            promise.resolve(status)
        } catch (e: Exception) {
            promise.reject("DEBUG_ERROR", e.message ?: "Failed to get debug status")
        }
    }
}