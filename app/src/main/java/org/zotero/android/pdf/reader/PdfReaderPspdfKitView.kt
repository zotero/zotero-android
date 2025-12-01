package org.zotero.android.pdf.reader

import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize
import timber.log.Timber

@Composable
fun PdfReaderPspdfKitView(
    uri: Uri,
    vMInterface: PdfReaderVMInterface
) {
    val activity = LocalActivity.current as? AppCompatActivity ?: return
    val annotationMaxSideSize = annotationMaxSideSize()
    val fragmentManager = activity.supportFragmentManager
    val layoutType = CustomLayoutSize.calculateLayoutType()
    vMInterface.annotationMaxSideSize = annotationMaxSideSize
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val frameLayout = FrameLayout(context)

            val containerId = R.id.container
            val fragmentContainerView = FragmentContainerView(context).apply {
                id = containerId
            }
            frameLayout.addView(fragmentContainerView)

            vMInterface.init(
                isTablet = layoutType.isTablet(),
                backgroundColor = backgroundColor,
                uri = uri,
                containerId = fragmentContainerView.id,
                fragmentManager = fragmentManager,
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