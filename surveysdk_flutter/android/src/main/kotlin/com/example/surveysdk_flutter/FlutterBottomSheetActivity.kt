package com.example.surveysdk_flutter

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.example.surveysdk.SurveyBottomSheetFragment

class FlutterBottomSheetActivity : FragmentActivity() {
    
    companion object {
        const val EXTRA_SURVEY_URL = "survey_url"
        const val EXTRA_BACKGROUND_COLOR = "background_color"
        const val EXTRA_ALLOWED_DOMAIN = "allowed_domain"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FlutterBottomSheetActivity", "📱 Creating bottom sheet wrapper for Flutter")
        
        val surveyUrl = intent.getStringExtra(EXTRA_SURVEY_URL) ?: return
        val backgroundColor = intent.getStringExtra(EXTRA_BACKGROUND_COLOR) ?: "#FFFFFF"
        val allowedDomain = intent.getStringExtra(EXTRA_ALLOWED_DOMAIN)
        
        try {
            // Use SurveyBottomSheetFragment from your existing SDK
            val bottomSheetFragment = SurveyBottomSheetFragment.newInstance(
                surveyUrl = surveyUrl,
                backgroundColor = backgroundColor,
                allowedDomain = allowedDomain
            )
            
            // Fix: Use the correct show method
            if (supportFragmentManager.findFragmentByTag("FlutterBottomSheetFragment") == null) {
                bottomSheetFragment.show(supportFragmentManager, "FlutterBottomSheetFragment")
            }
            
            Log.d("FlutterBottomSheetActivity", "✅ Bottom sheet shown for Flutter")
        } catch (e: Exception) {
            Log.e("FlutterBottomSheetActivity", "❌ Error: ${e.message}")
            finish()
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        try {
            com.example.surveysdk.SurveySDK.getInstance().surveyCompleted()
        } catch (e: Exception) {
            Log.e("FlutterBottomSheetActivity", "Error notifying completion: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            com.example.surveysdk.SurveySDK.getInstance().surveyCompleted()
        } catch (e: Exception) {
            Log.e("FlutterBottomSheetActivity", "Error in onDestroy: ${e.message}")
        }
    }
}