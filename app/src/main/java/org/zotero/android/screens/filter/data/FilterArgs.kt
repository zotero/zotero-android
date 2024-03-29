package org.zotero.android.screens.filter.data

import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

data class FilterArgs(
    val filters: List<ItemsFilter>,
    val collectionId: CollectionIdentifier,
    val libraryId: LibraryIdentifier,
    val selectedTags: Set<String>,
)