package org.zotero.android.citation

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import javax.inject.Inject

class CitationWebViewHandler @Inject constructor(
    dispatchers: Dispatchers,
    private val context: Context,
    private val gson: Gson,
) {
    private val uiMainCoroutineScope = CoroutineScope(dispatchers.main)
    private var wasPageAlreadyFullyLoaded: Boolean = false

    private lateinit var webView: WebView
    private lateinit var webViewPort: WebMessagePort

    var cookies: String? = null
    var userAgent: String? = null
    var referrer: String? = null

    fun set(cookies: String, userAgent: String, referrer: String) {
        this.cookies = cookies
        this.userAgent = userAgent
        this.referrer = referrer

    }

    fun load(url: String, onWebViewLoadPage: () -> Unit, processWebViewResponses: ((message: WebMessage) -> Unit)? = null) {
        uiMainCoroutineScope.launch {
            webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.settings.allowContentAccess = true
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d(
                        "WEBVIEW_LOG_TAG",
                        consoleMessage.message() + " -- From line "
                                + consoleMessage.lineNumber() + " of "
                                + consoleMessage.sourceId()
                    )
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
                    //Fix for onPageFinished getting called twice for some webpages
                    if (wasPageAlreadyFullyLoaded || view.progress != 100) {
                        return
                    }
                    wasPageAlreadyFullyLoaded = true
                    val channel = webView.createWebMessageChannel()
                    val port = channel[0]
                    this@CitationWebViewHandler.webViewPort = port
                    port.setWebMessageCallback(object :
                        WebMessagePort.WebMessageCallback() {
                        override fun onMessage(port: WebMessagePort, message: WebMessage) {
                            processWebViewResponses?.invoke(message)
                        }
                    })
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