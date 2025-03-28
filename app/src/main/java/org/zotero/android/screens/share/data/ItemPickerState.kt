package org.zotero.android.screens.share.data

import kotlinx.collections.immutable.ImmutableList

data class ItemPickerState(
    val items: ImmutableList<Pair<String, String>>,
    var picked: String?
)