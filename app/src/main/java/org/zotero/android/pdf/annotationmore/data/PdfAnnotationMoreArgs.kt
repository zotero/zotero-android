package org.zotero.android.pdf.annotationmore.data

import org.zotero.android.sync.Library

data class PdfAnnotationMoreArgs(
    val selectedAnnotation: org.zotero.android.pdf.data.PDFAnnotation?,
    val userId: Long,
    val library: Library,
)