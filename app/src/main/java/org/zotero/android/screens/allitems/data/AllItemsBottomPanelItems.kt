package org.zotero.android.screens.allitems.data

import androidx.compose.ui.graphics.Color

data class AllItemsBottomPanelItems(
    val panelItems: List<AllItemsBottomPanelItem>,
    val overflowItems: List<AllItemsBottomPanelItem>
)

data class AllItemsBottomPanelItem(
    val iconRes: Int,
    val overflowTextResId: Int,
    val iconTint: Color? = null,
    val textColor: Color? = null,
    val onClick: () -> Unit
)
