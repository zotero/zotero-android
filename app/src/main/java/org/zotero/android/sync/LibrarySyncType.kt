package org.zotero.android.sync

sealed class LibrarySyncType {
    object all: LibrarySyncType()
    data class specific(val identifiers: List<LibraryIdentifier>): LibrarySyncType()
}
