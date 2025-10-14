package com.example.surveysdk.android

import android.app.Activity
import android.app.Application
import android.content.Context
import com.example.surveysdk.core.SurveyPlatform
import com.example.surveysdk.SurveySDK

class AndroidSurveySDK(
    private val context: Application,
    private val apiKey: String
) : SurveyPlatform {

    private val surveySDK: SurveySDK by lazy {
        SurveySDK.initialize(context, apiKey)
        SurveySDK.getInstance()
    }

    override fun showSurvey() {
        // Will be shown when activity is available
        // For React Native/Flutter, we'll handle this differently
    }

    override fun setUserProperty(key: String, value: String) {
        // Store in shared preferences for cross-platform access
        context.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun trackEvent(eventName: String, properties: Map<String, Any>) {
        // Track events for analytics
        android.util.Log.d("SurveySDK", "Event: $eventName, Properties: $properties")
    }

    override fun setApiKey(apiKey: String) {
        // Re-initialize with new API key
        SurveySDK.initialize(context, apiKey)
    }

    fun autoSetup(activity: Activity) {
        surveySDK.autoSetup(activity)
    }

    fun showSurveyInActivity(activity: Activity) {
        surveySDK.showSurvey(activity)
    }
}