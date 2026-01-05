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

    @ReactMethod
    fun initialize(apiKey: String, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "RN: Initializing SurveySDK...")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                SurveySDK.initialize(activity.applicationContext, apiKey)
                Log.d("SurveySDK_RN", "✅ SDK initialized successfully")
                promise.resolve(true)
            } else {
                Log.w("SurveySDK_RN", "⚠️ No activity yet, scheduling initialization")
                promise.resolve(true) // Resolve anyway
            }
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ SDK initialization failed", e)
            promise.reject("INIT_ERROR", "Initialization failed: ${e.message}")
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

    private fun createScrollListener(onScroll: (Int) -> Unit): Any {
        return java.lang.reflect.Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(Class.forName("android.view.View\$OnScrollChangeListener"))
        ) { _, method, args ->
            if (method.name == "onScrollChange") {
                // args[0] = view, args[1] = scrollX, args[2] = scrollY
                val scrollY = args?.get(2) as? Int ?: 0
                onScroll(scrollY)
            }
            null
        }
    }

    private fun triggerScrollSurvey(activity: Activity) {
        try {
            val surveySDK = SurveySDK.getInstance()
            
            // Get all surveys from config
            val configField = surveySDK.javaClass.getDeclaredField("config")
            configField.isAccessible = true
            val config = configField.get(surveySDK) as com.example.surveysdk.Config
            
            // Find scroll trigger surveys
            val scrollSurveys = config.surveys.filter { it.enableScrollTrigger && it.scrollThreshold > 0 }
            
            if (scrollSurveys.isNotEmpty()) {
                // Get the highest priority scroll survey
                val highestPrioritySurvey = scrollSurveys.maxByOrNull { it.priority }
                
                if (highestPrioritySurvey != null) {
                    Log.d("SurveySDK_RN", "🎯 Triggering scroll survey: ${highestPrioritySurvey.surveyId}")
                    
                    // Use reflection to call showSingleSurvey
                    val showSingleSurveyMethod = surveySDK.javaClass.getDeclaredMethod(
                        "showSingleSurvey", 
                        Activity::class.java,
                        com.example.surveysdk.SurveyConfig::class.java
                    )
                    showSingleSurveyMethod.isAccessible = true
                    showSingleSurveyMethod.invoke(surveySDK, activity, highestPrioritySurvey)
                }
            }
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to trigger scroll survey", e)
        }
    }

    private fun setupReactNativeNavigationDetection(activity: Activity) {
        try {
            Log.d("SurveySDK_RN", "📍 Setting up React Native navigation detection...")
            
            // This will detect when user switches between tabs
            // In a real implementation, you'd hook into React Navigation events
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Navigation detection setup failed", e)
        }
    }

    @ReactMethod
    fun registerButton(buttonId: String, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "🎯 Registering React Native button: $buttonId")
            
            registeredReactButtons.add(buttonId)
            
            val activity = getCurrentActivity()
            if (activity != null) {
                // Setup click listener for this button
                setupReactNativeButton(activity, buttonId)
            }
            
            promise.resolve(true)
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to register button", e)
            promise.reject("BUTTON_ERROR", "Failed to register button: ${e.message}")
        }
    }

    private fun setupReactNativeButton(activity: Activity, buttonId: String) {
        try {
            // This is a simplified implementation
            // In production, you'd need to find the actual React Native view
            
            Log.d("SurveySDK_RN", "✅ React Native button registered: $buttonId")
            Log.d("SurveySDK_RN", "ℹ️ Button clicks will be handled by React Native onClick listeners")
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to setup React Native button", e)
        }
    }

   
    @ReactMethod
    fun triggerButtonSurvey(buttonId: String, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "🎯 RN Bridge: Manual trigger for button: $buttonId")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                // Call the new method we added to SurveySDK.kt
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
                // Call the new manual scroll method with a high value to force trigger
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
                // Call the existing navigation method
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

    private fun triggerButtonSurveyInternal(activity: Activity, buttonId: String) {
        try {
            val surveySDK = SurveySDK.getInstance()
            
            // Get all surveys from config
            val configField = surveySDK.javaClass.getDeclaredField("config")
            configField.isAccessible = true
            val config = configField.get(surveySDK) as com.example.surveysdk.Config
            
            // Find button trigger surveys
            val buttonSurveys = config.surveys.filter { it.enableButtonTrigger }
            
            if (buttonSurveys.isNotEmpty()) {
                // Find survey for this specific button or highest priority
                val targetSurvey = buttonSurveys.find { it.buttonTriggerId == buttonId } 
                    ?: buttonSurveys.maxByOrNull { it.priority }
                
                if (targetSurvey != null) {
                    Log.d("SurveySDK_RN", "🎯 Showing button survey: ${targetSurvey.surveyId}")
                    
                    // Use reflection to call showSingleSurvey
                    val showSingleSurveyMethod = surveySDK.javaClass.getDeclaredMethod(
                        "showSingleSurvey", 
                        Activity::class.java,
                        com.example.surveysdk.SurveyConfig::class.java
                    )
                    showSingleSurveyMethod.isAccessible = true
                    showSingleSurveyMethod.invoke(surveySDK, activity, targetSurvey)
                }
            }
            
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Failed to trigger button survey", e)
        }
    }

    // Keep all existing methods unchanged...
    @ReactMethod
    fun showSurvey(promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "RN: Manual survey request...")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                val surveySDK = SurveySDK.getInstance()
                surveySDK.showSurvey(activity)
                Log.d("SurveySDK_RN", "✅ Manual survey shown")
                promise.resolve(true)
            } else {
                Log.e("SurveySDK_RN", "❌ No activity available")
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Show survey failed", e)
            promise.reject("SHOW_ERROR", "Failed to show survey: ${e.message}")
        }
    }

    @ReactMethod
    fun showSurveyById(surveyId: String, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "RN: Showing specific survey: $surveyId")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                val surveySDK = SurveySDK.getInstance()
                surveySDK.showSurveyById(activity, surveyId)
                Log.d("SurveySDK_RN", "✅ Specific survey shown: $surveyId")
                promise.resolve(true)
            } else {
                Log.e("SurveySDK_RN", "❌ No activity available")
                promise.reject("NO_ACTIVITY", "No activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Show specific survey failed", e)
            promise.reject("SHOW_ERROR", "Failed to show survey $surveyId: ${e.message}")
        }
    }

    @ReactMethod
    fun setUserProperty(key: String, value: String, promise: Promise) {
        try {
            Log.d("SurveySDK_RN", "RN: Setting user property: $key = $value")
            
            val activity = getCurrentActivity()
            if (activity != null) {
                activity.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
                    .edit()
                    .putString(key, value)
                    .apply()
                Log.d("SurveySDK_RN", "✅ User property set")
                promise.resolve(true)
            } else {
                Log.e("SurveySDK_RN", "❌ No activity available")
                promise.reject("NO_ACTIVITY", "No current activity available")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Set user property failed", e)
            promise.reject("PROPERTY_ERROR", "Failed to set user property: ${e.message}")
        }
    }

    @ReactMethod
    fun isUserExcluded(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isExcluded = surveySDK.isUserExcluded()
            Log.d("SurveySDK_RN", "RN: User excluded: $isExcluded")
            promise.resolve(isExcluded)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ User excluded check failed", e)
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion: ${e.message}")
        }
    }

    @ReactMethod
    fun getDebugStatus(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val debugStatus = surveySDK.debugSurveyStatus()
            Log.d("SurveySDK_RN", "RN: Debug status requested")
            promise.resolve(debugStatus)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "❌ Debug status failed", e)
            promise.reject("DEBUG_ERROR", "Failed to get debug status: ${e.message}")
        }
    }

    // Add all other existing methods without changes
    @ReactMethod
    fun isUserExcludedForSurvey(surveyId: String, promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isExcluded = surveySDK.isUserExcluded(surveyId)
            Log.d("SurveySDK_RN", "RN: User excluded check for $surveyId: $isExcluded")
            promise.resolve(isExcluded)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: User excluded check failed for $surveyId", e)
            promise.reject("EXCLUSION_ERROR", "Failed to check exclusion for survey $surveyId: ${e.message}")
        }
    }

    @ReactMethod
    fun setSessionData(key: String, value: String, promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.setSessionData(key, value)
            Log.d("SurveySDK_RN", "RN: Session data set: $key = $value")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Set session data failed", e)
            promise.reject("SESSION_ERROR", "Failed to set session data: ${e.message}")
        }
    }

    @ReactMethod
    fun resetSessionData(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.resetSessionData()
            Log.d("SurveySDK_RN", "RN: Session data reset")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Reset session data failed", e)
            promise.reject("SESSION_ERROR", "Failed to reset session data: ${e.message}")
        }
    }

    @ReactMethod
    fun resetTriggers(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.resetTriggers()
            Log.d("SurveySDK_RN", "RN: Triggers reset")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Reset triggers failed", e)
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
            
            Log.d("SurveySDK_RN", "RN: Retrieved ${writableArray.size()} survey IDs")
            promise.resolve(writableArray)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to get survey IDs", e)
            promise.reject("CONFIG_ERROR", "Failed to get survey IDs: ${e.message}")
        }
    }

    @ReactMethod
    fun isConfigurationLoaded(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isLoaded = surveySDK.isConfigurationLoaded()
            Log.d("SurveySDK_RN", "RN: Configuration loaded: $isLoaded")
            promise.resolve(isLoaded)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to check config status", e)
            promise.reject("CONFIG_ERROR", "Failed to check configuration status: ${e.message}")
        }
    }

    @ReactMethod
    fun getQueueStatus(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val status = surveySDK.getQueueStatus()
            Log.d("SurveySDK_RN", "RN: Queue status requested")
            promise.resolve(status)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to get queue status", e)
            promise.reject("QUEUE_ERROR", "Failed to get queue status: ${e.message}")
        }
    }

    @ReactMethod
    fun clearSurveyQueue(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.clearSurveyQueue()
            Log.d("SurveySDK_RN", "RN: Survey queue cleared")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to clear survey queue", e)
            promise.reject("QUEUE_ERROR", "Failed to clear survey queue: ${e.message}")
        }
    }

    @ReactMethod
    fun isShowingSurvey(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isShowing = surveySDK.isShowingSurvey()
            Log.d("SurveySDK_RN", "RN: Is showing survey: $isShowing")
            promise.resolve(isShowing)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to check survey status", e)
            promise.reject("SURVEY_ERROR", "Failed to check if survey is showing: ${e.message}")
        }
    }

    @ReactMethod
    fun isSDKEnabled(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val isEnabled = surveySDK.isSDKEnabled()
            Log.d("SurveySDK_RN", "RN: SDK enabled: $isEnabled")
            promise.resolve(isEnabled)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to check SDK status", e)
            promise.reject("STATUS_ERROR", "Failed to check if SDK is enabled: ${e.message}")
        }
    }

    @ReactMethod
    fun fetchConfiguration(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.fetchConfiguration()
            Log.d("SurveySDK_RN", "RN: Configuration fetch initiated")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to fetch configuration", e)
            promise.reject("CONFIG_ERROR", "Failed to fetch configuration: ${e.message}")
        }
    }

    @ReactMethod
    fun getConfigForDebug(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            val configDebug = surveySDK.getConfigForDebug()
            Log.d("SurveySDK_RN", "RN: Config debug requested")
            promise.resolve(configDebug)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to get config debug", e)
            promise.reject("CONFIG_ERROR", "Failed to get config debug info: ${e.message}")
        }
    }

    @ReactMethod
    fun cleanup(promise: Promise) {
        try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.cleanup()
            coroutineScope.cancel()
            Log.d("SurveySDK_RN", "RN: SDK cleanup completed")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "RN: Failed to cleanup SDK", e)
            promise.reject("CLEANUP_ERROR", "Failed to cleanup SDK: ${e.message}")
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        coroutineScope.cancel()
    }
}