package org.zotero.android.screens.share.data

data class ItemPickerState(
    val items: List<Pair<String, String>>,
    var picked: String?
)