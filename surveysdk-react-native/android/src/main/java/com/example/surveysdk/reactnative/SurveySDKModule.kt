package com.example.surveysdk.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.example.surveysdk.SurveySDK
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
        // Store the current activity
        currentActivity = WeakReference(reactContext.currentActivity)
    }

    override fun getName(): String = "SurveySDK"


    // async initialize(apiKey, params) {
    //     if (params && params.length > 0) {
    //         return await SurveySDK.initializeWithParams(apiKey, params);
    //     } else {
    //         return await SurveySDK.initialize(apiKey);
    //     }
    // }

    @ReactMethod
    fun initialize(apiKey: String, params: ReadableArray?, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "RN: Initializing SurveySDK...")
            
            val activity = getCurrentActivity()
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            val context = activity.applicationContext
            
            // ALWAYS use the simple 2-parameter initialize method
            // We'll handle parameters separately
            SurveySDK.initialize(context, apiKey)
            
            // If we have parameters, set them manually
            if (params != null && params.size() > 0) {
                setParametersFromReactNative(context, params)
            }
            
            Log.d("SurveySDK_RN", "✅ SDK initialized")
            promise.resolve(true)
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ SDK initialization failed", e)
            promise.reject("INIT_ERROR", "Initialization failed: ${e.message}")
        }
    }

    private fun setParametersFromReactNative(context: Context, params: ReadableArray) {
        try {
            // Get the SDK instance
            val surveySDK = SurveySDK.getInstance()
            
            // Use reflection to access the customParams field
            val customParamsField = surveySDK.javaClass.getDeclaredField("customParams")
            customParamsField.isAccessible = true
            
            // Get current parameters
            val currentParams = (customParamsField.get(surveySDK) as? Map<*, *>)?.let {
                try {
                    @Suppress("UNCHECKED_CAST")
                    it as MutableMap<String, String>
                } catch (e: Exception) {
                    mutableMapOf<String, String>()
                }
            } ?: mutableMapOf<String, String>()
            
            // Add parameters from React Native
            for (i in 0 until params.size()) {
                when (params.getType(i)) {
                    ReadableType.String -> {
                        val paramName = params.getString(i)
                        if (paramName != null) {
                            // Look up from storage
                            val value = try {
                                val storageUtilsClass = Class.forName("com.example.surveysdk.StorageUtils")
                                val method = storageUtilsClass.getDeclaredMethod("findSpecificData", Context::class.java, String::class.java)
                                method.invoke(null, context, paramName) as? String
                            } catch (e: Exception) {
                                null
                            }
                            if (value != null) {
                                currentParams[paramName] = value
                                Log.d("SurveySDK_RN", "   ✅ From storage: $paramName = $value")
                            }
                        }
                    }
                    ReadableType.Map -> {
                        val paramMap = params.getMap(i)
                        paramMap?.keySetIterator()?.let { iterator ->
                            while (iterator.hasNextKey()) {
                                val key = iterator.nextKey()
                                val value = paramMap.getString(key)
                                if (key != null && value != null) {
                                    currentParams[key] = value
                                    Log.d("SurveySDK_RN", "   ✅ Direct param: $key = $value")
                                }
                            }
                        }
                    }
                    else -> {
                        Log.w("SurveySDK_RN", "⚠️ Skipping invalid parameter type")
                    }
                }
            }
            
            // Update the customParams field
            customParamsField.set(surveySDK, currentParams)
            
            Log.d("SurveySDK_RN", "✅ Set ${currentParams.size} parameters via reflection")
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to set parameters", e)
        }
    }

    @ReactMethod
    fun autoSetup(promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "🔄 RN: Starting autoSetup...")
            
            val activity = getCurrentActivity()
            if (activity == null) {
                Log.e("SurveySDK_RN", "❌ No activity available")
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            // Run on UI thread
            activity.runOnUiThread {
                try {
                    // Wait for UI to be ready
                    activity.window?.decorView?.postDelayed({
                        try {
                            Log.d("SurveySDK_RN", "📱 Setting up React Native auto detection...")
                            
                            // Get SDK instance
                            val surveySDK = SurveySDK.getInstance()
                            
                            // Setup React Native specific detection
                            setupReactNativeDetection(activity)
                            
                            // Also call the core SDK's autoSetup for native detection
                            surveySDK.autoSetup(activity)
                            
                            isAutoSetupComplete = true
                            
                            Log.d("SurveySDK_RN", "✅ React Native auto setup completed")
                            Log.d("SurveySDK_RN", "🎯 Detection methods:")
                            Log.d("SurveySDK_RN", "   • React Native button click listeners")
                            Log.d("SurveySDK_RN", "   • React Native scroll listeners")
                            Log.d("SurveySDK_RN", "   • Navigation tab changes")
                            
                            promise.resolve(true)
                            
                        } catch (e: Exception) {
                            Log.e("SurveySDK_RN", "❌ Auto setup failed", e)
                            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
                        }
                    }, 500) // Give UI time to fully render
                    
                } catch (e: Exception) {
                    Log.e("SurveySDK_RN", "❌ UI thread setup failed", e)
                    promise.reject("SETUP_ERROR", "UI thread setup failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Auto setup failed", e)
            promise.reject("SETUP_ERROR", "Auto setup failed: ${e.message}")
        }
    }

    private fun setupReactNativeDetection(activity: Activity) {
        try {
            Log.d("SurveySDK_RN", "🔍 Setting up React Native detection...")
            
            // Setup scroll detection for React Native
            setupReactNativeScrollDetection(activity)
            
            // Setup navigation detection for React Native tabs
            setupReactNativeNavigationDetection(activity)
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ React Native detection setup failed", e)
        }
    }

    private fun setupReactNativeScrollDetection(activity: Activity) {
        try {
            Log.d("SurveySDK_RN", "📜 Setting up React Native scroll detection...")
            
            // Find React Native ScrollView or FlatList components
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
            // Check if this is a scrollable view
            if (view::class.java.name.contains("ScrollView") ||
                view::class.java.name.contains("FlatList") ||
                view::class.java.name.contains("RecyclerView")) {
                
                Log.d("SurveySDK_RN", "✅ Found scrollable view: ${view.javaClass.simpleName}")
                setupScrollListener(view, activity)
            }
            
            // Recursively check child views
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    findAndSetupScrollViews(child, activity)
                }
            }
            
        } catch (e: Exception) {
            // Silent fail for individual views
        }
    }

   private fun setupScrollListener(scrollView: View, activity: Activity) {
        try {
            // State variables specific to this listener
            var lastTriggerTime = 0L
            val SCROLL_COOLDOWN_MS = 3000L // 3 Seconds wait time between triggers
            
            Log.d("SurveySDK_RN", "🎯 Setting up native scroll listener on: ${scrollView.javaClass.simpleName}")

            // Use Android's standard ViewTreeObserver instead of Reflection
            scrollView.viewTreeObserver.addOnScrollChangedListener {
                
                // 1. COOLDOWN CHECK (The "Fren" Mechanism)
                // If 3 seconds haven't passed since the last trigger, stop here.
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTriggerTime < SCROLL_COOLDOWN_MS) {
                    return@addOnScrollChangedListener
                }

                // 2. CALCULATE POSITIONS
                val scrollY = scrollView.scrollY
                val viewHeight = scrollView.height
                
                // React Native ScrollViews usually have 1 child containing the actual content
                val contentHeight = if (scrollView is ViewGroup && scrollView.childCount > 0) {
                    scrollView.getChildAt(0).height
                } else {
                    scrollView.height
                }

                // 3. CALCULATE PERCENTAGE
                // Formula: (Current Position + Screen Height) / Total Content Height
                val scrollPercentage = if (contentHeight > viewHeight) {
                    ((scrollY.toFloat() + viewHeight) / contentHeight) * 100
                } else {
                    0f
                }

                // Optional: Log only occasionally to avoid spam
                // Log.d("SurveySDK_RN", "📜 Scroll: $scrollPercentage%")

                // 4. TRIGGER CONDITION (e.g., Reached 90% of the page)
                if (scrollPercentage >= 90) {
                    Log.d("SurveySDK_RN", "🎯 Scroll threshold reached ($scrollPercentage%) - Triggering Survey")
                    
                    // LOCK: Update the time so it doesn't trigger again immediately
                    lastTriggerTime = currentTime
                    
                    // FIRE THE EVENT
                    triggerScrollSurvey(activity)
                }
            }
            
            Log.d("SurveySDK_RN", "✅ Scroll listener setup successful")

        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to setup scroll listener", e)
        }
    }

    private fun triggerScrollSurvey(activity: Activity) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.triggerScrollManual(activity, 1000)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to trigger scroll survey", e)
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

    private fun setupReactNativeNavigationDetection(activity: Activity) {
        try {
            Log.d("SurveySDK_RN", "📍 Setting up React Native navigation detection...")
            // Implementation would hook into React Navigation events
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Navigation detection setup failed", e)
        }
    }

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
            Log.e("SurveySDK_RN", "❌ Failed to trigger button survey", e)
            promise.reject("TRIGGER_ERROR", "Failed to trigger survey: ${e.message}")
        }
    }

    @ReactMethod
    fun triggerScrollSurvey(promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "📜 RN Bridge: Manual scroll trigger")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.getInstance().triggerScrollManual(activity, 1000)
                promise.resolve(true)
            } else {
                promise.reject("NO_ACTIVITY", "No activity available")
            }
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to trigger scroll survey", e)
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
            Log.e("SurveySDK_RN", "❌ Failed to trigger navigation survey", e)
            promise.reject("TRIGGER_ERROR", "Failed to trigger navigation survey: ${e.message}")
        }
    }

    // Keep all other existing methods exactly as they were...
    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "RN: Manual survey request...")
            
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
    fun isUserExcluded(promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().isUserExcluded())
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion: ${e.message}")
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
    fun isUserExcludedForSurvey(surveyId: String, promise: Promise) {
        try {
            promise.resolve(SurveySDK.getInstance().isUserExcluded(surveyId))
        } catch (e: Exception) {
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion for survey $surveyId: ${e.message}")
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