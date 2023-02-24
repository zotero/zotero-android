package org.zotero.android.uicomponents.singlepicker

data class SinglePickerArgs(
    val singlePickerState: SinglePickerState,
    val title: String? = null,
    val showSaveButton: Boolean = true,
    val callPoint: SinglePickerResult.CallPoint,
)