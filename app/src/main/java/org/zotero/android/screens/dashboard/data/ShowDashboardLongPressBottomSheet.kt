package org.zotero.android.screens.dashboard.data

import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem

data class ShowDashboardLongPressBottomSheet(
    val title: String,
    val longPressOptionItems: List<LongPressOptionItem>,
)
