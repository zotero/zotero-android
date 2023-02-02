package org.zotero.android.dashboard.data

import org.zotero.android.dashboard.ui.SinglePickerState

data class SinglePickerArgs(
    val singlePickerState: SinglePickerState,
    val title: String? = null,
    val showSaveButton: Boolean = true
)