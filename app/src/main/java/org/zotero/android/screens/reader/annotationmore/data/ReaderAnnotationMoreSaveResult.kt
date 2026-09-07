package org.zotero.android.screens.reader.annotationmore.data

import org.zotero.android.database.objects.AnnotationType

data class ReaderAnnotationMoreSaveResult(
    val key: String,
    val color: String,
    val lineWidth: Float,
    val fontSize: Float,
    val pageLabel: String,
    val updateSubsequentLabels: Boolean,
    val text: String,
    val type: AnnotationType,
)