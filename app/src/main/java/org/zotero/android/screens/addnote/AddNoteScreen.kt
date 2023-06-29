package org.zotero.android.screens.addnote

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AddNoteScreen(
    onBack: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(AddNoteViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            is AddNoteViewEffect.NavigateBack -> onBack()
            null -> Unit
        }
    }
    CustomScaffold(
        topBar = {
            AddNoteTopBar(titleData = viewState.title, onDoneClicked = viewModel::onDoneClicked)
        },
    ) {
        Column() {
            WebView(viewModel)
        }
    }

}

@Composable
private fun WebView(viewModel: AddNoteViewModel) {
    AndroidView(
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