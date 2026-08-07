package org.zotero.android.screens.reader.annotation.data

import org.zotero.android.screens.reader.data.ReaderAnnotation
import org.zotero.android.sync.Library

data class ReaderAnnotationArgs(
    val selectedAnnotation: ReaderAnnotation?,
    val library: Library,
)