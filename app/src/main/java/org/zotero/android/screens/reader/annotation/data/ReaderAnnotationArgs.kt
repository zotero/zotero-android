package org.zotero.android.screens.reader.annotation.data

import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.sync.Library

data class ReaderAnnotationArgs(
    val selectedAnnotation: NewReaderAnnotation?,
    val library: Library,
)