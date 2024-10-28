package org.zotero.android.sync

sealed class AttachmentFileDeletedNotification {
    data class individual(
        val key: String,
        val parentKey: String?,
        val libraryId: LibraryIdentifier
    ) : AttachmentFileDeletedNotification()

    data class allForItems(
        val keys: Set<String>,
        val collectionIdentifier: CollectionIdentifier?,
        val libraryId: LibraryIdentifier
    ) :
        AttachmentFileDeletedNotification()

    data class library(val libraryId: LibraryIdentifier) : AttachmentFileDeletedNotification()
    object all : AttachmentFileDeletedNotification()
}