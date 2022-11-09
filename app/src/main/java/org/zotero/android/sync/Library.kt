package org.zotero.android.sync

data class Library(
    val identifier: LibraryIdentifier,
    val name: String,
    val metadataEditable: Boolean,
    val filesEditable: Boolean,
) {
    val id: LibraryIdentifier get() {
        return this.identifier
    }
}
