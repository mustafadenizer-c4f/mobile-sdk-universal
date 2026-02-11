package com.example.surveysdk.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.example.surveysdk.SurveySDK
import com.example.surveysdk.core.SurveyPlatform

class AndroidSurveySDK(
    private val context: Application,
    private val apiKey: String,
    private val paramName: String? = null,
    private val paramValue: String? = null
) : SurveyPlatform {

    private val surveySDK: SurveySDK by lazy {
        when {
            paramName != null && paramValue != null -> 
                SurveySDK.initialize(context, apiKey, paramName to paramValue)
            paramName != null -> 
                SurveySDK.initialize(context, apiKey, paramName)
            else -> 
                SurveySDK.initialize(context, apiKey)
        }
        SurveySDK.getInstance()
    }

    fun initializeCoreSDK() {
        val isEnabled = surveySDK.isSDKEnabled()
        Log.d("AndroidSurveySDK", "Core SDK initialized. Enabled: $isEnabled")
    }   
    
    /**
     * Get current SDK parameters
     */
    fun getCurrentParameters(): Map<String, String> {
        return SurveySDK.getCurrentParameters()
    }

    override fun showSurvey() {
        throw IllegalStateException("Use showSurveyInActivity(activity) for React Native")
    }

    override fun setUserProperty(key: String, value: String) {
        context.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun setCustomParam(name: String, value: String) {
        context.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
            .edit()
            .putString(name, value)
            .apply()
        Log.d("AndroidSurveySDK", "Custom param set: $name=$value")
    }

    override fun trackEvent(eventName: String, properties: Map<String, Any>) {
        Log.d("SurveySDK", "Event tracked: $eventName, Properties: $properties")
    }

    override fun setApiKey(apiKey: String) {
        // Do nothing - API key is set in constructor
    }

    // --- Core SDK Methods ---

    fun autoSetup(activity: Activity) {
        surveySDK.autoSetup(activity)
    }

    fun showSurveyInActivity(activity: Activity) {
        surveySDK.showSurvey(activity)
    }

    fun showSurveyByIdInActivity(activity: Activity, surveyId: String) {
        surveySDK.showSurveyById(activity, surveyId)
    }

    fun isUserExcluded(surveyId: String): Boolean {
        return surveySDK.isUserExcluded(surveyId)
    }

    fun getDebugStatus(): String {
        return surveySDK.debugSurveyStatus()
    }
    
    fun setupButtonTrigger(buttonId: Int, activity: Activity) {
        surveySDK.setupButtonTrigger(buttonId, activity, null)
    }
    
    fun setupButtonTrigger(buttonId: Int, activity: Activity, surveyId: String) {
        surveySDK.setupButtonTrigger(buttonId, activity, surveyId)
    }
    
    fun triggerButtonByStringId(buttonId: String, activity: Activity) {
        surveySDK.triggerButtonByStringId(buttonId, activity)
    }
    
    fun triggerByNavigation(screenName: String, activity: Activity) {
        surveySDK.triggerByNavigation(screenName, activity)
    }
    
    fun triggerByTabChange(tabName: String, activity: Activity) {
        surveySDK.triggerByTabChange(tabName, activity)
    }

    
    fun autoSetupSafe(activity: Activity) {
        surveySDK.enableNavigationSafety().autoSetup(activity)
    }

    fun triggerScrollManual(activity: Activity, scrollY: Int = 500) {
        surveySDK.triggerScrollManual(activity, scrollY)
    }
    
    fun reinitializeWithParameters(paramName: String, paramValue: String): Boolean {
        try {
            // Reinitialize the core SDK with new parameters
            SurveySDK.initialize(context, apiKey, paramName to paramValue)
            return true
        } catch (e: Exception) {
            Log.e("AndroidSurveySDK", "Reinitialization failed: ${e.message}")
            return false
        }
    }
    
    fun reinitializeWithParameterName(paramName: String): Boolean {
        try {
            // Reinitialize the core SDK with parameter name
            SurveySDK.initialize(context, apiKey, paramName)
            return true
        } catch (e: Exception) {
            Log.e("AndroidSurveySDK", "Reinitialization failed: ${e.message}")
            return false
        }
    }


}