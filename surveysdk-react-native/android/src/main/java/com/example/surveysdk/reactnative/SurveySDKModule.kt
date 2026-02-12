package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.example.surveysdk.SurveySDK
import com.example.surveysdk.UniversalSurveySDK
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

@ReactModule(name = "SurveySDK")
class SurveySDKModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    private var currentActivity: WeakReference<Activity>? = null
    private var isAutoSetupComplete = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var registeredReactButtons = mutableSetOf<String>()
    private var scrollDetectionEnabled = false
    
    
    init {
    Log.d("SurveySDK_RN_CRITICAL", "=== MODULE LOADED ===")
    Log.d("SurveySDK_RN_CRITICAL", "Class: ${this::class.java.name}")
        
    currentActivity = WeakReference(reactContext.currentActivity)

    // Print ALL methods this module exposes
    val methods = this::class.java.methods
    methods.filter { it.declaringClass == this::class.java }
        .filter { it.name == "initialize" || it.name.contains("initialize") }
        .forEach {
            Log.d("SurveySDK_RN_CRITICAL", "📌 EXPOSED METHOD: ${it.name}")
            Log.d("SurveySDK_RN_CRITICAL", "   Parameters: ${it.parameterTypes.joinToString { it.simpleName }}")
        }
}

    override fun getName(): String = "SurveySDK"

    // ====================================================================
    // ✅ FIXED INITIALIZATION - NO REFLECTION, NO STORAGEUTILS
    // ====================================================================

    @ReactMethod
    fun initialize(apiKey: String, params: ReadableArray?, promise: Promise) {
        Log.d("SurveySDK_RN", "📱 RN Bridge")
        try {
            Log.d("SurveySDK_RN", "📱 RN Bridge: initialize() called")
            
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            // ✅ FIX: Use Application, not Context
            val application = activity.application
            
            if (params != null && params.size() > 0) {
                val anyParams = convertToAnyArray(params)
                
                // ✅ Call UniversalSurveySDK with Application
                UniversalSurveySDK.getInstance().initialize(application, apiKey, *anyParams)
                
                Log.d("SurveySDK_RN", "✅ SDK initialized with ${anyParams.size} params")
            } else {
                // Simple initialization
                UniversalSurveySDK.getInstance().initialize(application, apiKey)
                Log.d("SurveySDK_RN", "✅ SDK initialized without parameters")
            }
            
            promise.resolve(true)
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Initialization failed", e)
            promise.reject("INIT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        Log.d("SurveySDK_RN", "📱 RN Bridge: initialize(apiKey) - redirecting")
        initialize(apiKey, null, promise)  // Call with null params
    }

    private fun convertToAnyArray(params: ReadableArray): Array<Any> {
        val result = mutableListOf<Any>()
        
        for (i in 0 until params.size()) {
            when (params.getType(i)) {
                ReadableType.String -> {
                    params.getString(i)?.let { result.add(it) }
                }
                ReadableType.Map -> {
                    val map = params.getMap(i)
                    map?.keySetIterator()?.let { iterator ->
                        while (iterator.hasNextKey()) {
                            val key = iterator.nextKey()
                            val value = map.getString(key)
                            if (key != null && value != null) {
                                result.add(Pair(key, value))
                            }
                        }
                    }
                }
                else -> {
                    Log.w("SurveySDK_RN", "⚠️ Skipping unsupported type: ${params.getType(i)}")
                }
            }
        }
        
        return result.toTypedArray()
    }

    @ReactMethod
    fun autoSetup(promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            activity.runOnUiThread {
                SurveySDK.getInstance().autoSetup(activity)
                promise.resolve(true)
            }
        } catch (e: Exception) {
            promise.reject("SETUP_ERROR", e.message)
        }
    }

    // ====================================================================
    // AUTO SETUP
    // ====================================================================
    @ReactMethod
    fun autoSetupReact(promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "🔄 RN: Starting autoSetup...")
            
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            activity.runOnUiThread {
                try {
                    activity.window?.decorView?.postDelayed({
                        try {
                            val surveySDK = SurveySDK.getInstance()
                            setupReactNativeDetection(activity)
                            surveySDK.autoSetup(activity)
                            isAutoSetupComplete = true
                            Log.d("SurveySDK_RN", "✅ React Native auto setup completed")
                            promise.resolve(true)
                        } catch (e: Exception) {
                            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
                        }
                    }, 500)
                } catch (e: Exception) {
                    promise.reject("SETUP_ERROR", "UI thread setup failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
        }
    }

    // ====================================================================
    // REACT NATIVE DETECTION
    // ====================================================================
    private fun setupReactNativeDetection(activity: Activity) {
        try {
            Log.d("SurveySDK_RN", "🔍 Setting up React Native detection...")
            setupReactNativeScrollDetection(activity)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ React Native detection setup failed", e)
        }
    }

    private fun setupReactNativeScrollDetection(activity: Activity) {
        try {
            Log.d("SurveySDK_RN", "📜 Setting up React Native scroll detection...")
            val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
            if (rootView != null) {
                findAndSetupScrollViews(rootView, activity)
                scrollDetectionEnabled = true
            }
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Scroll detection setup failed", e)
        }
    }

    private fun findAndSetupScrollViews(view: View, activity: Activity) {
        try {
            if (view::class.java.name.contains("ScrollView") ||
                view::class.java.name.contains("FlatList") ||
                view::class.java.name.contains("RecyclerView")) {
                
                Log.d("SurveySDK_RN", "✅ Found scrollable view: ${view.javaClass.simpleName}")
                setupScrollListener(view, activity)
            }
            
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findAndSetupScrollViews(view.getChildAt(i), activity)
                }
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }

    private fun setupScrollListener(scrollView: View, activity: Activity) {
        try {
            var lastTriggerTime = 0L
            val SCROLL_COOLDOWN_MS = 3000L
            
            scrollView.viewTreeObserver.addOnScrollChangedListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTriggerTime < SCROLL_COOLDOWN_MS) {
                    return@addOnScrollChangedListener
                }

                val scrollY = scrollView.scrollY
                val viewHeight = scrollView.height
                
                val contentHeight = if (scrollView is ViewGroup && scrollView.childCount > 0) {
                    scrollView.getChildAt(0).height
                } else {
                    scrollView.height
                }

                val scrollPercentage = if (contentHeight > viewHeight) {
                    ((scrollY.toFloat() + viewHeight) / contentHeight) * 100
                } else {
                    0f
                }

                if (scrollPercentage >= 90) {
                    Log.d("SurveySDK_RN", "🎯 Scroll threshold reached ($scrollPercentage%)")
                    lastTriggerTime = currentTime
                    // ✅ FIX: Call the SDK directly, not the ReactMethod
                    SurveySDK.getInstance().triggerScrollManual(activity, 1000)
                }
            }
            
            Log.d("SurveySDK_RN", "✅ Scroll listener setup successful")
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to setup scroll listener", e)
        }
    }

    // ====================================================================
    // TRIGGER METHODS
    // ====================================================================
    @ReactMethod
    fun triggerButtonSurvey(buttonId: String, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "🎯 RN Bridge: Manual trigger for button: $buttonId")
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.getInstance().triggerButtonByStringId(buttonId, activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to trigger survey: ${e.message}")
        }
    }

    @ReactMethod
    fun triggerScrollSurvey(promise: Promise) {  // ← Parameter is Promise, NOT Activity
        try {
            Log.d("SurveySDK_RN", "📜 RN Bridge: Manual scroll trigger")
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.getInstance().triggerScrollManual(activity, 1000)  // ← Pass activity here
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to trigger scroll survey: ${e.message}")
        }
    }

    @ReactMethod
    fun triggerNavigationSurvey(screenName: String, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "📍 RN Bridge: Manual navigation trigger: $screenName")
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.getInstance().triggerByNavigation(screenName, activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to trigger navigation survey: ${e.message}")
        }
    }

    @ReactMethod
    fun enableNavigationSafety(promise: Promise) {
        try {
            SurveySDK.getInstance().enableNavigationSafety()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SAFETY_ERROR", "Failed to enable navigation safety")
        }
    }

    @ReactMethod
    fun autoSetupSafe(promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.getInstance().autoSetupSafe(activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("SETUP_ERROR", "Failed to safe auto setup")
        }
    }

    // ====================================================================
    // SURVEY DISPLAY METHODS
    // ====================================================================
    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.getInstance().showSurvey(activity)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("SHOW_ERROR", "Failed to show survey: ${e.message}")
        }
    }

    @ReactMethod
    fun showSurveyById(surveyId: String, promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.getInstance().showSurveyById(activity, surveyId)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            promise.reject("SHOW_ERROR", "Failed to show survey $surveyId: ${e.message}")
        }
    }

    // ====================================================================
    // USER PROPERTIES
    // ====================================================================
    @ReactMethod
    fun setUserProperty(key: String, value: String, promise: Promise) {
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                activity.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
                    .edit().putString(key, value).apply()
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            promise.reject("PROPERTY_ERROR", "Failed to set user property: ${e.message}")
        }
    }

    @ReactMethod
    fun setSessionData(key: String, value: String, promise: Promise) {
        try {
            SurveySDK.getInstance().setSessionData(key, value)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SESSION_ERROR", "Failed to set session data: ${e.message}")
        }
    }

    @ReactMethod
    fun resetSessionData(promise: Promise) {
        try {
            SurveySDK.getInstance().resetSessionData()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SESSION_ERROR", "Failed to reset session data: ${e.message}")
        }
    }

    // ====================================================================
    // STATUS & DEBUG METHODS
    // ====================================================================
    @ReactMethod
    fun isUserExcluded(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().isUserExcluded())
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion: ${e.message}")
        }
    }

    @ReactMethod
    fun isUserExcludedForSurvey(surveyId: String, promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().isUserExcluded(surveyId))
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion for survey $surveyId: ${e.message}")
        }
    }

    @ReactMethod
    fun getDebugStatus(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().debugSurveyStatus())
        } catch (e: Exception) {
            promise.reject("DEBUG_ERROR", "Failed to get debug status: ${e.message}")
        }
    }

    @ReactMethod
    fun getSurveyIds(promise: Promise) {
        try {
            val surveyIds = SurveySDK.getInstance().getSurveyIds()
            val writableArray = Arguments.createArray()
            surveyIds.forEach { writableArray.pushString(it) }
            promise.resolve(writableArray)
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to get survey IDs: ${e.message}")
        }
    }

    @ReactMethod
    fun isConfigurationLoaded(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().isConfigurationLoaded())
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to check configuration status: ${e.message}")
        }
    }

    @ReactMethod
    fun getQueueStatus(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().getQueueStatus())
        } catch (e: Exception) {
            promise.reject("QUEUE_ERROR", "Failed to get queue status: ${e.message}")
        }
    }

    @ReactMethod
    fun clearSurveyQueue(promise: Promise) {
        try {
            SurveySDK.getInstance().clearSurveyQueue()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("QUEUE_ERROR", "Failed to clear survey queue: ${e.message}")
        }
    }

    @ReactMethod
    fun isShowingSurvey(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().isShowingSurvey())
        } catch (e: Exception) {
            promise.reject("SURVEY_ERROR", "Failed to check if survey is showing: ${e.message}")
        }
    }

    @ReactMethod
    fun isSDKEnabled(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().isSDKEnabled())
        } catch (e: Exception) {
            promise.reject("STATUS_ERROR", "Failed to check if SDK is enabled: ${e.message}")
        }
    }

    @ReactMethod
    fun fetchConfiguration(promise: Promise) {
        try {
            SurveySDK.getInstance().fetchConfiguration()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to fetch configuration: ${e.message}")
        }
    }

    @ReactMethod
    fun getConfigForDebug(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().getConfigForDebug())
        } catch (e: Exception) {
            promise.reject("CONFIG_ERROR", "Failed to get config debug info: ${e.message}")
        }
    }

    @ReactMethod
    fun resetTriggers(promise: Promise) {
        try {
            SurveySDK.getInstance().resetTriggers()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("TRIGGER_ERROR", "Failed to reset triggers: ${e.message}")
        }
    }

    @ReactMethod
    fun cleanup(promise: Promise) {
        try {
            SurveySDK.getInstance().cleanup()
            coroutineScope.cancel()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CLEANUP_ERROR", "Failed to cleanup SDK: ${e.message}")
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        coroutineScope.cancel()
    }
}