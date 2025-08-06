package org.zotero.android.citation

import org.zotero.android.sync.LibraryIdentifier
import java.util.UUID

data class CitationSession(
    val id: String = UUID.randomUUID().toString(),
    val itemIds: Set<String>,
    val libraryId: LibraryIdentifier,

    val styleXML: String,
    val styleLocaleId: String,
    val localeXML: String,
    val supportsBibliography: Boolean,
    val itemsCSL: String
)