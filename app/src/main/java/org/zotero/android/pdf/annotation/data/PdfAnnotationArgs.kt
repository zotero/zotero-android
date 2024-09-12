package org.zotero.android.pdf.annotation.data

import org.zotero.android.sync.Library

data class PdfAnnotationArgs(
    val selectedAnnotation: org.zotero.android.pdf.data.PDFAnnotation?,
    val userId: Long,
    val library: Library,
)