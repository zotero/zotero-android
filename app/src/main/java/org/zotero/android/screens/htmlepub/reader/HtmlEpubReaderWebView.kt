package org.zotero.android.screens.htmlepub.reader

import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.reader.web.HtmlEpubReaderCustomWebView

@Composable
fun HtmlEpubReaderWebView(viewModel: HtmlEpubReaderViewModel) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()
    val textFont = MaterialTheme.typography.bodyMedium
    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
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