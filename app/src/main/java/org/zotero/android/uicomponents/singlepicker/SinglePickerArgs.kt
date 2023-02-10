package org.zotero.android.uicomponents.singlepicker

import org.zotero.android.uicomponents.singlepicker.SinglePickerState

data class SinglePickerArgs(
    val singlePickerState: SinglePickerState,
    val title: String? = null,
    val showSaveButton: Boolean = true
)