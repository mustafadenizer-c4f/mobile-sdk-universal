package com.example.surveysdk

import android.os.Bundle
import android.webkit.WebView
import android.util.Log
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class SurveyBottomSheetActivity : AppCompatActivity() {

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var backPressHandler: BackPressHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surveyUrl = intent.getStringExtra("SURVEY_URL") ?: return
        val backgroundColor = intent.getStringExtra("BACKGROUND_COLOR") ?: "#FFFFFF"
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "slide_up"
        val allowedDomain = intent.getStringExtra("ALLOWED_DOMAIN")

        Log.d("SurveyBottomSheet", "Loading URL in bottom sheet: $surveyUrl")

        AnimationUtils.applyEnterTransition(this, animationType)
        setupBackPressHandler(animationType)
        createBottomSheet(surveyUrl, backgroundColor, allowedDomain, animationType)
    }

    private fun setupBackPressHandler(animationType: String) {
        backPressHandler = BackPressHandler(this, "SurveyBottomSheet") {
            Log.d("SurveyBottomSheet", "Back gesture detected")
            if (::bottomSheetDialog.isInitialized && bottomSheetDialog.isShowing) {
                AnimationUtils.applyExitTransition(this@SurveyBottomSheetActivity, animationType)
                bottomSheetDialog.dismiss()
            } else {
                AnimationUtils.applyExitTransition(this@SurveyBottomSheetActivity, animationType)
                finish()
            }
        }
        backPressHandler.enable()
    }

    private fun createBottomSheet(url: String, backgroundColor: String, allowedDomain: String?, animationType: String) {
        bottomSheetDialog = BottomSheetDialog(this)

        val webView = WebView(this)
        setupWebView(webView, url, allowedDomain)

        try {
            webView.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor))
        } catch (e: Exception) {
            webView.setBackgroundColor(android.graphics.Color.WHITE)
        }

        bottomSheetDialog.setContentView(webView)
        bottomSheetDialog.setCancelable(true)

        bottomSheetDialog.behavior.isDraggable = true
        bottomSheetDialog.behavior.isFitToContents = false
        bottomSheetDialog.behavior.halfExpandedRatio = 0.7f
        bottomSheetDialog.behavior.peekHeight = 800

        bottomSheetDialog.setOnDismissListener {
            AnimationUtils.applyExitTransition(this, animationType)
            finish()
        }

        bottomSheetDialog.show()
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
        Log.d("SurveyBottomSheet", "Physical back button pressed")
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "slide_up"
        if (::bottomSheetDialog.isInitialized && bottomSheetDialog.isShowing) {
            AnimationUtils.applyExitTransition(this, animationType)
            bottomSheetDialog.dismiss()
        } else {
            super.onBackPressed()
        }
    }

    override fun finish() {
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "slide_up"
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