package org.zotero.android.pdf.reader.sidebar.sections

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import org.zotero.android.androidx.content.pxToDp
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.sidebar.sectionVerticalPadding

@Composable
internal fun PdfReaderAnnotationsSidebarImageSection(
    loadPreview: () -> Bitmap?,
    vMInterface: PdfReaderVMInterface,
) {
    val cachedBitmap = loadPreview()
    if (cachedBitmap != null) {
        Image(
            modifier = Modifier
                .sectionVerticalPadding()
                .fillMaxWidth()
                .heightIn(max = vMInterface.annotationMaxSideSize.pxToDp()),
            bitmap = cachedBitmap.asImageBitmap(),
            contentDescription = null,
        )
    }
}