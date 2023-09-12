package org.zotero.android.pdf

import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
            val savedWrapper = viewModel.pspdfLayoutWrapper
            if (savedWrapper != null) {
                viewModel.updateVisibilityOfAnnotations()
                return@AndroidView savedWrapper
            }
            val frameLayout = FrameLayout(context).apply {
                val wrapperLayout = this
                id = View.generateViewId()
                viewModel.init2(
                    isTablet = layoutType.isTablet(),
                    uri = uri,
                    pspdfLayoutWrapper = wrapperLayout,
                    fragmentManager = fragmentManager,
                    annotationMaxSideSize = annotationMaxSideSize
                )

            }
            frameLayout
        },
        update = {
        }
    )
}


//@Composable
//fun PdfReaderPspdfKitView(uri: Uri, viewModel: PdfReaderViewModel) {
//    val activity = LocalContext.current as? AppCompatActivity ?: return
//    val annotationMaxSideSize = annotationMaxSideSize()
//    val fragmentManager = activity.supportFragmentManager
//    AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        onRelease = {t ->
//            fragmentManager.commitNow {
//                remove(viewModel.fragment)
//            }
//            println()
//        },
//        onReset = {t ->},
//        factory = { context ->
//            val savedWrapper = viewModel.pspdfLayoutWrapper
//            if (savedWrapper != null) {
//                return@AndroidView savedWrapper
//            }
//            val frameLayout = FrameLayout(context).apply {
//                val wrapperLayout = this
//                id = View.generateViewId()
//                fragmentManager.commit {
//                    val configuration = viewModel.generatePdfConfiguration()
//                    val newFragment = PdfFragment.newInstance(uri, configuration)
//                    newFragment.addDocumentListener(object : DocumentListener {
//                        override fun onDocumentLoaded(document: PdfDocument) {
//                            viewModel.init(
//                                document = document,
//                                wrapperLayout = wrapperLayout,
//                                fragment = newFragment,
//                                annotationMaxSideSize = annotationMaxSideSize
//                            )
//                        }
//                    })
//                    add(id, newFragment)
//                }
//            }
//            frameLayout
//        },
//        update = {
//        }
//    )
//}

@Composable
private fun annotationMaxSideSize(): Int {
    val context = LocalContext.current
    val outValue = TypedValue()
    context.resources.getValue(R.dimen.pdf_sidebar_width_percent, outValue, true)
    val sidebarWidthPercentage = outValue.float
    val annotationSize = Resources.getSystem().displayMetrics.widthPixels * sidebarWidthPercentage
    return annotationSize.toInt()
}