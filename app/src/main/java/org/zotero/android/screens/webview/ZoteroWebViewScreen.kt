package org.zotero.android.screens.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ZoteroWebViewScreen(
    url: String,
    onClose: () -> Unit,
) {
    CustomThemeWithStatusAndNavBars {
        CustomScaffold(
            topBarColor = CustomTheme.colors.surface,
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