package org.zotero.android.screens.reader.web.actionmenu

data class ReaderActionMenuItems(
    val panelItems: List<ReaderActionMenuItem>,
    val overflowItems: List<ReaderActionMenuItem>
)

data class ReaderActionMenuItem(
    val overflowTextResId: Int,
    val onClick: () -> Unit
)
