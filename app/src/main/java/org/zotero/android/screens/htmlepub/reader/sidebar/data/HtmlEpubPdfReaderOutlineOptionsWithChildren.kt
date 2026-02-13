package org.zotero.android.screens.htmlepub.reader.sidebar.data

import com.pspdfkit.annotations.actions.GoToAction
import com.pspdfkit.document.OutlineElement
import java.util.UUID

data class HtmlEpubPdfReaderOutlineOptionsWithChildren(
    val outline: Outline,
    val children: MutableList<HtmlEpubPdfReaderOutlineOptionsWithChildren> = mutableListOf()
)

data class Outline(
    val id: String,
    val title: String,
    val page: Int,
    val isActive: Boolean
) {
    constructor(element: OutlineElement, isActive: Boolean) : this(
        id = UUID.randomUUID().toString(),
        title = element.title ?: "",
        page = (element.action as? GoToAction)?.pageIndex ?: -1,
        isActive = isActive
    )
}
