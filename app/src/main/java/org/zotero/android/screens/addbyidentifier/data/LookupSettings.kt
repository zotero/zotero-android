package org.zotero.android.screens.addbyidentifier.data

import org.zotero.android.sync.LibraryIdentifier

data class LookupSettings(
    val libraryIdentifier: LibraryIdentifier,
    val collectionKeys: Set<String>,
)
