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
            }

            startActivity(surveyIntent)
            finish() // Close this transparent activity

        } catch (e: Exception) {
            Log.e("SurveyForeground", "Error starting survey: ${e.message}")
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
    }
}