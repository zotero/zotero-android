package org.zotero.android.pdf.reader.sidebar.data

import android.graphics.PointF
import com.pspdfkit.annotations.InkAnnotation
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.sync.Tag

data class InkAnnotationsData(
    val oldestAnnotation: PDFAnnotation,
    val oldestDocumentAnnotation: InkAnnotation,
    val lines: List<List<PointF>>,
    val lineWidth: Float,
    val tags: List<Tag>
)
