package com.example.surveysdk.reactnative

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.example.surveysdk.UniversalSurveySDK

class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SurveySDK"

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            val activity = currentActivity
            if (activity != null) {
                UniversalSurveySDK.getInstance().initializeWithActivity(activity, apiKey)
                promise.resolve(null)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            val activity = currentActivity
            if (activity != null) {
                UniversalSurveySDK.getInstance().showSurvey(activity)
                promise.resolve(null)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            promise.reject("SHOW_ERROR", e.message)
        }
    }

    @ReactMethod
    fun setUserProperty(key: String, value: String, promise: Promise) {
        try {
            UniversalSurveySDK.getInstance().setUserProperty(key, value)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("PROPERTY_ERROR", e.message)
        }
    }

    @ReactMethod
    fun trackEvent(eventName: String, properties: ReadableMap?, promise: Promise) {
        try {
            val props = properties?.toHashMap() ?: emptyMap<String, Any>()
            UniversalSurveySDK.getInstance().trackEvent(eventName, props)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("TRACK_ERROR", e.message)
        }
    }
}