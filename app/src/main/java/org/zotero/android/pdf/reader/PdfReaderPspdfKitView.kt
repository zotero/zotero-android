package org.zotero.android.pdf.reader

import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.pspdfkit.ui.PdfThumbnailBar
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize
import timber.log.Timber

@Composable
fun PdfReaderPspdfKitView(uri: Uri, viewModel: PdfReaderViewModel) {
    val activity = LocalContext.current as? AppCompatActivity ?: return
    val annotationMaxSideSize = annotationMaxSideSize()
    val fragmentManager = activity.supportFragmentManager
    val layoutType = CustomLayoutSize.calculateLayoutType()
    viewModel.annotationMaxSideSize = annotationMaxSideSize
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val frameLayout = FrameLayout(context)

            val containerId = R.id.container
            val fragmentContainerView = FragmentContainerView(context).apply {
                id = containerId
            }
            frameLayout.addView(fragmentContainerView)

            val pdfThumbnailBar = PdfThumbnailBar(context)
            val thumbnailBarLayoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            thumbnailBarLayoutParams.gravity = Gravity.BOTTOM
            pdfThumbnailBar.layoutParams = thumbnailBarLayoutParams
            frameLayout.addView(pdfThumbnailBar)

            viewModel.init(
                isTablet = layoutType.isTablet(),
                uri = uri,
                containerId = fragmentContainerView.id,
                fragmentManager = fragmentManager,
                pdfThumbnailBar = pdfThumbnailBar,
                annotationMaxSideSize = annotationMaxSideSize
            )
            frameLayout
        },
        update = { _ ->
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
        val errorMessage = "PdfReaderPspdfKitView annotationMaxSideSize is $result" +
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