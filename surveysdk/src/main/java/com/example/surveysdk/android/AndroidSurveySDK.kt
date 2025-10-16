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
        throw IllegalStateException("Use showSurveyInActivity(activity) for React Native")
    }

    override fun setUserProperty(key: String, value: String) {
        context.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun trackEvent(eventName: String, properties: Map<String, Any>) {
        android.util.Log.d("SurveySDK", "Event tracked: $eventName, Properties: $properties")
    }

    override fun setApiKey(apiKey: String) {
        // Not used in React Native context
    }

    fun autoSetup(activity: Activity) {
        surveySDK.autoSetup(activity)
    }

    fun showSurveyInActivity(activity: Activity) {
        surveySDK.showSurvey(activity)
    }

    fun isUserExcluded(): Boolean {
        return surveySDK.isUserExcluded()
    }

    fun getDebugStatus(): String {
        return surveySDK.debugSurveyStatus()
    }
}