package com.example.surveysdk

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.util.Log
import android.view.View
import android.webkit.WebResourceResponse
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object WebViewConfigurator {

    // UPDATED METHOD SIGNATURE - With callbacks
    fun setupSecureWebView(
        webView: WebView,
        url: String,
        allowedDomain: String?,
        onPageStarted: (() -> Unit)? = null,
        onPageFinished: (() -> Unit)? = null,
        onSurveyClosed: (() -> Unit)? = null  // ✅ ADD THIS: Callback for when survey is closed
    ) {
        configureWebViewSettings(webView)
        webView.webViewClient = createSafeWebViewClient(allowedDomain, webView, url, onPageStarted, onPageFinished, onSurveyClosed)
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(url)
    }

    // ADD THIS METHOD - WebView settings configuration
    private fun configureWebViewSettings(webView: WebView) {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }
    }

    // UPDATED METHOD - With callbacks
    private fun createSafeWebViewClient(
        allowedDomain: String?,
        webView: WebView,
        originalUrl: String,
        onPageStarted: (() -> Unit)? = null,
        onPageFinished: (() -> Unit)? = null,
        onSurveyClosed: (() -> Unit)? = null  // ✅ ADD THIS
    ): WebViewClient {
        return object : WebViewClient() {
            private var errorOccurred = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                Log.d("WebViewConfigurator", "🚀 Starting to load: $url")
                errorOccurred = false // ⭐⭐⭐ RESET ERROR STATE ⭐⭐⭐
                hideErrorLayout(webView) // ⭐⭐⭐ HIDE ANY PREVIOUS ERROR ⭐⭐⭐
                onPageStarted?.invoke()
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                if (errorOccurred) return
                hideErrorLayout(webView)
                onPageFinished?.invoke() // Call the callback
                Log.d("WebViewConfigurator", "✅ Page loaded successfully: $loadedUrl")
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                errorOccurred = true
                Log.e("WebViewConfigurator", "❌ Page load error: $description")
                Log.e("WebViewConfigurator", "❌ Error code: $errorCode")
                Log.e("WebViewConfigurator", "❌ Failing URL: $failingUrl")
                Log.e("WebViewConfigurator", "❌ Original URL: $originalUrl")
                showErrorLayout(webView, originalUrl, onSurveyClosed)  // ✅ PASS CALLBACK
            }

            // Add HTTP error handling for API 21+
            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Log.e("WebViewConfigurator", "❌ HTTP Error: ${errorResponse?.statusCode}")
                    Log.e("WebViewConfigurator", "❌ HTTP Error URL: ${request?.url}")
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                val shouldAllow = isUrlAllowed(url, allowedDomain)

                Log.d("WebViewConfigurator", "🔗 Navigation attempt: $url")
                Log.d("WebViewConfigurator", "🔗 Allowed: $shouldAllow")

                if (shouldAllow) {
                    errorOccurred = false
                    return false
                } else {
                    Log.w("WebViewConfigurator", "🚫 Blocked navigation to: $url")
                    return true
                }
            }
        }
    }

    // ✅ UPDATED: Accept onSurveyClosed callback
    private fun showErrorLayout(webView: WebView, originalUrl: String, onSurveyClosed: (() -> Unit)? = null) {
        webView.post {
            try {
                val context = webView.context

                // Create error layout
                val errorLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(0xFFFFFFFF.toInt())
                    setPadding(50, 100, 50, 100)
                    gravity = android.view.Gravity.CENTER
                }

                // Error icon
                val errorIcon = TextView(context).apply {
                    text = "⚠️"
                    textSize = 48f
                    gravity = android.view.Gravity.CENTER
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 20
                    }
                }

                // Error title
                val errorText = TextView(context).apply {
                    text = "Survey Loading Failed"
                    textSize = 18f
                    setTextColor(0xFF333333.toInt())
                    gravity = android.view.Gravity.CENTER
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 10
                    }
                }

                // Error description
                val errorDesc = TextView(context).apply {
                    text = "Please check your internet connection and try again."
                    textSize = 14f
                    setTextColor(0xFF666666.toInt())
                    gravity = android.view.Gravity.CENTER
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 30
                    }
                }

                // Retry button
                val retryButton = Button(context).apply {
                    text = "Retry Survey"
                    setBackgroundColor(0xFF007AFF.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                    setPadding(40, 20, 40, 20)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                    setOnClickListener {
                        Log.d("WebViewConfigurator", "🔄 Retry button clicked")
                        hideErrorLayout(webView)
                        webView.loadUrl(originalUrl)
                    }
                }

                // Close button
                val closeButton = Button(context).apply {
                    text = "Close"
                    setBackgroundColor(0xFF8E8E93.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                    setPadding(40, 20, 40, 20)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                        topMargin = 10
                    }
                    setOnClickListener {
                        Log.d("WebViewConfigurator", "❌ Close button clicked")
                        hideErrorLayout(webView)
                        // Dismiss the survey using callback instead of direct call
                        onSurveyClosed?.invoke() ?: dismissSurvey(webView)
                    }
                }

                // Add views to error layout
                errorLayout.addView(errorIcon)
                errorLayout.addView(errorText)
                errorLayout.addView(errorDesc)
                errorLayout.addView(retryButton)
                errorLayout.addView(closeButton)

                // Replace WebView with error layout
                val parent = webView.parent as? android.view.ViewGroup
                parent?.let {
                    // Store reference to webView for later restoration
                    webView.tag = "survey_webview"
                    it.removeView(webView)
                    it.addView(errorLayout)
                    errorLayout.tag = "error_layout" // Tag for easy identification
                }

                Log.d("WebViewConfigurator", "🔄 Error layout shown with retry/close buttons")

            } catch (e: Exception) {
                Log.e("WebViewConfigurator", "❌ Error showing error layout: ${e.message}")
            }
        }
    }

    private fun hideErrorLayout(webView: WebView) {
        webView.post {
            try {
                val parent = webView.parent as? android.view.ViewGroup
                parent?.let {
                    // Remove error layout if present
                    val errorLayout = it.findViewWithTag<LinearLayout>("error_layout")
                    errorLayout?.let { errorView ->
                        it.removeView(errorView)
                        Log.d("WebViewConfigurator", "✅ Error layout removed")
                    }

                    // Restore WebView if not already present
                    if (it.indexOfChild(webView) == -1) {
                        it.addView(webView)
                        Log.d("WebViewConfigurator", "✅ WebView restored")
                    }
                }
            } catch (e: Exception) {
                Log.e("WebViewConfigurator", "❌ Error hiding error layout: ${e.message}")
            }
        }
    }

    // ✅ UPDATED: Remove the surveyCompleted() call to prevent duplicates
    private fun dismissSurvey(webView: WebView) {
        try {
            val context = webView.context
            when (context) {
                is androidx.fragment.app.DialogFragment -> {
                    context.dismiss()
                    Log.d("WebViewConfigurator", "✅ DialogFragment dismissed")
                }
                is com.google.android.material.bottomsheet.BottomSheetDialogFragment -> {
                    context.dismiss()
                    Log.d("WebViewConfigurator", "✅ BottomSheetFragment dismissed")
                }
                is android.app.Activity -> {
                    context.finish()
                    Log.d("WebViewConfigurator", "✅ Activity finished")
                }
                else -> {
                    Log.w("WebViewConfigurator", "⚠️ Unknown context type, cannot dismiss")
                }
            }
            // ❌ REMOVED: Don't call surveyCompleted() here - let the activity handle it
        } catch (e: Exception) {
            Log.e("WebViewConfigurator", "❌ Error dismissing survey: ${e.message}")
        }
    }

    private fun isUrlAllowed(url: String, allowedDomain: String?): Boolean {
        if (allowedDomain.isNullOrEmpty()) return true

        return try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host ?: return false
            host == allowedDomain || host.endsWith(".$allowedDomain")
        } catch (e: Exception) {
            false
        }
    }
}