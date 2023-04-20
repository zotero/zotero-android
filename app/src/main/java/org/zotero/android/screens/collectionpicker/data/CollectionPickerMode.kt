package org.zotero.android.screens.collectionpicker.data

sealed interface CollectionPickerMode {
    data class single(val title: String): CollectionPickerMode
    object multiple: CollectionPickerMode
}