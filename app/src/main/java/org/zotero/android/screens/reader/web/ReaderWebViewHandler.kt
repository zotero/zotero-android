package org.zotero.android.screens.reader.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import timber.log.Timber
import javax.inject.Inject

@ViewModelScoped
class ReaderWebViewHandler @Inject constructor(
    dispatchers: Dispatchers,
    private val context: Context,
    private val fileStore: FileStore,
) {
    private val uiMainCoroutineScope = CoroutineScope(dispatchers.main)

    private lateinit var webView: WebView
    private lateinit var webViewPort: WebMessagePort

    private class PendingJavascriptResult(val callback: (String) -> Unit)

    private val pendingJavascriptResults = mutableSetOf<PendingJavascriptResult>()
    private var isRenderProcessGone = false

    @SuppressLint("SetJavaScriptEnabled")
    fun load(
        webView: WebView,
        url: String,
        onWebViewLoadPage: () -> Unit,
        processWebViewResponses: ((message: WebMessage) -> Unit)? = null,
        onRendererGone: ((didCrash: Boolean) -> Unit)? = null,
    ) {
        this.webView = webView
        uiMainCoroutineScope.launch {
            isRenderProcessGone = false
            failPendingJavascriptResults()
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.settings.allowContentAccess = true
            webView.settings.javaScriptCanOpenWindowsAutomatically = true
            webView.setFocusable(true);
            webView.setFocusableInTouchMode(true);
            webView.requestFocus()

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
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/local/", WebViewAssetLoader.InternalStoragePathHandler(context, fileStore.runningReaderDirectory()))
                .build()
            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    val url = request.url ?: return null
                    return assetLoader.shouldInterceptRequest(url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }

                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail
                ): Boolean {
                    if (view !== this@ReaderWebViewHandler.webView) {
                        return true
                    }
                    Timber.e("ReaderWebViewHandler: render process gone, didCrash=${detail.didCrash()}")
                    isRenderProcessGone = true
                    failPendingJavascriptResults()
                    onRendererGone?.invoke(detail.didCrash())
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    webView.requestFocus()
                    val channel = webView.createWebMessageChannel()
                    val port = channel[0]
                    this@ReaderWebViewHandler.webViewPort = port
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
            if (isRenderProcessGone) {
                result("null")
                return@launch
            }
            val pending = PendingJavascriptResult(result)
            pendingJavascriptResults.add(pending)
            webView.evaluateJavascript(javascript) { value ->
                if (pendingJavascriptResults.remove(pending)) {
                    result(value)
                }
            }
        }
    }

    private fun failPendingJavascriptResults() {
        val pending = pendingJavascriptResults.toList()
        pendingJavascriptResults.clear()
        pending.forEach { it.callback("null") }
    }

}