package org.zotero.android.screens.reader.annotationmore.data

import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.sync.Library

data class ReaderAnnotationMoreArgs(
    val selectedAnnotation: NewReaderAnnotation?,
    val library: Library,
)