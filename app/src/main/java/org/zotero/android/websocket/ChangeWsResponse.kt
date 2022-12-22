package org.zotero.android.websocket

import org.zotero.android.sync.LibraryIdentifier

data class ChangeWsResponse(val type: Kind) {

    sealed class Kind {
        data class library(val libraryIdentifier: LibraryIdentifier, val version: Int?): Kind()
        object translators: Kind()
    }

    sealed class Error : Throwable() {
        data class unknownChange(val text: String) : Error()
    }
}
