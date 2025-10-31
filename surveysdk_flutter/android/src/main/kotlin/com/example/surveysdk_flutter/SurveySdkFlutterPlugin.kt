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
            result.success(true)
          } catch (e: Exception) {
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
            result.success(true)
          } catch (e: Exception) {
            result.error("SHOW_ERROR", "Failed to show survey", e.message)
          }
        } else {
          result.error("NO_ACTIVITY", "No activity available", null)
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