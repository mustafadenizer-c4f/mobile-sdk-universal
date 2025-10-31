package com.example.surveysdk

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SurveyDialogActivity : AppCompatActivity() {

    private lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make THIS activity completely invisible/transparent
        makeActivityInvisible()

        val surveyUrl = intent.getStringExtra("SURVEY_URL") ?: return
        val backgroundColor = intent.getStringExtra("BACKGROUND_COLOR") ?: "#FFFFFF"
        val animationType = intent.getStringExtra("ANIMATION_TYPE") ?: "fade"
        val allowedDomain = intent.getStringExtra("ALLOWED_DOMAIN")

        Log.d("SurveyDialog", "Creating invisible activity with dialog overlay")

        createFloatingDialog(surveyUrl, backgroundColor, allowedDomain)
    }

    private fun makeActivityInvisible() {
        // Make the entire activity window transparent and invisible
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setDimAmount(0.0f)
        window.decorView.setBackgroundColor(Color.TRANSPARENT)

        // Remove all window decorations
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()

        // Make the activity itself invisible
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
    }

    private fun createFloatingDialog(url: String, backgroundColor: String, allowedDomain: String?) {
        // Create a completely custom dialog that floats over the app
        dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)

        // Configure the dialog window to be transparent and non-blocking
        dialog.window?.apply {
            // Make dialog background completely transparent
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.0f)

            // Remove all dimming and blocking
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

            // Set layout parameters for floating dialog
            val layoutParams = WindowManager.LayoutParams().apply {
                copyFrom(attributes)
                width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                height = (resources.displayMetrics.heightPixels * 0.8).toInt()
                gravity = Gravity.CENTER
                flags = flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            attributes = layoutParams
        }

        // Create the content view
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Create WebView
        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Add some rounded corners for better appearance
            clipToOutline = true
        }

        // Create close button
        val closeButton = TextView(this).apply {
            text = "✕"
            setTextColor(Color.BLACK)
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = 20
                marginEnd = 20
            }
            setOnClickListener {
                dialog.dismiss()
                finish()
            }
            setPadding(30, 20, 30, 20)
            setBackgroundColor(0xEEFFFFFF.toInt()) // Semi-transparent white
        }

        container.addView(webView)
        container.addView(closeButton)
        dialog.setContentView(container)

        // Setup WebView
        WebViewConfigurator.setupSecureWebView(webView, url, allowedDomain)

        // Set WebView background
        try {
            webView.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor))
        } catch (e: Exception) {
            webView.setBackgroundColor(Color.WHITE)
        }

        // Bring close button to front
        closeButton.bringToFront()

        // Set dismiss listener
        dialog.setOnDismissListener {
            finish()
        }

        // Show the dialog
        dialog.show()

        Log.d("SurveyDialog", "Floating dialog shown")
    }

    override fun onBackPressed() {
        Log.d("SurveyDialog", "Back pressed - dismissing dialog")
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
        finish()
    }

    override fun onDestroy() {
        SafeDelayExecutor.cancelDelayed("activity_${this.hashCode()}")
        super.onDestroy()
    }
}