package com.example.surveysdk

import android.os.Bundle
import android.webkit.WebView
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SurveyFullScreenActivity : AppCompatActivity() {

    private lateinit var backPressHandler: BackPressHandler
    private lateinit var closeButton: TextView
    private lateinit var webView: WebView
    private var surveyCompletedNotified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surveyUrl = intent.getStringExtra("SURVEY_URL") ?: return
        val backgroundColor = intent.getStringExtra("BACKGROUND_COLOR") ?: "#FFFFFF"
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
        val allowedDomain = intent.getStringExtra("ALLOWED_DOMAIN")
        val surveyId = intent.getStringExtra("SURVEY_ID") ?: "unknown"

        Log.d("SurveyFullScreen", "Loading survey: $surveyId")
        Log.d("SurveyFullScreen", "URL: $surveyUrl")

        removeActivityTitle()
        createFrameLayout(backgroundColor)
        setupBackPressHandler(animationType)
        AnimationUtils.applyEnterTransition(this, animationType)

        setupWebView(webView, surveyUrl, allowedDomain)
    }

    private fun removeActivityTitle() {
        try {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        } catch (e: Exception) {
            // Ignore if already set
        }
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.title = ""
        setTitle("")
        window.setFeatureInt(android.view.Window.FEATURE_CUSTOM_TITLE, android.view.Window.FEATURE_NO_TITLE)

        // Ensure no dim background
        window.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun createFrameLayout(backgroundColor: String) {
        // Create main FrameLayout
        val mainLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Create WebView first (background)
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create simple bold X (TextView)
        closeButton = TextView(this).apply {
            text = "✕"
            setTextColor(0xFF333333.toInt())
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = 40
                marginEnd = 20
            }
            setOnClickListener {
                handleSurveyClose()
            }
            setPadding(20, 20, 20, 20)

            // Add background to close button
            setBackgroundColor(0xCCFFFFFF.toInt())
        }

        // Add views to layout
        mainLayout.addView(webView)
        mainLayout.addView(closeButton)

        // Set background color for webview only
        try {
            webView.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor))
        } catch (e: Exception) {
            webView.setBackgroundColor(android.graphics.Color.WHITE)
        }

        setContentView(mainLayout)
        closeButton.bringToFront()
    }

    private fun setupBackPressHandler(animationType: String) {
        backPressHandler = BackPressHandler(this, "SurveyFullScreen") {
            Log.d("SurveyFullScreen", "Back gesture detected")
            handleSurveyClose()
        }
        backPressHandler.enable()
    }

    private fun setupWebView(webView: WebView, url: String, allowedDomain: String?) {
        WebViewConfigurator.setupSecureWebView(
            webView = webView,
            url = url,
            allowedDomain = allowedDomain,
            onSurveyClosed = {
                // This will be called when the "Close" button in error layout is clicked
                handleSurveyClose()
            }
        )
    }

    private fun handleSurveyClose() {
        if (surveyCompletedNotified) {
            return // Already notified, prevent duplicate calls
        }

        surveyCompletedNotified = true
        Log.d("SurveyFullScreen", "Survey closed by user")

        // Notify SDK that survey completed
        com.example.surveysdk.SurveySDK.getInstance().surveyCompleted()

        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
        AnimationUtils.applyExitTransition(this, animationType)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("SurveyFullScreen", "Physical back button pressed")
        handleSurveyClose()
    }

    override fun finish() {
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
        AnimationUtils.applyExitTransition(this, animationType)
        super.finish()
    }

    override fun onDestroy() {
        SafeDelayExecutor.cancelDelayed("activity_${this.hashCode()}")

        if (::backPressHandler.isInitialized) {
            backPressHandler.disable()
        }

        // ✅ SIMPLEST FIX:
        if (::webView.isInitialized) {
            try {
                webView.stopLoading()
                webView.clearCache(true)
                // Don't set webViewClient or webChromeClient - Android will handle them
            } catch (e: Exception) {
                Log.e("SurveyFullScreen", "Error in WebView cleanup: ${e.message}")
            }
        }

        if (!surveyCompletedNotified && !isChangingConfigurations) {
            Log.d("SurveyFullScreen", "Activity destroyed without proper close")
            com.example.surveysdk.SurveySDK.getInstance().surveyCompleted()
        }

        super.onDestroy()
        Log.d("SurveySDK", "🔄 Notifying SDK of survey completion")
        com.example.surveysdk.SurveySDK.getInstance().surveyCompleted()
    }
}