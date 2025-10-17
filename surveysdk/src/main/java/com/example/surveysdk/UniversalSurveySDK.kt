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
     * Initialize for React Native/Flutter (activity-based)
     */
    /**
 * Initialize for React Native/Flutter (activity-based)
 */
    fun initializeWithActivity(activity: Activity, apiKey: String) {
        if (!isInitialized) {
            Log.d("UniversalSurveySDK", "Initializing with activity and API key")
            platform = AndroidSurveySDK(activity.application as Application, apiKey)
            
            // FIX: Actually initialize the core SurveySDK
            SurveySDK.initialize(activity.applicationContext, apiKey)
            
            isInitialized = true
            Log.d("UniversalSurveySDK", "Initialization completed - isInitialized: $isInitialized")
        } else {
            Log.d("UniversalSurveySDK", "Already initialized")
        }
    }

    /**
    * Show survey - works for React Native
    */
    fun showSurvey(activity: Activity) {
        Log.d("UniversalSurveySDK", "showSurvey called - isInitialized: $isInitialized")
        
        if (!isInitialized) {
            throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }
        
        // FIX: Use the core SurveySDK directly since we initialized it
        try {
            SurveySDK.getInstance().showSurvey(activity)
            Log.d("UniversalSurveySDK", "Survey shown via core SDK")
        } catch (e: Exception) {
            Log.e("UniversalSurveySDK", "Error showing survey via core SDK", e)
            throw e
        }
    }

    /**
     * Auto setup for Native Android
     */
    fun autoSetup(activity: Activity) {
        if (!isInitialized) {
            throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }
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