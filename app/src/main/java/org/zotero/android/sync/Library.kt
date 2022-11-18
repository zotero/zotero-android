package org.zotero.android.sync

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Library(
    val identifier: LibraryIdentifier,
    val name: String,
    val metadataEditable: Boolean,
    val filesEditable: Boolean,
): Parcelable {
    val id: LibraryIdentifier get() {
        return this.identifier
    }
}
