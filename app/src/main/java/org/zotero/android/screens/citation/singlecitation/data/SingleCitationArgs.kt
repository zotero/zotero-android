package org.zotero.android.screens.citation.singlecitation.data

import org.zotero.android.sync.LibraryIdentifier

data class SingleCitationArgs(
    val libraryId: LibraryIdentifier,
    val itemIds: Set<String>,
)

