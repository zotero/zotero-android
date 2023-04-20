package org.zotero.android.screens.collectionpicker.data

import org.zotero.android.sync.Library

data class CollectionPickerArgs(
    val library: Library,
    val excludedKeys: Set<String>,
    val selected: Set<String>,
    val mode: CollectionPickerMode
)

