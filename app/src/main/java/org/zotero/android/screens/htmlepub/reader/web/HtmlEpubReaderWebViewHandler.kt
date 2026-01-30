package org.zotero.android.screens.htmlepub.reader.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import timber.log.Timber

class HtmlEpubReaderWebViewHandler(
    dispatchers: Dispatchers,
    private val context: Context,
    private val webView: WebView
) {
    private val uiMainCoroutineScope = CoroutineScope(dispatchers.main)

    private lateinit var webViewPort: WebMessagePort

    var cookies: String? = null
    var userAgent: String? = null
    var referrer: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun load(url: String, onWebViewLoadPage: () -> Unit, processWebViewResponses: ((message: WebMessage) -> Unit)? = null,) {
        uiMainCoroutineScope.launch {
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.settings.allowContentAccess = true

            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    val log = ("WEBVIEW_LOG_TAG:" +
                            consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of "
                            + consoleMessage.sourceId())
                    Timber.d(log)
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    val channel = webView.createWebMessageChannel()
                    val port = channel[0]
                    this@HtmlEpubReaderWebViewHandler.webViewPort = port
                    port.setWebMessageCallback(object :
                        WebMessagePort.WebMessageCallback() {
                        override fun onMessage(port: WebMessagePort, message: WebMessage) {
                            processWebViewResponses?.invoke(message)
                        }
                    })

                    println()
                    //Passing to JS code the handle to our onMessage listener on kotlin side
                    webView.postWebMessage(
                        WebMessage("initPort", arrayOf(channel[1])),
                        Uri.EMPTY
                    )
                    onWebViewLoadPage()
                }
            }
            webView.loadUrl(url)
        }

    }

    fun evaluateJavascript(javascript: String, result: (String) -> Unit) {
        uiMainCoroutineScope.launch {
            webView.evaluateJavascript(javascript) { result ->
                result(result)
            }
        }
    }

}