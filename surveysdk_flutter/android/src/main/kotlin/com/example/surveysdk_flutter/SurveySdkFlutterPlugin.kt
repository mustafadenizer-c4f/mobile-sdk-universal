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
import android.content.Context
import android.util.Log
import com.example.surveysdk.SurveySDK // Core SDK Import

class SurveySdkFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel : MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "surveysdk_flutter")
        channel.setMethodCallHandler(this)
        Log.d("SurveySDKFlutter", "🔌 Plugin attached via MethodChannel")
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        
        when (call.method) {
            // =====================================================================
            // 🚀 1. INITIALIZATION & SETUP
            // =====================================================================
            "initialize" -> {
                val apiKey = call.argument<String>("apiKey")
                if (apiKey != null && activity != null) {
                    try {
                        SurveySDK.initialize(activity!!.applicationContext, apiKey)
                        Log.d("SurveySDKFlutter", "✅ SDK initialized")
                        result.success(true)
                    } catch (e: Exception) {
                        Log.e("SurveySDKFlutter", "❌ Init failed", e)
                        result.error("INIT_ERROR", e.message, null)
                    }
                } else {
                    result.error("INVALID_ARGS", "API key or activity missing", null)
                }
            }
            
            "autoSetup" -> {
                if (activity != null) {
                    try {
                        // Flutter için sadece Lifecycle (App Launch) takibi yapar.
                        // Buton taraması Flutter tarafında Widget ile yapılır.
                        SurveySDK.getInstance().autoSetup(activity!!)
                        Log.d("SurveySDKFlutter", "✅ Lifecycle Auto-Setup completed")
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SETUP_ERROR", e.message, null)
                    }
                } else {
                    result.error("NO_ACTIVITY", "No activity", null)
                }
            }

            // =====================================================================
            // 🔗 2. TRIGGERS (Flutter'dan Gelen Sinyaller)
            // =====================================================================
            
            "triggerButton" -> {
                val buttonId = call.argument<String>("buttonId")
                if (activity != null && buttonId != null) {
                    // Core SDK'daki String ID eşleştiriciyi kullanır
                    SurveySDK.getInstance().triggerButtonByStringId(buttonId, activity!!)
                    result.success(true)
                } else {
                    result.success(false)
                }
            }

            "triggerNavigation" -> {
                val screenName = call.argument<String>("screenName")
                if (activity != null && screenName != null) {
                    // Core SDK'ya navigasyon bilgisi ver
                    SurveySDK.getInstance().triggerByNavigation(screenName, activity!!)
                    // Tab değişimi de olabilir, onu da kontrol et
                    SurveySDK.getInstance().triggerByTabChange(screenName, activity!!)
                    result.success(true)
                } else {
                    result.success(false)
                }
            }

            "triggerScroll" -> {
                if (activity != null) {
                    val scrollY = call.argument<Int>("scrollY") ?: 500
                    SurveySDK.getInstance().triggerScrollManual(activity!!, scrollY)
                    result.success(true)
                } else {
                    result.success(false)
                }
            }

            // =====================================================================
            // 🛠️ 3. SHOW & DISPLAY METHODS
            // =====================================================================

            "showSurvey" -> {
                if (activity != null) {
                    SurveySDK.getInstance().showSurvey(activity!!)
                    result.success(true)
                } else {
                    result.error("NO_ACTIVITY", "No activity", null)
                }
            }
            
            "showSurveyById" -> {
                val surveyId = call.argument<String>("surveyId")
                if (activity != null && surveyId != null) {
                    // DÜZELTME: Manuel Intent açmak yerine Core SDK'ya devrettik.
                    // Core SDK config'e bakıp Dialog mu BottomSheet mi karar verecek.
                    SurveySDK.getInstance().showSurveyById(activity!!, surveyId)
                    result.success(true)
                } else {
                    result.error("INVALID_ARGS", "Missing ID", null)
                }
            }

            // =====================================================================
            // ⚙️ 4. USER PROPERTIES & SESSION
            // =====================================================================

            "setUserProperty" -> {
                val key = call.argument<String>("key")
                val value = call.argument<String>("value")
                if (key != null && value != null && activity != null) {
                    // Veriyi SharedPreferences'a yazıyoruz, Core SDK buradan okuyor
                    activity!!.getSharedPreferences("survey_sdk_data", Context.MODE_PRIVATE)
                        .edit().putString(key, value).apply()
                    result.success(true)
                } else {
                    result.success(false)
                }
            }

            "setSessionData" -> {
                val key = call.argument<String>("key")
                val value = call.argument<String>("value")
                if (key != null && value != null) {
                    try {
                        SurveySDK.getInstance().setSessionData(key, value)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SESSION_ERROR", e.message, null)
                    }
                } else {
                    result.error("INVALID_ARGS", "Args missing", null)
                }
            }

            "resetSessionData" -> {
                try {
                    SurveySDK.getInstance().resetSessionData()
                    result.success(true)
                } catch (e: Exception) {
                    result.error("SESSION_ERROR", e.message, null)
                }
            }

            "trackEvent" -> {
                // Core SDK'da trackEvent public değilse veya otomatikse burası loglama yapar
                val eventName = call.argument<String>("eventName")
                val properties = call.argument<Map<String, Any>>("properties")
                Log.d("SurveySDKFlutter", "Track Event: $eventName, Props: $properties")
                // Eğer Core SDK'da public bir trackEvent varsa:
                // SurveySDK.getInstance().trackEvent(eventName, properties)
                result.success(true)
            }

            // =====================================================================
            // 📊 5. STATUS & DEBUGGING
            // =====================================================================
            
            "getDebugStatus" -> {
                try {
                    val status = SurveySDK.getInstance().debugSurveyStatus()
                    result.success(status)
                } catch (e: Exception) {
                    result.success("Error: ${e.message}")
                }
            }

            "getSurveyIds" -> {
                try {
                    val ids = SurveySDK.getInstance().getSurveyIds()
                    result.success(ids)
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }

            "isSDKEnabled" -> {
                try {
                    result.success(SurveySDK.getInstance().isSDKEnabled())
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }

            "isConfigurationLoaded" -> {
                try {
                    result.success(SurveySDK.getInstance().isConfigurationLoaded())
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }
            
            "getQueueStatus" -> {
                try {
                    result.success(SurveySDK.getInstance().getQueueStatus())
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }
            
            "isShowingSurvey" -> {
                try {
                    result.success(SurveySDK.getInstance().isShowingSurvey())
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }

            // =====================================================================
            // 🧹 6. CLEANUP & EXCLUSIONS
            // =====================================================================

            "isUserExcluded" -> {
                try {
                    result.success(SurveySDK.getInstance().isUserExcluded())
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }

            "isUserExcludedForSurvey" -> {
                val surveyId = call.argument<String>("surveyId")
                if (surveyId != null) {
                    try {
                        result.success(SurveySDK.getInstance().isUserExcluded(surveyId))
                    } catch (e: Exception) {
                        result.error("ERROR", e.message, null)
                    }
                } else {
                    result.error("INVALID_ARGS", "Missing ID", null)
                }
            }

            "clearSurveyQueue" -> {
                try {
                    SurveySDK.getInstance().clearSurveyQueue()
                    result.success(true)
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }

            "resetTriggers" -> {
                try {
                    SurveySDK.getInstance().resetTriggers()
                    result.success(true)
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }
            
            "fetchConfiguration" -> {
                try {
                    SurveySDK.getInstance().fetchConfiguration()
                    result.success(true)
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }
            
            "getConfigForDebug" -> {
                try {
                    result.success(SurveySDK.getInstance().getConfigForDebug())
                } catch (e: Exception) {
                    result.error("ERROR", e.message, null)
                }
            }

            "cleanup" -> {
                try { 
                    SurveySDK.getInstance().cleanup()
                    result.success(true) 
                } catch (e: Exception) { 
                    result.success(false) 
                }
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