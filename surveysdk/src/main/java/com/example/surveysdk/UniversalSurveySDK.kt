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
    private var isInitialized = false

    /**
     * Initialize for Native Android
     */
    fun initialize(context: Context, apiKey: String) {
        if (!isInitialized) {
            platform = AndroidSurveySDK(context.applicationContext as Application, apiKey)
            isInitialized = true
        }
    }

    /**
     * Initialize for React Native/Flutter (activity-based)
     */
    fun initializeWithActivity(activity: Activity, apiKey: String) {
        if (!isInitialized) {
            platform = AndroidSurveySDK(activity.application, apiKey)
            isInitialized = true
        }
    }

    /**
     * Show survey - works for all platforms
     */
    fun showSurvey(activity: Activity? = null) {
        if (!isInitialized) {
            throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }
        
        activity?.let {
            // For React Native, we need to show the survey in the provided activity
            (platform as? AndroidSurveySDK)?.showSurveyInActivity(it)
        } ?: run {
            platform?.showSurvey()
        }
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
        if (!isInitialized) {
            throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }
        platform?.setUserProperty(key, value)
    }

    /**
     * Track events - works for all platforms
     */
    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        if (!isInitialized) {
            throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }
        platform?.trackEvent(eventName, properties)
    }

    /**
     * Get platform instance (for bridges)
     */
    fun getPlatform(): SurveyPlatform? = platform

    /**
     * Check if SDK is initialized
     */
    fun isInitialized(): Boolean = isInitialized
}