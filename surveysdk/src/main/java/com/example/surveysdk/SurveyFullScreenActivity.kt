package com.example.surveysdk

import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.util.Log
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity

class SurveyFullScreenActivity : AppCompatActivity() {

    private lateinit var backPressHandler: BackPressHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surveyUrl = intent.getStringExtra("SURVEY_URL") ?: return
        val backgroundColor = intent.getStringExtra("BACKGROUND_COLOR") ?: "#FFFFFF"
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
        val allowedDomain = intent.getStringExtra("ALLOWED_DOMAIN")

        Log.d("SurveyFullScreen", "Loading URL: $surveyUrl")

        val layoutRes = resources.getIdentifier("activity_survey_webview", "layout", packageName)
        if (layoutRes != 0) {
            setContentView(layoutRes)
        } else {
            createFallbackLayout()
            return
        }

        val webView = findViewById<WebView>(resources.getIdentifier("webView", "id", packageName))
        val closeButton = findViewById<Button>(resources.getIdentifier("closeButton", "id", packageName))

        setupBackPressHandler(animationType)
        AnimationUtils.applyEnterTransition(this, animationType)

        val rootLayoutId = resources.getIdentifier("rootLayout", "id", packageName)
        if (rootLayoutId != 0) {
            try {
                findViewById<android.widget.LinearLayout>(rootLayoutId)
                    .setBackgroundColor(android.graphics.Color.parseColor(backgroundColor))
            } catch (e: Exception) {
                // Use default background
            }
        }

        setupWebView(webView, surveyUrl, allowedDomain)

        closeButton?.setOnClickListener {
            AnimationUtils.applyExitTransition(this, animationType)
            finish()
        }
    }

    private fun createFallbackLayout() {
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val headerLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(50, 30, 50, 30)
            setBackgroundColor(0xFFF0F0F0.toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val closeButton = Button(this).apply {
            text = "Close Survey"
            setBackgroundColor(0xFFFF6B6B.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(40, 20, 40, 20)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
                AnimationUtils.applyExitTransition(this@SurveyFullScreenActivity, animationType)
                finish()
            }
        }

        val titleText = android.widget.TextView(this).apply {
            text = "Survey"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }

        headerLayout.addView(closeButton)
        headerLayout.addView(titleText)

        val webView = WebView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            id = android.view.View.generateViewId()
        }

        linearLayout.addView(headerLayout)
        linearLayout.addView(webView)

        setContentView(linearLayout)

        val surveyUrl = intent.getStringExtra("SURVEY_URL") ?: return
        val allowedDomain = intent.getStringExtra("ALLOWED_DOMAIN")
        setupWebView(webView, surveyUrl, allowedDomain)

        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
        setupBackPressHandler(animationType)
        AnimationUtils.applyEnterTransition(this, animationType)
    }

    private fun setupBackPressHandler(animationType: String) {
        backPressHandler = BackPressHandler(this, "SurveyFullScreen") {
            Log.d("SurveyFullScreen", "Back gesture detected")
            AnimationUtils.applyExitTransition(this@SurveyFullScreenActivity, animationType)
            finish()
        }
        backPressHandler.enable()
    }

    private fun setupWebView(webView: WebView, url: String, allowedDomain: String?) {
        // CORRECTED: Removed invalid parameters
        WebViewConfigurator.setupSecureWebView(
            webView = webView,
            url = url,
            allowedDomain = allowedDomain
        )
    }

    override fun onBackPressed() {
        Log.d("SurveyFullScreen", "Physical back button pressed")
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
        AnimationUtils.applyExitTransition(this, animationType)
        super.onBackPressed()
    }

    override fun finish() {
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "none"
        AnimationUtils.applyExitTransition(this, animationType)
        super.finish()
    }

    override fun onDestroy() {
        // Cancel any pending delays for this specific activity
        SafeDelayExecutor.cancelDelayed("activity_${this.hashCode()}")

        if (::backPressHandler.isInitialized) {
            backPressHandler.disable()
        }
        super.onDestroy()
    }
}