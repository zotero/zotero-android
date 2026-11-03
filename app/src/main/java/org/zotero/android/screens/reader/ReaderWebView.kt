package org.zotero.android.screens.reader

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.reader.web.ReaderCustomWebView

@Composable
fun ReaderWebView(viewModel: ReaderViewModel) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()
    val textFont = MaterialTheme.typography.bodyMedium
    AndroidView(
        factory = { context ->
            val webView = ReaderCustomWebView(
                ContextThemeWrapper(context, R.style.ReaderWebViewThemeOverlay)
            )
            webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webView.onUserGestureDetected = { viewModel.onDocumentUserGestureDetected() }
            viewModel.initOnce(isTablet = isTablet, textFont = textFont)
            viewModel.initEveryTime(webView = webView)
            webView
        },
        update = {
        }
    )
}