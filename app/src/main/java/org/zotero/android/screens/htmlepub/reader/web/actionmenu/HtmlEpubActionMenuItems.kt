package org.zotero.android.screens.htmlepub.reader.web.actionmenu

data class HtmlEpubActionMenuItems(
    val panelItems: List<HtmlEpubActionMenuItem>,
    val overflowItems: List<HtmlEpubActionMenuItem>
)

data class HtmlEpubActionMenuItem(
    val overflowTextResId: Int,
    val onClick: () -> Unit
)
