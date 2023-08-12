package org.zotero.android.screens.addnote

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AddNoteScreen(
    onBack: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel(),
    navigateToTagPicker: () -> Unit,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(AddNoteViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var isKeyboardShown by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()

        KeyboardVisibilityEvent.setEventListener(
            context.findActivity()!!,
            lifecycleOwner
        ) { isOpen ->
            isKeyboardShown = isOpen
        }
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            null -> Unit
            is AddNoteViewEffect.NavigateBack -> onBack()
            is AddNoteViewEffect.NavigateToTagPickerScreen -> {
                navigateToTagPicker()
            }
            AddNoteViewEffect.RefreshUI -> {}
        }
    }
    CustomScaffold(
        topBar = {
            AddNoteTopBar(titleData = viewState.title, onDoneClicked = viewModel::onDoneClicked)
        },
    ) {
        Box {
            WebView(
                viewModel = viewModel,
                isKeyboardShown = isKeyboardShown
            )
            if (!isKeyboardShown) {
                AddNoteTagSelector(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType
                )
            }
        }
    }

}

@Composable
private fun BoxScope.WebView(viewModel: AddNoteViewModel, isKeyboardShown: Boolean) {
    val bottomPadding = if (isKeyboardShown) 0.dp else 50.dp
    AndroidView(
        modifier = Modifier
            .align(Alignment.TopCenter)
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

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}