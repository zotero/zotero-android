package org.zotero.android.pdf

import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.ui.PdfFragment
import org.zotero.android.R
import org.zotero.android.databinding.PdfScreenReaderHolderBinding

@Composable
fun PdfReaderPspdfKitView(uri: Uri, viewModel: PdfReaderViewModel) {
    val activity = LocalContext.current as? AppCompatActivity ?: return
    val configuration = viewModel.generatePdfConfiguration()
    val annotationMaxSideSize = annotationMaxSideSize()
    AndroidViewBinding(
        modifier = Modifier.fillMaxSize(),
        factory = { inflater, _, _ ->
            val binding = PdfScreenReaderHolderBinding.inflate(inflater)
            val newFragment = PdfFragment.newInstance(uri, configuration)
            newFragment.addDocumentListener(object : DocumentListener {
                override fun onDocumentLoaded(document: PdfDocument) {
                    viewModel.init(
                        document = document,
                        fragment = newFragment,
                        annotationMaxSideSize = annotationMaxSideSize
                    )
                }
            })
            activity.supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, newFragment)
                .commit()
            binding
        }, update = {
        })
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