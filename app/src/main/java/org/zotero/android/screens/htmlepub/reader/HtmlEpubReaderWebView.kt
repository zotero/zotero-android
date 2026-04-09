package org.zotero.android.screens.htmlepub.reader

import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.reader.web.HtmlEpubReaderCustomWebView


@Composable
fun HtmlEpubReaderWebView(viewModel: HtmlEpubReaderViewModel) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()
    val textFont = MaterialTheme.typography.bodyMedium
    AndroidView(
        factory = { context ->
            val webView = HtmlEpubReaderCustomWebView(context)
            webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            viewModel.initOnce(isTablet = isTablet, textFont = textFont)
            viewModel.initEveryTime(webView = webView)
            webView
        },
        update = {
        }
    )
}