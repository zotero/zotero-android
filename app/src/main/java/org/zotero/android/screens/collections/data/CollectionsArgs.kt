package org.zotero.android.screens.collections.data

import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

data class CollectionsArgs(
    val libraryId: LibraryIdentifier,
    val selectedCollectionId: CollectionIdentifier,
    )