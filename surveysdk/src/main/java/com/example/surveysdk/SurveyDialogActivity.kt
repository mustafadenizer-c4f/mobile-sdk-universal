package com.example.surveysdk

import android.app.Dialog
import android.os.Bundle
import android.webkit.WebView
import android.util.Log
import android.webkit.WebChromeClient
import android.view.Window
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity

class SurveyDialogActivity : AppCompatActivity() {

    private lateinit var dialog: Dialog
    private lateinit var backPressHandler: BackPressHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surveyUrl = intent.getStringExtra("SURVEY_URL") ?: return
        val backgroundColor = intent.getStringExtra("BACKGROUND_COLOR") ?: "#FFFFFF"
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "fade"
        val allowedDomain = intent.getStringExtra("ALLOWED_DOMAIN")

        Log.d("SurveyDialog", "Loading URL in dialog: $surveyUrl")

        AnimationUtils.applyEnterTransition(this, animationType)
        setupBackPressHandler(animationType)
        createDialog(surveyUrl, backgroundColor, allowedDomain, animationType)
    }

    private fun setupBackPressHandler(animationType: String) {
        backPressHandler = BackPressHandler(this, "SurveyDialog") {
            Log.d("SurveyDialog", "Back gesture detected")
            if (::dialog.isInitialized && dialog.isShowing) {
                AnimationUtils.applyExitTransition(this@SurveyDialogActivity, animationType)
                dialog.dismiss()
            } else {
                AnimationUtils.applyExitTransition(this@SurveyDialogActivity, animationType)
                finish()
            }
        }
        backPressHandler.enable()
    }

    private fun createDialog(url: String, backgroundColor: String, allowedDomain: String?, animationType: String) {
        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val webView = WebView(this)
        setupWebView(webView, url, allowedDomain)

        try {
            webView.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor))
        } catch (e: Exception) {
            webView.setBackgroundColor(android.graphics.Color.WHITE)
        }

        dialog.setContentView(webView)

        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt()
        val height = (displayMetrics.heightPixels * 0.8).toInt()

        dialog.window?.setLayout(width, height)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        dialog.setOnDismissListener {
            AnimationUtils.applyExitTransition(this, animationType)
            finish()
        }

        dialog.show()
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
        Log.d("SurveyDialog", "Physical back button pressed")
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "fade"
        if (::dialog.isInitialized && dialog.isShowing) {
            AnimationUtils.applyExitTransition(this, animationType)
            dialog.dismiss()
        } else {
            super.onBackPressed()
        }
    }

    override fun finish() {
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "fade"
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