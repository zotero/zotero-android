package org.zotero.android.pdf.annotationmore.data

import org.zotero.android.pdf.reader.AnnotationKey

data class PdfAnnotationMoreSaveResult(
    val key: AnnotationKey,
    val color: String,
    val lineWidth: Float,
    val fontSize: Float,
    val pageLabel: String,
    val updateSubsequentLabels: Boolean,
    val text: String,
)