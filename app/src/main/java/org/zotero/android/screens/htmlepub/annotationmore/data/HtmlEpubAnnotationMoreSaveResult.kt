package org.zotero.android.screens.htmlepub.annotationmore.data

import org.zotero.android.database.objects.AnnotationType

data class HtmlEpubAnnotationMoreSaveResult(
    val key: String,
    val color: String,
    val lineWidth: Float,
    val pageLabel: String,
    val updateSubsequentLabels: Boolean,
    val text: String,
    val type: AnnotationType,
)