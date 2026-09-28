package org.zotero.android.speech.documentworker

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import timber.log.Timber

class DocumentWorkerWebViewHandler(
    dispatchers: Dispatchers,
    private val context: Context,
) {
    private val uiMainCoroutineScope = CoroutineScope(dispatchers.main)

    private var webView: WebView? = null
    private var webViewPort: WebMessagePort? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun load(
        url: String,
        onWebViewLoadPage: () -> Unit,
        onWebViewFailure: (reason: String) -> Unit,
        processWebViewResponses: (message: WebMessage) -> Unit,
    ) {
        uiMainCoroutineScope.launch {
            val webView = WebView(context)
            this@DocumentWorkerWebViewHandler.webView = webView
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
                    val channel = view.createWebMessageChannel()
                    val port = channel[0]
                    this@DocumentWorkerWebViewHandler.webViewPort = port
                    port.setWebMessageCallback(object :
                        WebMessagePort.WebMessageCallback() {
                        override fun onMessage(port: WebMessagePort, message: WebMessage) {
                            processWebViewResponses(message)
                        }
                    })
                    //Passing to JS code the handle to our onMessage listener on kotlin side
                    view.postWebMessage(
                        WebMessage("initPort", arrayOf(channel[1])),
                        Uri.EMPTY
                    )
                    onWebViewLoadPage()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        onWebViewFailure("failed to load ${request.url}: ${error.description}")
                    }
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail
                ): Boolean {
                    onWebViewFailure("render process gone, didCrash=${detail.didCrash()}")
                    return true
                }
            }
            webView.loadUrl(url)
        }
    }

    fun evaluateJavascript(javascript: String, result: (String) -> Unit) {
        uiMainCoroutineScope.launch {
            webView?.evaluateJavascript(javascript) { evaluationResult ->
                result(evaluationResult)
            }
        }
    }

    fun destroy() {
        uiMainCoroutineScope.launch {
            webViewPort?.close()
            webViewPort = null
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}