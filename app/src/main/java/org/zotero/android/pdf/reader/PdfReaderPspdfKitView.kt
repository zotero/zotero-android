package org.zotero.android.pdf.reader

import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize

@Composable
fun PdfReaderPspdfKitView(uri: Uri, viewModel: PdfReaderViewModel) {
    val activity = LocalContext.current as? AppCompatActivity ?: return
    val annotationMaxSideSize = annotationMaxSideSize()
    val fragmentManager = activity.supportFragmentManager
    val layoutType = CustomLayoutSize.calculateLayoutType()
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val containerId = R.id.container
            val fragmentContainerView = FragmentContainerView(context).apply {
                id = containerId
            }
            viewModel.init(
                isTablet = layoutType.isTablet(),
                uri = uri,
                containerId = fragmentContainerView.id,
                fragmentManager = fragmentManager,
                annotationMaxSideSize = annotationMaxSideSize
            )
            fragmentContainerView
        },
        update = { _ ->
        }
    )
}

@Composable
private fun annotationMaxSideSize(): Int {
    val context = LocalContext.current
    val outValue = TypedValue()
    context.resources.getValue(R.dimen.pdf_sidebar_width_percent, outValue, true)
    val sidebarWidthPercentage = outValue.float
    val annotationSize = Resources.getSystem().displayMetrics.widthPixels * sidebarWidthPercentage
    return annotationSize.toInt()
}