// react-native/surveysdk-react-native/android/src/main/java/com/example/surveysdk/reactnative/SurveySDKModule.kt
package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import android.app.Activity
import com.example.surveysdk.UniversalSurveySDK

class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SurveySDK"

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            val context = reactApplicationContext
            UniversalSurveySDK.getInstance().initialize(context, apiKey)
            promise.resolve("SurveySDK initialized successfully")
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
            val props = properties?.toHashMap() ?: emptyMap<String, Any>()
            UniversalSurveySDK.getInstance().trackEvent(eventName, props)
            promise.resolve("Event tracked successfully")
        } catch (e: Exception) {
            promise.reject("TRACK_ERROR", e.message ?: "Failed to track event")
        }
    }
}