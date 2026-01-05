package com.example.surveysdk.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.example.surveysdk.SurveySDK
import com.example.surveysdk.core.SurveyPlatform

class AndroidSurveySDK(
    private val context: Application,
    private val apiKey: String
) : SurveyPlatform {

    // Core SDK'yı lazy olarak (ihtiyaç duyulduğunda) başlat
    private val surveySDK: SurveySDK by lazy {
        SurveySDK.initialize(context, apiKey)
        SurveySDK.getInstance()
    }

    // React Native tarafından SDK'yı manuel tetiklemek için kullanılır
    fun initializeCoreSDK() {
        val isEnabled = surveySDK.isSDKEnabled()
        Log.d("AndroidSurveySDK", "Core SDK initialized. Enabled: $isEnabled")
    }    

    // React Native tarafı Activity referansı olmadan çağırırsa hata fırlatır
    // (React Native için showSurveyInActivity kullanılmalı)
    override fun showSurvey() {
        throw IllegalStateException("Use showSurveyInActivity(activity) for React Native")
    }

    // Kullanıcı özelliklerini kaydetmek için
    override fun setUserProperty(key: String, value: String) {
        context.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    // Olay takibi
    override fun trackEvent(eventName: String, properties: Map<String, Any>) {
        Log.d("SurveySDK", "Event tracked: $eventName, Properties: $properties")
    }

    override fun setApiKey(apiKey: String) {
        // React Native'de constructor ile alındığı için burası boş kalabilir
    }

    // --- Core SDK'ya Yönlendirilen Metodlar ---

    fun autoSetup(activity: Activity) {
        surveySDK.autoSetup(activity)
    }

    fun showSurveyInActivity(activity: Activity) {
        surveySDK.showSurvey(activity)
    }

    // Core SDK'ya surveyId ileterek kontrol eder
    fun isUserExcluded(surveyId: String): Boolean {
        return surveySDK.isUserExcluded(surveyId)
    }

    // Debug durumunu String olarak döner
    fun getDebugStatus(): String {
        return surveySDK.debugSurveyStatus()
    }
    
    // Buton tetikleyicisi kurulumu (Core SDK'ya null surveyId ileterek varsayılanı kullanır)
    fun setupButtonTrigger(buttonId: Int, activity: Activity) {
        surveySDK.setupButtonTrigger(buttonId, activity, null)
    }
}