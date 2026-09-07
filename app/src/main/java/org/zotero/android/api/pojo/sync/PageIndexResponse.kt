package org.zotero.android.api.pojo.sync

import org.zotero.android.sync.LibraryIdentifier

data class PageIndexResponse(
    val key: String,
    val value: String,
    val version: Int,
    val libraryId: LibraryIdentifier,
)
