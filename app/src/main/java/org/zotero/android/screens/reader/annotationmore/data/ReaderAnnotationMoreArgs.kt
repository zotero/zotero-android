package org.zotero.android.screens.reader.annotationmore.data

import org.zotero.android.screens.reader.data.ReaderAnnotation
import org.zotero.android.sync.Library

data class ReaderAnnotationMoreArgs(
    val selectedAnnotation: ReaderAnnotation?,
    val library: Library,
)