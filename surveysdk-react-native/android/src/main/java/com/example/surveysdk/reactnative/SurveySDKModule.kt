package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.surveysdk.SurveySDK

@ReactModule(name = "SurveySDK")
class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SurveySDK"

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Initializing core SurveySDK...")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                // Updated to match new initialization that returns SurveySDK instance
                SurveySDK.initialize(activity.applicationContext, apiKey)
                Log.d("SurveySDK", "RN: Core SDK initialized successfully")
                promise.resolve(true)
            } else {
                Log.e("SurveySDK", "RN: No current activity available")
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
            
            val activity = getCurrentActivity()
            if (activity != null) {
                val surveySDK = SurveySDK.getInstance()
                surveySDK.showSurvey(activity)
                Log.d("SurveySDK", "RN: Survey shown via core SDK")
                promise.resolve(true)
            } else {
                Log.e("SurveySDK", "RN: No current activity available")
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Show survey failed", e)
            promise.reject("SHOW_ERROR", "Failed to show survey: ${e.message}")
        }
    }

    @ReactMethod
    fun showSurveyById(surveyId: String, promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Showing specific survey: $surveyId")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                val surveySDK = SurveySDK.getInstance()
                surveySDK.showSurveyById(activity, surveyId)
                Log.d("SurveySDK", "RN: Specific survey shown: $surveyId")
                promise.resolve(true)
            } else {
                Log.e("SurveySDK", "RN: No current activity available")
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Show specific survey failed", e)
            promise.reject("SHOW_ERROR", "Failed to show survey $surveyId: ${e.message}")
        }
    }

    @ReactMethod
    fun setUserProperty(key: String, value: String, promise: Promise) {
        try {
            Log.d("SurveySDK", "RN: Setting user property: $key = $value")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                activity.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
                    .edit()
                    .putString(key, value)
                    .apply()
                Log.d("SurveySDK", "RN: User property set successfully")
                promise.resolve(true)
            } else {
                Log.e("SurveySDK", "RN: No current activity available")
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
            
            Log.d("SurveySDK", "Event tracked: $eventName, Properties: $props")
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
    fun isUserExcludedForSurvey(surveyId: String, promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isExcluded = surveySDK.isUserExcluded(surveyId)
            Log.d("SurveySDK", "RN: User excluded check for $surveyId: $isExcluded")
            promise.resolve(isExcluded)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: User excluded check failed for $surveyId", e)
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion for survey $surveyId: ${e.message}")
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
            val activity = getCurrentActivity()
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
    fun setSessionData(key: String, value: String, promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.setSessionData(key, value)
            Log.d("SurveySDK", "RN: Session data set: $key = $value")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Set session data failed", e)
            promise.reject("SESSION_ERROR", "Failed to set session data: ${e.message}")
        }
    }

    @ReactMethod
    fun resetSessionData(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.resetSessionData()
            Log.d("SurveySDK", "RN: Session data reset")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Reset session data failed", e)
            promise.reject("SESSION_ERROR", "Failed to reset session data: ${e.message}")
        }
    }

    @ReactMethod
    fun resetTriggers(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.resetTriggers()
            Log.d("SurveySDK", "RN: Triggers reset")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Reset triggers failed", e)
            promise.reject("TRIGGER_ERROR", "Failed to reset triggers: ${e.message}")
        }
    }

    @ReactMethod
    fun getSurveyIds(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val surveyIds = surveySDK.getSurveyIds()
            
            val writableArray = Arguments.createArray()
            surveyIds.forEach { surveyId ->
                writableArray.pushString(surveyId)
            }
            
            Log.d("SurveySDK", "RN: Retrieved ${writableArray.size()} survey IDs")
            promise.resolve(writableArray)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to get survey IDs", e)
            promise.reject("CONFIG_ERROR", "Failed to get survey IDs: ${e.message}")
        }
    }

    @ReactMethod
    fun isConfigurationLoaded(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isLoaded = surveySDK.isConfigurationLoaded()
            Log.d("SurveySDK", "RN: Configuration loaded: $isLoaded")
            promise.resolve(isLoaded)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to check config status", e)
            promise.reject("CONFIG_ERROR", "Failed to check configuration status: ${e.message}")
        }
    }

    // ===== NEW METHODS FOR MULTI-SURVEY SUPPORT =====

    @ReactMethod
    fun getQueueStatus(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val status = surveySDK.getQueueStatus()
            Log.d("SurveySDK", "RN: Queue status requested")
            promise.resolve(status)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to get queue status", e)
            promise.reject("QUEUE_ERROR", "Failed to get queue status: ${e.message}")
        }
    }

    @ReactMethod
    fun clearSurveyQueue(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.clearSurveyQueue()
            Log.d("SurveySDK", "RN: Survey queue cleared")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to clear survey queue", e)
            promise.reject("QUEUE_ERROR", "Failed to clear survey queue: ${e.message}")
        }
    }

    @ReactMethod
    fun isShowingSurvey(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isShowing = surveySDK.isShowingSurvey()
            Log.d("SurveySDK", "RN: Is showing survey: $isShowing")
            promise.resolve(isShowing)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to check survey status", e)
            promise.reject("SURVEY_ERROR", "Failed to check if survey is showing: ${e.message}")
        }
    }

    @ReactMethod
    fun getSetupStatus(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val status = surveySDK.getSetupStatus()
            Log.d("SurveySDK", "RN: Setup status requested")
            promise.resolve(status)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to get setup status", e)
            promise.reject("STATUS_ERROR", "Failed to get setup status: ${e.message}")
        }
    }

    @ReactMethod
    fun isSDKEnabled(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isEnabled = surveySDK.isSDKEnabled()
            Log.d("SurveySDK", "RN: SDK enabled: $isEnabled")
            promise.resolve(isEnabled)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to check SDK status", e)
            promise.reject("STATUS_ERROR", "Failed to check if SDK is enabled: ${e.message}")
        }
    }

    @ReactMethod
    fun isReady(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isReady = surveySDK.isReady()
            Log.d("SurveySDK", "RN: SDK ready: $isReady")
            promise.resolve(isReady)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to check SDK readiness", e)
            promise.reject("STATUS_ERROR", "Failed to check if SDK is ready: ${e.message}")
        }
    }

    @ReactMethod
    fun fetchConfiguration(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.fetchConfiguration()
            Log.d("SurveySDK", "RN: Configuration fetch initiated")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to fetch configuration", e)
            promise.reject("CONFIG_ERROR", "Failed to fetch configuration: ${e.message}")
        }
    }

    @ReactMethod
    fun getConfigForDebug(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val configDebug = surveySDK.getConfigForDebug()
            Log.d("SurveySDK", "RN: Config debug requested")
            promise.resolve(configDebug)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to get config debug", e)
            promise.reject("CONFIG_ERROR", "Failed to get config debug info: ${e.message}")
        }
    }

    @ReactMethod
    fun cleanup(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.cleanup()
            Log.d("SurveySDK", "RN: SDK cleanup completed")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK", "RN: Failed to cleanup SDK", e)
            promise.reject("CLEANUP_ERROR", "Failed to cleanup SDK: ${e.message}")
        }
    }
}