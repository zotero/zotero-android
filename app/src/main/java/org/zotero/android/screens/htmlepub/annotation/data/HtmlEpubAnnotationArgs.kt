package org.zotero.android.screens.htmlepub.annotation.data

import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.sync.Library

data class HtmlEpubAnnotationArgs(
    val selectedAnnotation: HtmlEpubAnnotation?,
    val library: Library,
)