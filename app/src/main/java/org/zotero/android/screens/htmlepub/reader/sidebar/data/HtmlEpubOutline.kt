package org.zotero.android.screens.htmlepub.reader.sidebar.data

import org.zotero.android.screens.htmlepub.reader.data.Outline
import java.util.UUID

data class HtmlEpubOutline(
    val id: String,
    val title: String,
    val location: Map<String, Any>,
    val children: MutableList<HtmlEpubOutline>,
    val isActive: Boolean,
) {
    constructor(outline: Outline, isActive: Boolean) : this(
        id = UUID.randomUUID().toString(),
        title = outline.title,
        location = outline.location,
        isActive = isActive,
        children = outline.children.map {
            HtmlEpubOutline(it, it.isActive)
        }.toMutableList()
    )
}
