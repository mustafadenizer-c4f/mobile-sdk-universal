package com.example.surveysdk.core

interface SurveyPlatform {
    fun showSurvey()
    fun setUserProperty(key: String, value: String)
    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap())
    fun setApiKey(apiKey: String)
}