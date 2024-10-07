package org.zotero.android.pdf.pdffilter.data

import org.zotero.android.pdf.data.AnnotationsFilter
import org.zotero.android.sync.Tag

data class PdfFilterArgs(
    val filter: AnnotationsFilter?,
    val availableColors: MutableList<String>,
    val availableTags: List<Tag>
)