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

    // UPDATED METHOD SIGNATURE - Removed callback parameters
    fun setupSecureWebView(
        webView: WebView,
        url: String,
        allowedDomain: String?
    ) {
        configureWebViewSettings(webView)
        webView.webViewClient = createSafeWebViewClient(allowedDomain, webView, url)
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(url)
    }

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

    private fun createSafeWebViewClient(
        allowedDomain: String?,
        webView: WebView,
        originalUrl: String
    ): WebViewClient {
        return object : WebViewClient() {
            private var errorOccurred = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                Log.d("WebViewConfigurator", "🚀 Starting to load: $url")
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                if (errorOccurred) return
                hideErrorLayout(webView)
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
                showErrorLayout(webView, originalUrl)
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

    private fun showErrorLayout(webView: WebView, originalUrl: String) {
        webView.post {
            val context = webView.context

            val errorLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xFFFFFFFF.toInt())
                setPadding(50, 100, 50, 100)
            }

            val errorText = TextView(context).apply {
                text = "Survey Loading Failed"
                textSize = 18f
                setTextColor(0xFF333333.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 20
                }
            }

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

            val retryButton = Button(context).apply {
                text = "Retry"
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
                    hideErrorLayout(webView)
                    webView.loadUrl(originalUrl)
                }
            }

            errorLayout.addView(errorText)
            errorLayout.addView(errorDesc)
            errorLayout.addView(retryButton)

            val parent = webView.parent as? android.view.ViewGroup
            parent?.removeView(webView)
            parent?.addView(errorLayout)
        }
    }

    private fun hideErrorLayout(webView: WebView) {
        webView.post {
            val parent = webView.parent as? android.view.ViewGroup
            parent?.let {
                for (i in 0 until it.childCount) {
                    val child = it.getChildAt(i)
                    if (child is LinearLayout && child != webView) {
                        it.removeView(child)
                    }
                }
                if (it.indexOfChild(webView) == -1) {
                    it.addView(webView)
                }
            }
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