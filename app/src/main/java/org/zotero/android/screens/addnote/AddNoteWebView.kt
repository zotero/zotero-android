package org.zotero.android.screens.addnote

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

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
            viewModel.initWebView(webView)
            webView
        },
        onRelease = { webView ->
            viewModel.onWebViewReleased(webView)
            webView.destroy()
        }
    )
}