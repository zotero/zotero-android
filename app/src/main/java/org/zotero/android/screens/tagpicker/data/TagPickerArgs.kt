package org.zotero.android.screens.tagpicker.data

import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag

data class TagPickerArgs(
    val libraryId: LibraryIdentifier,
    val selectedTags: Set<String>,
    val tags: List<Tag> = emptyList()
)

