package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import com.example.surveysdk.SurveySDK

@ReactModule(name = SurveySDKModule.NAME)
class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "SurveySDK"
    }

    override fun getName(): String = NAME

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            val context = reactApplicationContext
            SurveySDK.initialize(context, apiKey)
            promise.resolve("SurveySDK initialized successfully with key: $apiKey")
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message ?: "Initialization failed")
        }
    }

    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            val activity = currentActivity
            if (activity != null) {
                val sdk = SurveySDK.getInstance()
                sdk.showSurvey(activity)
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
            // Store in shared preferences (as shown in AndroidSurveySDK)
            val prefs = reactApplicationContext.getSharedPreferences("survey_sdk_data", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
            promise.resolve("User property '$key' set to '$value'")
        } catch (e: Exception) {
            promise.reject("PROPERTY_ERROR", e.message ?: "Failed to set user property")
        }
    }

    @ReactMethod
    fun trackEvent(eventName: String, properties: ReadableMap?, promise: Promise) {
        try {
            val props = properties?.toHashMap() ?: emptyMap<String, Any>()
            android.util.Log.d("SurveySDK", "Event: $eventName, Properties: $props")
            promise.resolve("Event '$eventName' tracked successfully")
        } catch (e: Exception) {
            promise.reject("TRACK_ERROR", e.message ?: "Failed to track event")
        }
    }

    @ReactMethod
    fun isUserExcluded(promise: Promise) {
        try {
            val sdk = SurveySDK.getInstance()
            val excluded = sdk.isUserExcluded()
            promise.resolve(excluded)
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", e.message ?: "Failed to check exclusion status")
        }
    }

    @ReactMethod
    fun debugSurveyStatus(promise: Promise) {
        try {
            val sdk = SurveySDK.getInstance()
            val status = sdk.debugSurveyStatus()
            promise.resolve(status)
        } catch (e: Exception) {
            promise.reject("DEBUG_ERROR", e.message ?: "Failed to get debug status")
        }
    }
}