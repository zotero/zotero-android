package org.zotero.android.screens.citbibexport.data

import org.zotero.android.sync.LibraryIdentifier

data class CitBibExportArgs(
    val itemIds: Set<String>,
    val libraryId: LibraryIdentifier
)