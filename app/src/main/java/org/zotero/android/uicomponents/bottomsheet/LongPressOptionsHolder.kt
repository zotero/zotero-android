package org.zotero.android.uicomponents.bottomsheet

data class LongPressOptionsHolder(
    val title: String,
    val isTitleEnabled: Boolean = true,
    val longPressOptionItems: List<LongPressOptionItem>
)