package org.zotero.android.sync

sealed class Libraries {
    object all: Libraries()
    data class specific(val identifiers: List<LibraryIdentifier>): Libraries()
}
