package org.zotero.android.screens.share.data

import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

data class RecentData(
    val collection: Collection?,
    val library: Library,
    val isRecent: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecentData

        if (collection != other.collection) return false
        if (library != other.library) return false

        return true
    }

    override fun hashCode(): Int {
        var result = collection?.hashCode() ?: 0
        result = 31 * result + library.hashCode()
        return result
    }
}