package com.example.surveysdk

import android.app.Activity
import android.app.Application
import android.content.Context
import com.example.surveysdk.core.SurveyPlatform
import com.example.surveysdk.android.AndroidSurveySDK

/**
 * Universal SDK entry point that works with Native Android, React Native, and Flutter
 */
class UniversalSurveySDK private constructor() {
    companion object {
        @Volatile private var instance: UniversalSurveySDK? = null

        fun getInstance(): UniversalSurveySDK {
            return instance ?: synchronized(this) {
                instance ?: UniversalSurveySDK().also { instance = it }
            }
        }
    }

    private var platform: SurveyPlatform? = null

    /**
     * Initialize for Native Android
     */
    fun initialize(context: Context, apiKey: String) {
        if (platform == null) {
            platform = AndroidSurveySDK(context.applicationContext as Application, apiKey)
        }
    }

    /**
     * Initialize for React Native/Flutter (activity-based)
     */
    fun initializeWithActivity(activity: Activity, apiKey: String) {
        if (platform == null) {
            platform = AndroidSurveySDK(activity.application, apiKey)
        }
    }

    /**
     * Show survey - works for all platforms
     */
    fun showSurvey(activity: Activity? = null) {
        platform?.showSurvey()
    }

    /**
     * Auto setup for Native Android
     */
    fun autoSetup(activity: Activity) {
        (platform as? AndroidSurveySDK)?.autoSetup(activity)
    }

    /**
     * Set user properties - works for all platforms
     */
    fun setUserProperty(key: String, value: String) {
        platform?.setUserProperty(key, value)
    }

    /**
     * Track events - works for all platforms
     */
    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        platform?.trackEvent(eventName, properties)
    }

    /**
     * Get platform instance (for bridges)
     */
    fun getPlatform(): SurveyPlatform? = platform
}