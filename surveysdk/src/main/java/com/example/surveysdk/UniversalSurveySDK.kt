package com.example.surveysdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.example.surveysdk.core.SurveyPlatform
import com.example.surveysdk.android.AndroidSurveySDK

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
    private var androidSDK: AndroidSurveySDK? = null

    // ====================================================================
    // INITIALIZATION METHODS (UPDATED)
    // ====================================================================

    // SIMPLE: Just one initialization method
    fun initialize(application: Application, apiKey: String, vararg params: Any) {
        if (!isInitialized) {
            Log.d("UniversalSurveySDK", "Initializing with API key")
            
            // Pass through to the real SDK
            SurveySDK.initialize(application, apiKey, *params)
            
            androidSDK = AndroidSurveySDK(application, apiKey)
            platform = androidSDK
            isInitialized = true
        }
    }

    // ====================================================================
    // SURVEY DISPLAY METHODS
    // ====================================================================

    /**
     * Show survey
     */
    fun showSurvey(activity: Activity) {
        checkInitialized()
        androidSDK?.showSurveyInActivity(activity)
    }

    /**
     * Show specific survey by ID
     */
    fun showSurveyById(activity: Activity, surveyId: String) {
        checkInitialized()
        androidSDK?.showSurveyByIdInActivity(activity, surveyId)
    }

    /**
     * Auto setup
     */
    fun autoSetup(activity: Activity) {
        checkInitialized()
        androidSDK?.autoSetup(activity)
    }

    /**
     * Auto setup with navigation safety
     */
    fun autoSetupSafe(activity: Activity) {
        checkInitialized()
        // Check if AndroidSDK has this method, if not, we need to add it
        androidSDK?.let {
            // We'll need to add autoSetupSafe() to AndroidSurveySDK
            SurveySDK.getInstance().enableNavigationSafety().autoSetup(activity)
        }
    }

    // ====================================================================
    // TRIGGER METHODS (NEW)
    // ====================================================================

    /**
     * Trigger button survey
     */
    fun triggerButtonByStringId(buttonId: String, activity: Activity) {
        checkInitialized()
        androidSDK?.triggerButtonByStringId(buttonId, activity)
    }

    /**
     * Trigger navigation survey
     */
    fun triggerByNavigation(screenName: String, activity: Activity) {
        checkInitialized()
        androidSDK?.triggerByNavigation(screenName, activity)
    }

    /**
     * Trigger tab change survey
     */
    fun triggerByTabChange(tabName: String, activity: Activity) {
        checkInitialized()
        androidSDK?.triggerByTabChange(tabName, activity)
    }

    /**
     * Trigger scroll survey
     */
    fun triggerScrollManual(activity: Activity, scrollY: Int = 500) {
        checkInitialized()
        androidSDK?.let {
            // AndroidSurveySDK needs this method
            SurveySDK.getInstance().triggerScrollManual(activity, scrollY)
        }
    }

    // ====================================================================
    // USER PROPERTIES & EVENTS
    // ====================================================================

    /**
     * Set user property
     */
    fun setUserProperty(key: String, value: String) {
        checkInitialized()
        platform?.setUserProperty(key, value)
    }

    /**
     * Set custom parameter
     */
    fun setCustomParam(name: String, value: String) {
        checkInitialized()
        platform?.setCustomParam(name, value)
    }

    /**
     * Track event
     */
    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        checkInitialized()
        platform?.trackEvent(eventName, properties)
    }

    // ====================================================================
    // NAVIGATION SAFETY (NEW)
    // ====================================================================

    /**
     * Enable navigation safety mode
     */
    fun enableNavigationSafety(): UniversalSurveySDK {
        checkInitialized()
        SurveySDK.getInstance().enableNavigationSafety()
        return this
    }

    // ====================================================================
    // DEBUG & STATUS METHODS
    // ====================================================================

    /**
     * Get current parameters
     */
    fun getCurrentParameters(): Map<String, String> {
        checkInitialized()
        return androidSDK?.getCurrentParameters() ?: emptyMap()
    }
    
    /**
     * Get debug status
     */
    fun getDebugStatus(): String {
        checkInitialized()
        return androidSDK?.getDebugStatus() ?: "Platform not available"
    }
    
    /**
     * Check if user is excluded
     */
    fun isUserExcluded(surveyId: String? = null): Boolean {
        checkInitialized()
        return if (surveyId != null) {
            androidSDK?.isUserExcluded(surveyId) ?: false
        } else {
            SurveySDK.getInstance().isUserExcluded()
        }
    }
    
    /**
     * Setup button trigger
     */
    fun setupButtonTrigger(buttonId: Int, activity: Activity) {
        checkInitialized()
        androidSDK?.setupButtonTrigger(buttonId, activity)
    }

    fun setupButtonTrigger(buttonId: Int, activity: Activity, surveyId: String) {
        checkInitialized()
        androidSDK?.setupButtonTrigger(buttonId, activity, surveyId)
    }

    // ====================================================================
    // REINITIALIZATION METHODS
    // ====================================================================
    
    /**
     * Reinitialize
     */
    fun reinitialize(context: Context): Boolean {
        checkInitialized()
        
        try {
            return SurveySDK.forceReinitialize(context)
        } catch (e: Exception) {
            Log.e("UniversalSurveySDK", "Reinitialization failed: ${e.message}")
            return false
        }
    }
    
    /**
     * Reinitialize with parameters
     */
    fun reinitializeWithParameters(context: Context, apiKey: String, paramName: String, paramValue: String): Boolean {
        try {
            // Reset and create new platform
            androidSDK = AndroidSurveySDK(
                context.applicationContext as Application,
                apiKey,
                paramName,
                paramValue
            )
            platform = androidSDK
            isInitialized = true
            
            // Force reinitialize core SDK
            return SurveySDK.forceReinitialize(context)
        } catch (e: Exception) {
            Log.e("UniversalSurveySDK", "Reinitialization failed: ${e.message}")
            return false
        }
    }
    
    /**
     * Reinitialize with parameter name
     */
    fun reinitializeWithParameterName(context: Context, apiKey: String, paramName: String): Boolean {
        try {
            androidSDK = AndroidSurveySDK(
                context.applicationContext as Application,
                apiKey,
                paramName,
                null
            )
            platform = androidSDK
            isInitialized = true
            
            return SurveySDK.forceReinitialize(context)
        } catch (e: Exception) {
            Log.e("UniversalSurveySDK", "Reinitialization failed: ${e.message}")
            return false
        }
    }

    // ====================================================================
    // UTILITY METHODS
    // ====================================================================

    fun getPlatform(): SurveyPlatform? = platform
    fun isInitialized(): Boolean = isInitialized

    fun getAndroidSDK(): AndroidSurveySDK? = androidSDK

    /**
     * Check if SDK is ready
     */
    fun isReady(): Boolean {
        return isInitialized && SurveySDK.getInstance().isReady()
    }

    /**
     * Get setup status
     */
    fun getSetupStatus(): String {
        return if (isInitialized) {
            "UniversalSurveySDK Status:\n" +
            "- Initialized: $isInitialized\n" +
            "- AndroidSDK: ${androidSDK != null}\n" +
            "- Platform: ${platform?.javaClass?.simpleName ?: "null"}\n" +
            "- Core SDK Ready: ${SurveySDK.getInstance().isReady()}"
        } else {
            "UniversalSurveySDK not initialized"
        }
    }

    // ====================================================================
    // PRIVATE HELPERS
    // ====================================================================

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }
    }
}