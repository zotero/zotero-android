package org.zotero.android.screens.login

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun LoginWebView(viewModel: LoginViewModel) {
    AndroidView(
        factory = { context ->
            val oldWebView = viewModel.webView
            if (oldWebView != null) {
                oldWebView
            } else {
                val webView = WebView(context)
                webView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webView.webViewClient = WebViewClient()
                webView.settings.javaScriptEnabled = true
                webView.clearCache(true);
                webView.clearHistory();
                webView.clearFormData();
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
                WebStorage.getInstance().deleteAllData();
                viewModel.init(webView)
                webView
            }
        },
        update = {
        }
    )
}