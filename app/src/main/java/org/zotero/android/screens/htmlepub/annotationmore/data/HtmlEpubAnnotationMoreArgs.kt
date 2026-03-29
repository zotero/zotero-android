package org.zotero.android.screens.htmlepub.annotationmore.data

import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.sync.Library

data class HtmlEpubAnnotationMoreArgs(
    val selectedAnnotation: HtmlEpubAnnotation?,
    val library: Library,
)