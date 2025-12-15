package com.example.surveysdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SurveyForegroundActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SurveyForeground", "Foreground activity created")

        // Make window transparent
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Immediately start the actual survey
        startSurvey()
    }

    private fun startSurvey() {
        try {
            val surveyUrl = intent.getStringExtra("SURVEY_URL") ?: return
            val backgroundColor = intent.getStringExtra("BACKGROUND_COLOR") ?: "#FFFFFF"
            val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
            val allowedDomain = intent.getStringExtra("ALLOWED_DOMAIN")

            Log.d("SurveyForeground", "Starting survey from foreground")

            val surveyIntent = Intent(this, SurveyFullScreenActivity::class.java).apply {
                putExtra("SURVEY_URL", surveyUrl)
                putExtra("BACKGROUND_COLOR", backgroundColor)
                putExtra("ANIMATION_TYPE", animationType)
                putExtra("ALLOWED_DOMAIN", allowedDomain)
                // Add the original calling activity info
                putExtra("CALLING_ACTIVITY", intent.getStringExtra("CALLING_ACTIVITY"))
            }

            startActivityForResult(surveyIntent, REQUEST_SURVEY)
            // Don't finish here - wait for result

        } catch (e: Exception) {
            Log.e("SurveyForeground", "Error starting survey: ${e.message}")
            completeSurvey()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SURVEY) {
            Log.d("SurveyForeground", "Survey completed with result: $resultCode")
            completeSurvey()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("SurveyForeground", "New intent received")
        completeSurvey()
    }

    private fun completeSurvey() {
        Log.d("SurveyForeground", "Completing survey foreground activity")
        // Notify SDK that survey flow is complete
        SurveySDK.getInstance().surveyCompleted()
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("SurveyForeground", "Back pressed in foreground activity")
        completeSurvey()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SurveyForeground", "Foreground activity destroyed")
    }

    companion object {
        private const val REQUEST_SURVEY = 1001
    }
}