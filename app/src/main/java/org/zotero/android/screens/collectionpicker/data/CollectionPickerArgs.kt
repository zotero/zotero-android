package org.zotero.android.screens.collectionpicker.data

import org.zotero.android.sync.LibraryIdentifier

data class CollectionPickerArgs(
    val libraryId: LibraryIdentifier,
    val excludedKeys: Set<String>,
    val selected: Set<String>,
    val mode: CollectionPickerMode
)

