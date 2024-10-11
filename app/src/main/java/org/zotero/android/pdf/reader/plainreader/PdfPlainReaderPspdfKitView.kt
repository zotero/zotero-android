package org.zotero.android.pdf.reader.plainreader

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.pspdfkit.ui.PdfReaderView

@Composable
fun PdfPlainReaderPspdfKitView(
    viewModel: PdfPlainReaderViewModel,
) {
    val activity = LocalContext.current as? AppCompatActivity ?: return
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val frameLayout = FrameLayout(context)
            frameLayout.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val pdfReaderView = PdfReaderView(activity)
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