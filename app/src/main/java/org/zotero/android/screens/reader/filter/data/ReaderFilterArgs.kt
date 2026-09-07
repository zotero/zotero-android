package org.zotero.android.screens.reader.filter.data

import org.zotero.android.screens.reader.data.ReaderAnnotationsFilter
import org.zotero.android.sync.Tag

data class ReaderFilterArgs(
    val filter: ReaderAnnotationsFilter?,
    val availableColors: MutableList<String>,
    val availableTags: List<Tag>
)