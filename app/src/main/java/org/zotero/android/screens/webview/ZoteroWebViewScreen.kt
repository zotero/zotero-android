package org.zotero.android.screens.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ZoteroWebViewScreen(
    url: String,
    onClose: () -> Unit,
) {
    AppThemeM3 {
        CustomScaffoldM3(
            topBar = {
                ZoteroWebviewTopBar(onClose = onClose)
            },
        ) {
            WebView(
                url = url
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebView(url: String) {
    AndroidView(
        factory = { context ->
            val webView = WebView(context)
            webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            webView.loadUrl(url)
            webView
        },
        update = {
        }
    )
}