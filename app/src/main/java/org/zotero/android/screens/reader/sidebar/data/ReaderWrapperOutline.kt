package org.zotero.android.screens.reader.sidebar.data

import org.zotero.android.screens.reader.data.ReaderOutline
import java.util.UUID

data class ReaderWrapperOutline(
    val id: String,
    val title: String,
    val location: Map<String, Any>,
    val children: MutableList<ReaderWrapperOutline>,
    val isActive: Boolean,
) {
    constructor(outline: ReaderOutline, isActive: Boolean) : this(
        id = UUID.randomUUID().toString(),
        title = outline.title,
        location = outline.location,
        isActive = isActive,
        children = outline.children.map {
            ReaderWrapperOutline(it, it.isActive)
        }.toMutableList()
    )
}
