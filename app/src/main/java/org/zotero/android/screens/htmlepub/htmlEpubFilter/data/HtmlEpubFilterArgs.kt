package org.zotero.android.screens.htmlepub.htmlEpubFilter.data

import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotationsFilter
import org.zotero.android.sync.Tag

data class HtmlEpubFilterArgs(
    val filter: HtmlEpubAnnotationsFilter?,
    val availableColors: MutableList<String>,
    val availableTags: List<Tag>
)