package org.zotero.android.screens.addnote.web

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
import com.google.gson.Gson
import dagger.hilt.android.scopes.ViewModelScoped
import org.zotero.android.files.FileStore
import org.zotero.android.translator.helper.TranslatorHelper.encodeAsJSONForJavascript
import timber.log.Timber
import javax.inject.Inject

@ViewModelScoped
internal class NoteEditorWebViewHandler @Inject constructor(
    private val context: Context,
    private val fileStore: FileStore,
    private val gson: Gson,
) {
    private var webView: WebView? = null
    private var port: WebMessagePort? = null
    private var isRenderProcessGone = false

    val isAlive: Boolean
        get() = webView != null && port != null && !isRenderProcessGone

    val showIndentButtons: Boolean
        get() = context.resources.configuration.smallestScreenWidthDp < TABLET_MIN_SMALLEST_WIDTH_DP

    @SuppressLint("SetJavaScriptEnabled")
    fun load(
        webView: WebView,
        onEditorMessage: (NoteEditorMessage) -> Unit,
        onRendererGone: () -> Unit,
    ) {
        this.webView = webView
        this.port = null
        this.isRenderProcessGone = false

        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Timber.d(
                    "NoteEditor WebView: " + consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId()
                )
                return super.onConsoleMessage(consoleMessage)
            }
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                LOCAL_PATH,
                WebViewAssetLoader.InternalStoragePathHandler(context, fileStore.noteEditorDirectory())
            )
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return true
                if (url.startsWith(LOCAL_URL_PREFIX)) {
                    return false
                }
                Timber.w("NoteEditorWebViewHandler: blocked navigation")
                return true
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                if (view !== this@NoteEditorWebViewHandler.webView) {
                    return true
                }
                Timber.e("NoteEditorWebViewHandler: render process gone, didCrash=${detail.didCrash()}")
                isRenderProcessGone = true
                onRendererGone()
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (view !== this@NoteEditorWebViewHandler.webView) {
                    return
                }
                val channel = view.createWebMessageChannel()
                val port = channel[0]
                this@NoteEditorWebViewHandler.port = port
                port.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
                    override fun onMessage(port: WebMessagePort, message: WebMessage) {
                        val data = message.data ?: return
                        onEditorMessage(NoteEditorMessage.parse(gson, data))
                    }
                })

                view.postWebMessage(
                    WebMessage(INIT_PORT_MESSAGE, arrayOf(channel[1])),
                    Uri.EMPTY
                )
            }
        }
        webView.loadUrl(EDITOR_URL)
    }

    fun start(options: NoteEditorStartOptions) {
        evaluate("start('${encodeAsJSONForJavascript(gson = gson, data = options)}');")
    }

    fun forceSave() {
        evaluate("forceSave();")
    }

    fun release(webView: WebView) {
        if (this.webView !== webView) {
            return
        }
        try {
            port?.close()
        } catch (e: Exception) {
            Timber.w(e, "NoteEditorWebViewHandler: can't close the port")
        }
        port = null
        this.webView = null
    }

    private fun evaluate(javascript: String) {
        val webView = this.webView
        if (webView == null || isRenderProcessGone) {
            return
        }
        webView.evaluateJavascript(javascript, null)
    }

    private companion object {
        const val LOCAL_PATH = "/local/"
        const val LOCAL_URL_PREFIX = "https://appassets.androidplatform.net$LOCAL_PATH"
        const val EDITOR_URL = "${LOCAL_URL_PREFIX}editor.html"
        const val INIT_PORT_MESSAGE = "initPort"
        const val TABLET_MIN_SMALLEST_WIDTH_DP = 600
    }
}