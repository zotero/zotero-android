package org.zotero.android.screens.addnote

import android.net.Uri
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import timber.log.Timber

@Composable
internal fun BoxScope.AddNoteWebView(viewModel: AddNoteViewModel, isKeyboardShown: Boolean) {
    val bottomPadding = if (isKeyboardShown) 0.dp else 48.dp
    AndroidView(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
            .padding(bottom = bottomPadding),
        factory = { context ->
            val webView = WebView(context)
            webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webView.settings.javaScriptEnabled = true
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Timber.d(
                        consoleMessage.message() + " -- From line "
                                + consoleMessage.lineNumber() + " of "
                                + consoleMessage.sourceId()
                    )
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val channel: Array<WebMessagePort> = webView.createWebMessageChannel()
                    val port = channel[0]
                    viewModel.setPort(port)
                    port.setWebMessageCallback(object :
                        WebMessagePort.WebMessageCallback() {
                        override fun onMessage(port: WebMessagePort, message: WebMessage) {
                            viewModel.processWebViewResponse(message)
                        }
                    })

                    webView.postWebMessage(
                        WebMessage("", arrayOf(channel[1])),
                        Uri.EMPTY
                    )
                    port.postMessage(viewModel.generateInitWebMessage())
                }
            }
            webView.loadUrl("file:///android_asset/editor.html")
            webView
        },
        update = {
        }
    )
}
