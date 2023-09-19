package org.zotero.android.pdf.annotation

import org.zotero.android.sync.Library

data class PdfAnnotationArgs(
    val selectedAnnotation: org.zotero.android.pdf.data.Annotation?,
    val userId: Long,
    val library: Library,
)