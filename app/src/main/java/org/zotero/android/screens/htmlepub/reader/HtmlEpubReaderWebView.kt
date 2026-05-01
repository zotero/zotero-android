package org.zotero.android.screens.htmlepub.reader

import android.content.res.Resources
import android.util.TypedValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.reader.web.HtmlEpubReaderCustomWebView
import timber.log.Timber

@Composable
fun HtmlEpubReaderWebView(viewModel: HtmlEpubReaderViewModel) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val annotationMaxSideSize = annotationMaxSideSize()
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
            viewModel.initEveryTime(webView = webView, annotationMaxSideSize = annotationMaxSideSize)
            webView
        },
        update = {
        }
    )
}

@Composable
private fun annotationMaxSideSize(): Int {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val context = LocalContext.current
    val outValue = TypedValue()
    context.resources.getValue(R.dimen.pdf_sidebar_width_percent, outValue, true)
    val sidebarWidthPercentage = outValue.float
    val metricsWidthPixels = Resources.getSystem().displayMetrics.widthPixels
    val annotationSize = metricsWidthPixels * sidebarWidthPercentage
    val result = annotationSize.toInt()
    if (result <= 0) {
        val errorMessage = "HtmlEpubReaderWebView annotationMaxSideSize is $result" +
                ".sidebarWidthPercentage = $sidebarWidthPercentage" +
                ".metricsWidthPixels = $metricsWidthPixels"
        Timber.e(errorMessage)
        return if (layoutType.isTablet()) {
            480
        } else {
            1080
        }
    }
    return result
}