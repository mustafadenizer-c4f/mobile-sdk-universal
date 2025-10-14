package com.example.surveysdk.flutter

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.example.surveysdk.UniversalSurveySDK

class SurveySdkPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: android.app.Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "surveysdk")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "initialize" -> {
                val apiKey = call.argument<String>("apiKey") ?: ""
                activity?.let {
                    UniversalSurveySDK.getInstance().initializeWithActivity(it, apiKey)
                    result.success(null)
                } ?: result.error("NO_ACTIVITY", "No activity available", null)
            }
            "showSurvey" -> {
                activity?.let {
                    UniversalSurveySDK.getInstance().showSurvey(it)
                    result.success(null)
                } ?: result.error("NO_ACTIVITY", "No activity available", null)
            }
            "setUserProperty" -> {
                val key = call.argument<String>("key") ?: ""
                val value = call.argument<String>("value") ?: ""
                UniversalSurveySDK.getInstance().setUserProperty(key, value)
                result.success(null)
            }
            "trackEvent" -> {
                val eventName = call.argument<String>("eventName") ?: ""
                val properties = call.argument<Map<String, Any>>("properties") ?: emptyMap()
                UniversalSurveySDK.getInstance().trackEvent(eventName, properties)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}