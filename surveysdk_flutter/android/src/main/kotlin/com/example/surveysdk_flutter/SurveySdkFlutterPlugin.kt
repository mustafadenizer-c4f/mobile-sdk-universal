package com.example.surveysdk_flutter

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.app.Activity
import android.util.Log
import com.example.surveysdk.SurveySDK

class SurveySdkFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var channel : MethodChannel
  private var activity: Activity? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "surveysdk_flutter")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "initialize" -> {
        val apiKey = call.argument<String>("apiKey")
        if (apiKey != null && activity != null) {
          try {
            SurveySDK.initialize(activity!!.applicationContext, apiKey)
            Log.d("SurveySDKFlutter", "SDK initialized successfully")
            result.success(true)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "Initialization failed: ${e.message}")
            result.error("INIT_ERROR", "Initialization failed", e.message)
          }
        } else {
          result.error("INVALID_ARGS", "API key or activity missing", null)
        }
      }
      
      "showSurvey" -> {
        if (activity != null) {
          try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.showSurvey(activity!!)
            Log.d("SurveySDKFlutter", "Showing auto-selected survey")
            result.success(true)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "Show survey failed: ${e.message}")
            result.error("SHOW_ERROR", "Failed to show survey", e.message)
          }
        } else {
          result.error("NO_ACTIVITY", "No activity available", null)
        }
      }
      
      "showSurveyById" -> {
        val surveyId = call.argument<String>("surveyId")
        if (activity != null && surveyId != null) {
          try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.showSurveyById(activity!!, surveyId)
            Log.d("SurveySDKFlutter", "Showing specific survey: $surveyId")
            result.success(true)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "Show survey by ID failed: ${e.message}")
            result.error("SHOW_ERROR", "Failed to show survey $surveyId", e.message)
          }
        } else {
          result.error("INVALID_ARGS", "Survey ID or activity missing", null)
        }
      }
      
      "autoSetup" -> {
        if (activity != null) {
          try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.autoSetup(activity!!)
            Log.d("SurveySDKFlutter", "Auto setup completed")
            result.success(true)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "Auto setup failed: ${e.message}")
            result.error("SETUP_ERROR", "Auto setup failed", e.message)
          }
        } else {
          result.error("NO_ACTIVITY", "No activity available", null)
        }
      }
      
      "isUserExcluded" -> {
        try {
          val surveySDK = SurveySDK.getInstance()
          val isExcluded = surveySDK.isUserExcluded()
          Log.d("SurveySDKFlutter", "User excluded check: $isExcluded")
          result.success(isExcluded)
        } catch (e: Exception) {
          Log.e("SurveySDKFlutter", "User excluded check failed: ${e.message}")
          result.error("EXCLUSION_ERROR", "Failed to check exclusion", e.message)
        }
      }
      
      "isUserExcludedForSurvey" -> {
        val surveyId = call.argument<String>("surveyId")
        if (surveyId != null) {
          try {
            val surveySDK = SurveySDK.getInstance()
            val isExcluded = surveySDK.isUserExcluded(surveyId)
            Log.d("SurveySDKFlutter", "User excluded check for $surveyId: $isExcluded")
            result.success(isExcluded)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "User excluded check failed for $surveyId: ${e.message}")
            result.error("EXCLUSION_ERROR", "Failed to check exclusion for survey", e.message)
          }
        } else {
          result.error("INVALID_ARGS", "Survey ID missing", null)
        }
      }
      
      "getDebugStatus" -> {
        try {
          val surveySDK = SurveySDK.getInstance()
          val debugStatus = surveySDK.debugSurveyStatus()
          Log.d("SurveySDKFlutter", "Debug status retrieved")
          result.success(debugStatus)
        } catch (e: Exception) {
          Log.e("SurveySDKFlutter", "Debug status failed: ${e.message}")
          result.error("DEBUG_ERROR", "Failed to get debug status", e.message)
        }
      }
      
      "getSurveyIds" -> {
        try {
          val surveySDK = SurveySDK.getInstance()
          val surveyIds = surveySDK.getSurveyIds()
          Log.d("SurveySDKFlutter", "Retrieved ${surveyIds.size} survey IDs")
          result.success(surveyIds)
        } catch (e: Exception) {
          Log.e("SurveySDKFlutter", "Get survey IDs failed: ${e.message}")
          result.error("CONFIG_ERROR", "Failed to get survey IDs", e.message)
        }
      }
      
      "isConfigurationLoaded" -> {
        try {
          val surveySDK = SurveySDK.getInstance()
          val isLoaded = surveySDK.isConfigurationLoaded()
          Log.d("SurveySDKFlutter", "Configuration loaded: $isLoaded")
          result.success(isLoaded)
        } catch (e: Exception) {
          Log.e("SurveySDKFlutter", "Config status check failed: ${e.message}")
          result.error("CONFIG_ERROR", "Failed to check config status", e.message)
        }
      }
      
      "setUserProperty" -> {
        val key = call.argument<String>("key")
        val value = call.argument<String>("value")
        if (key != null && value != null && activity != null) {
          try {
            activity!!.getSharedPreferences("survey_sdk_data", android.content.Context.MODE_PRIVATE)
              .edit()
              .putString(key, value)
              .apply()
            Log.d("SurveySDKFlutter", "User property set: $key = $value")
            result.success(true)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "Set user property failed: ${e.message}")
            result.error("PROPERTY_ERROR", "Failed to set user property", e.message)
          }
        } else {
          result.error("INVALID_ARGS", "Key, value or activity missing", null)
        }
      }
      
      "trackEvent" -> {
        val eventName = call.argument<String>("eventName")
        val properties = call.argument<Map<String, Any>>("properties")
        if (eventName != null) {
          try {
            Log.d("SurveySDKFlutter", "Event tracked: $eventName, Properties: $properties")
            result.success(true)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "Track event failed: ${e.message}")
            result.error("TRACK_ERROR", "Failed to track event", e.message)
          }
        } else {
          result.error("INVALID_ARGS", "Event name missing", null)
        }
      }
      
      "setSessionData" -> {
        val key = call.argument<String>("key")
        val value = call.argument<String>("value")
        if (key != null && value != null) {
          try {
            val surveySDK = SurveySDK.getInstance()
            surveySDK.setSessionData(key, value)
            Log.d("SurveySDKFlutter", "Session data set: $key = $value")
            result.success(true)
          } catch (e: Exception) {
            Log.e("SurveySDKFlutter", "Set session data failed: ${e.message}")
            result.error("SESSION_ERROR", "Failed to set session data", e.message)
          }
        } else {
          result.error("INVALID_ARGS", "Key or value missing", null)
        }
      }
      
      "resetSessionData" -> {
        try {
          val surveySDK = SurveySDK.getInstance()
          surveySDK.resetSessionData()
          Log.d("SurveySDKFlutter", "Session data reset")
          result.success(true)
        } catch (e: Exception) {
          Log.e("SurveySDKFlutter", "Reset session data failed: ${e.message}")
          result.error("SESSION_ERROR", "Failed to reset session data", e.message)
        }
      }
      
      "resetTriggers" -> {
        try {
          val surveySDK = SurveySDK.getInstance()
          surveySDK.resetTriggers()
          Log.d("SurveySDKFlutter", "Triggers reset")
          result.success(true)
        } catch (e: Exception) {
          Log.e("SurveySDKFlutter", "Reset triggers failed: ${e.message}")
          result.error("TRIGGER_ERROR", "Failed to reset triggers", e.message)
        }
      }
      
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }
}