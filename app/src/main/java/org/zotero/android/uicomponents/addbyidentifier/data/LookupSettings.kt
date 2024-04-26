package org.zotero.android.uicomponents.addbyidentifier.data

import org.zotero.android.sync.LibraryIdentifier

data class LookupSettings(
    val libraryIdentifier: LibraryIdentifier,
    val collectionKeys: Set<String>,
)
