package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import android.util.Log
import android.content.Context  // ← ADD THIS
import com.example.surveysdk.SurveySDK  // ← ADD THIS (use main SDK directly)
import com.example.surveysdk.UniversalSurveySDK  // ← ADD THIS

@ReactModule(name = "SurveySDK")
class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SurveySDK"

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Initializing with API key: ${apiKey.take(8)}...")
            
            val activity = currentActivity
            if (activity != null) {
                // Use reflection or direct SDK call to avoid compile-time dependency issues
                initializeSDK(activity, apiKey)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Initialization failed", e)
            promise.reject("INIT_ERROR", "Initialization failed: ${e.message}")
        }
    }

    private fun initializeSDK(activity: Activity, apiKey: String) {
        try {
            // Use reflection to avoid direct dependency
            val sdkClass = Class.forName("com.example.surveysdk.SurveySDK")
            val initializeMethod = sdkClass.getMethod("initialize", Context::class.java, String::class.java)
            initializeMethod.invoke(null, activity.applicationContext, apiKey)
        } catch (e: Exception) {
            Log.e("SurveySDK", "Reflection initialization failed", e)
            // Fallback to direct call (will fail at runtime if main SDK not included)
            com.example.surveysdk.SurveySDK.initialize(activity.applicationContext, apiKey)
        }
    }


    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Showing survey...")
            
            val activity = currentActivity
            if (activity != null) {
                UniversalSurveySDK.getInstance().showSurvey(activity)
                Log.d("SurveySDK", "RN: Survey shown successfully")
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
            
            UniversalSurveySDK.getInstance().setUserProperty(key, value)
            promise.resolve(true)
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
            
            UniversalSurveySDK.getInstance().trackEvent(eventName, props)
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Track event failed", e)
            promise.reject("TRACK_ERROR", "Failed to track event: ${e.message}")
        }
    }

    @ReactMethod
    fun isUserExcluded(promise: Promise) {
        try {
            val platform = UniversalSurveySDK.getInstance().getPlatform()
            val isExcluded = if (platform is com.example.surveysdk.android.AndroidSurveySDK) {
                platform.isUserExcluded()
            } else {
                false
            }
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
            val platform = UniversalSurveySDK.getInstance().getPlatform()
            val debugStatus = if (platform is com.example.surveysdk.android.AndroidSurveySDK) {
                platform.getDebugStatus()
            } else {
                "SDK not properly initialized"
            }
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
                Log.d("SurveySDK", "RN: Auto setup started")
                UniversalSurveySDK.getInstance().autoSetup(activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available for auto setup")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Auto setup failed", e)
            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
        }
    }
}