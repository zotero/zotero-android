package org.zotero.android.api.pojo.sync

import org.zotero.android.sync.LibraryIdentifier

data class LastReadResponse(
    val key: String,
    val value: Long,
    val libraryId: LibraryIdentifier,
    val version: Int,
)
