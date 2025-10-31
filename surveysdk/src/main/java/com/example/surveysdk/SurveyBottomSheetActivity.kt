package com.example.surveysdk

import android.os.Bundle
import android.webkit.WebView
import android.util.Log
import android.view.View
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

        // Make activity transparent
        makeActivityTransparent()

        AnimationUtils.applyEnterTransition(this, animationType)
        setupBackPressHandler(animationType)
        createBottomSheet(surveyUrl, backgroundColor, allowedDomain, animationType)
    }

    private fun makeActivityTransparent() {
        // Make window completely transparent
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setDimAmount(0.0f)

        // Remove any window decorations
        supportActionBar?.hide()
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
        // Create bottom sheet with regular theme but remove dim manually
        bottomSheetDialog = BottomSheetDialog(this)

        // COMPLETELY remove dim background
        bottomSheetDialog.window?.setDimAmount(0.0f)
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val webView = WebView(this)
        setupWebView(webView, url, allowedDomain)

        try {
            webView.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor))
        } catch (e: Exception) {
            webView.setBackgroundColor(android.graphics.Color.WHITE)
        }

        bottomSheetDialog.setContentView(webView)
        bottomSheetDialog.setCancelable(true)

        // Configure bottom sheet behavior
        bottomSheetDialog.behavior.isDraggable = true
        bottomSheetDialog.behavior.isFitToContents = false
        bottomSheetDialog.behavior.halfExpandedRatio = 0.7f
        bottomSheetDialog.behavior.peekHeight = 800

        // Remove the scrim (dim) completely by making it transparent
        removeBottomSheetDim()

        bottomSheetDialog.setOnDismissListener {
            AnimationUtils.applyExitTransition(this, animationType)
            finish()
        }

        bottomSheetDialog.show()

        // Ensure no dimming after showing
        bottomSheetDialog.window?.setDimAmount(0.0f)
    }

    private fun removeBottomSheetDim() {
        try {
            // Method 1: Remove the dim view directly
            val dimView = bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.touch_outside)
            dimView?.setBackgroundResource(android.R.color.transparent)

            // Method 2: Remove dim from the bottom sheet container
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)

            // Method 3: Use window flags to remove dim
            bottomSheetDialog.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        } catch (e: Exception) {
            Log.e("SurveyBottomSheet", "Failed to remove bottom sheet dim: ${e.message}")
        }
    }

    private fun setupWebView(webView: WebView, url: String, allowedDomain: String?) {
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
        SafeDelayExecutor.cancelDelayed("activity_${this.hashCode()}")
        if (::backPressHandler.isInitialized) {
            backPressHandler.disable()
        }
        super.onDestroy()
    }
}