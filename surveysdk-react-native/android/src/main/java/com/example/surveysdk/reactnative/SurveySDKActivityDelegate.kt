package com.example.surveysdk.reactnative

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.example.surveysdk.SurveySDK
import com.facebook.react.ReactActivity
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import kotlinx.coroutines.*

class SurveySDKActivityDelegate(private val reactContext: ReactApplicationContext) {
    private var currentActivity: Activity? = null
    private var isAutoSetupComplete = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Activity lifecycle callbacks to track React Native activities
    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Log.d("SurveySDK_RN", "Activity created: ${activity.javaClass.simpleName}")
            
            // Only handle ReactActivity instances
            if (activity is ReactActivity) {
                Log.d("SurveySDK_RN", "ReactActivity detected: ${activity.javaClass.simpleName}")
                currentActivity = activity
                
                // Auto-setup when activity is ready
                if (!isAutoSetupComplete) {
                    setupAutoDetection(activity)
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (activity is ReactActivity) {
                Log.d("SurveySDK_RN", "ReactActivity started: ${activity.javaClass.simpleName}")
                currentActivity = activity
            }
        }

        override fun onActivityResumed(activity: Activity) {
            if (activity is ReactActivity) {
                Log.d("SurveySDK_RN", "ReactActivity resumed: ${activity.javaClass.simpleName}")
                currentActivity = activity
                
                // Track screen view for navigation detection
                try {
                    SurveySDK.getInstance().trackScreenView(activity)
                } catch (e: Exception) {
                    Log.e("SurveySDK_RN", "Error tracking screen view: ${e.message}")
                }
            }
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            if (activity == currentActivity) {
                Log.d("SurveySDK_RN", "Current ReactActivity destroyed")
                currentActivity = null
                
                // Clean up queue for this activity
                try {
                    SurveySDK.getInstance().clearQueueForActivity(activity)
                } catch (e: Exception) {
                    Log.e("SurveySDK_RN", "Error clearing queue: ${e.message}")
                }
            }
        }
    }
    
    fun initialize() {
        try {
            val application = reactContext.applicationContext as? Application
            application?.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            Log.d("SurveySDK_RN", "Activity delegate initialized")
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "Failed to initialize activity delegate: ${e.message}")
        }
    }
    
    private fun setupAutoDetection(activity: Activity) {
        UiThreadUtil.runOnUiThread {
            try {
                Log.d("SurveySDK_RN", "Setting up auto detection on UI thread")
                
                // Wait for UI to be ready
                activity.window?.decorView?.post {
                    try {
                        val surveySDK = SurveySDK.getInstance()
                        surveySDK.autoSetup(activity)
                        isAutoSetupComplete = true
                        Log.d("SurveySDK_RN", "✅ Auto detection setup complete")
                    } catch (e: Exception) {
                        Log.e("SurveySDK_RN", "Auto detection setup failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SurveySDK_RN", "UI thread setup failed: ${e.message}")
            }
        }
    }
    
    fun getCurrentActivity(): Activity? {
        return currentActivity
    }
    
    fun cleanup() {
        try {
            val application = reactContext.applicationContext as? Application
            application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
            coroutineScope.cancel()
            Log.d("SurveySDK_RN", "Activity delegate cleaned up")
        } catch (e: Exception) {
            Log.e("SurveySDK_RN", "Error cleaning up activity delegate: ${e.message}")
        }
    }
}