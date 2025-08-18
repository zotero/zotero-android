package org.zotero.android.pdf.reader.plainreader

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pspdfkit.ui.PdfReaderView

@Composable
fun PdfPlainReaderPspdfKitView(
    viewModel: PdfPlainReaderViewModel,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val frameLayout = FrameLayout(context)
            frameLayout.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val pdfReaderView = PdfReaderView(context)
            pdfReaderView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            frameLayout.addView(pdfReaderView)
            viewModel.init(
                pdfReaderView = pdfReaderView
            )
            frameLayout
        },
        update = { _ ->
        }
    )
}